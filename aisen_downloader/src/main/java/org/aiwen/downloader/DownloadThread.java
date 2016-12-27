package org.aiwen.downloader;

import android.net.Uri;
import android.os.SystemClock;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Response;

import org.aiwen.downloader.utils.Constants;
import org.aiwen.downloader.utils.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * 下载线程
 *
 * Created by wangdan on 16/11/18.
 */
public class DownloadThread implements Runnable {

    private final static OkHttpClient mOkHttpClient = new OkHttpClient();

    private final static int CONN_TIMEOUT = 30000;
    private final static int READ_TIMEOUT = 30000;

    static {
        try {
            mOkHttpClient.setConnectTimeout(CONN_TIMEOUT, TimeUnit.MILLISECONDS);
            mOkHttpClient.setReadTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS);

            TrustManager tm = new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType)
                        throws CertificateException {
                }

                public void checkServerTrusted(X509Certificate[] chain, String authType)
                        throws CertificateException {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };

            SSLContext e = SSLContext.getInstance("TLS");
            e.init(null, new TrustManager[]{tm}, null);
            mOkHttpClient.setSslSocketFactory(e.getSocketFactory());
        } catch (Exception e) {
            throw new RuntimeException("supportHttps failed", e);
        }
    }

    private final Request mRequest;
    private final DownloadService mService;
    private final DownloadNotifier mNotifier;
    private final Hawk mHawk;
    private File mTempFile;
    private File mSaveFile;

    /**
     * Details from the last time we pushed a database update.
     */
    private long mLastUpdateBytes = 0;
    private long mLastUpdateTime = 0;

    public DownloadThread(Hawk hawk, Request request, DownloadService service) {
        mHawk = hawk;
        mNotifier = hawk.notifier;
        mRequest = request;
        mRequest.thread = this;
        mRequest.downloadInfo.status = Downloads.Status.STATUS_PENDING;
        mRequest.downloadInfo.writeToDatabase();
        mHawk.trace.peddingThread.incrementAndGet();
        mService = service;
        mService.threadIncrement();
    }

    @Override
    public void run() {
        mHawk.trace.peddingThread.decrementAndGet();
        mHawk.trace.concurrentThread.incrementAndGet();

        final DownloadInfo downloadInfo = mRequest.downloadInfo;

        try {
            mRequest.trace = new ThreadTrace(mRequest);

            // 创建临时文件
            mTempFile = FileManager.createFile(mRequest, true);
            mSaveFile = FileManager.createFile(mRequest, false);;

            // 验证文件
            if (checkFile()) {
                DLogger.w(Utils.getDownloaderTAG(mRequest), "文件校验成功，完成下载");

                downloadInfo.status = Downloads.Status.STATUS_SUCCESS;
                downloadInfo.rangeBytes = downloadInfo.fileBytes;
                downloadInfo.numFailed = 0;

                return;
            }

            downloadInfo.status = Downloads.Status.STATUS_RUNNING;
            downloadInfo.writeToDatabase();

            DLogger.d(Utils.getDownloaderTAG(mRequest), "开始下载(%s)", mRequest.uri);
            executeDownload(mRequest);
            DLogger.d(Utils.getDownloaderTAG(mRequest), "结束下载(%s)", mRequest.uri);

            downloadInfo.status = Downloads.Status.STATUS_SUCCESS;
        } catch (DownloadException e) {
            e.printStackTrace();

            // 处理错误状态
            switch (e.getStatus()) {
                case Downloads.Status.STATUS_HTTP_EXCEPTION:
                case Downloads.Status.STATUS_HTTP_DATA_ERROR:
                    // 网络正常连接，尝试重连
                    if (Utils.isNetworkActive()) {
                        int numFailed = ++downloadInfo.numFailed;
                        if (numFailed <= Constants.MAX_RETRIES) {
                            downloadInfo.retryAfter = (numFailed << 2) * 1000;
                            downloadInfo.status = Downloads.Status.STATUS_WAITING_TO_RETRY;

                            mService.setRetryAlarm(mRequest);
                        }
                        else {
                            downloadInfo.retryAfter = 0;
                            downloadInfo.status = e.getStatus();
                        }
                    }
                    // 没有网络连接
                    else {
                        downloadInfo.retryAfter = 0;
                        downloadInfo.status = Downloads.Status.STATUS_WAITING_FOR_NETWORK;
                    }
                    break;
                default:
                    downloadInfo.status = e.getStatus();
                    break;
            }
            downloadInfo.error = Downloads.Status.statusToString(downloadInfo.status);
        } finally {
            mHawk.trace.concurrentThread.decrementAndGet();

            downloadInfo.lastMod = Utils.realtime();
            mRequest.trace.speed = 0l;
            mNotifier.notifyDownloadSpeed(mRequest.id, 0);
            downloadInfo.writeToDatabase();
        }

        mRequest.thread = null;

        mService.threadDecrement();
        mService.stopIfNeed();
    }

    // 开始下载
    private void executeDownload(Request request) throws DownloadException {
        final Uri uri = request.uri;
        final DownloadInfo downloadInfo = request.downloadInfo;
        final ThreadTrace trace = request.trace;

        com.squareup.okhttp.Request.Builder builder = new com.squareup.okhttp.Request.Builder();
        final String url;
        if (uri.getScheme().equalsIgnoreCase("http") || uri.getScheme().equalsIgnoreCase("https")) {
            url = uri.toString();
        }
        else {
            url = uri.getPath();
        }
        builder.url(url);

        // 断点下载
        if (downloadInfo.rangeBytes > 0) {
            DLogger.i(Utils.getDownloaderTAG(request), "set range header = %d", downloadInfo.rangeBytes);
            builder.addHeader("Range", "bytes="+ downloadInfo.rangeBytes + "-");
        }

        final Response response;
        // 开始请求数据
        try {
            trace.beginConnect();

            response = mOkHttpClient.newCall(builder.build()).execute();
            if (!response.isSuccessful()) {
                throw new DownloadException(Downloads.Status.STATUS_HTTP_EXCEPTION);
            }

            trace.endConnect();
        } catch (IOException e) {
            Utils.printStackTrace(e);

            trace.endConnect();
            trace.endRead();

            throw new DownloadException(Downloads.Status.STATUS_HTTP_EXCEPTION);
        }

        try {
            // 开始解析数据
            transferData(request, response);

            // 临时文件copy成目标文件
            copyFile();
        } catch (IOException e) {
            Utils.printStackTrace(e);

            trace.endSpeedCount();
            trace.computeSpeed();
            trace.endRead();

            throw new DownloadException(Downloads.Status.STATUS_HTTP_DATA_ERROR);
        }
    }

    // 缓存数据
    private void transferData(Request request, Response response) throws IOException, DownloadException {
        final DownloadInfo downloadInfo = request.downloadInfo;
        final ThreadTrace trace = request.trace;

        // Content-Length
        long contentLen = -1;
        long totalLen = -1;
        try {
            String header = response.header("Content-Length");
            contentLen = Long.parseLong(header);
            totalLen = contentLen + downloadInfo.rangeBytes;// 文件大小
        } catch (Exception e) {
            e.printStackTrace();
        }
        DLogger.d(Utils.getDownloaderTAG(request), "fileBytes(%d), rangeBytes(%d), Content-Length(%d)", totalLen, downloadInfo.rangeBytes, contentLen);
        if (totalLen != -1) {
            downloadInfo.fileBytes = totalLen;
        }

        downloadInfo.writeToDatabase();

        InputStream in = null;
        final FileOutputStream out;
        try {
            out = new FileOutputStream(mTempFile, true);
        } catch (FileNotFoundException e) {
            Utils.printStackTrace(e);

            throw new DownloadException(Downloads.Status.STATUS_FILE_ERROR);
        }
        try {
            in = response.body().byteStream();
            byte[] readBuffer = new byte[8 * 1024];
            int readLen = -1;

            trace.benginRead();
            trace.beginSpeedCount();
            while ((readLen = in.read(readBuffer)) != -1) {
                out.write(readBuffer, 0, readLen);

                downloadInfo.rangeBytes += readLen;
                trace.receive(readLen);

                final long now = SystemClock.elapsedRealtime();
                final long currentBytes = downloadInfo.rangeBytes;

                // 更新DB下载进度
                final long bytesDelta = currentBytes - mLastUpdateBytes;
                final long timeDelta = now - mLastUpdateTime;
                if (bytesDelta > Constants.MIN_PROGRESS_STEP && timeDelta > Constants.MIN_PROGRESS_TIME) {
                    mLastUpdateBytes = currentBytes;
                    mLastUpdateTime = now;

                    trace.endSpeedCount();
                    trace.computeSpeed();
                    mNotifier.notifyDownloadSpeed(mRequest.id, (long) trace.getSpeed());

                    trace.beginSpeedCount();

                    // 正常下载数据后，将下载失败次数清零
                    if (downloadInfo.numFailed > 0) {
                        downloadInfo.numFailed = 0;
                        downloadInfo.retryAfter = 0;
                    }
                    downloadInfo.writeToDatabase();
                }
            }
            trace.endRead();

            out.flush();
        } finally {
            Utils.close(in);
            Utils.close(out);
        }
    }

    // 重新获取文件的长度，目前仅支持使用文件长度校验文件合法性
    private boolean checkFile() {
        // 目标文件存在
        if (mSaveFile.exists()) {
            if (mSaveFile.length() == mRequest.downloadInfo.fileBytes) {
                DLogger.w(Utils.getDownloaderTAG(mRequest), "目标文件已存在");

                return mHawk.fileCheckCallback.onFileCheck(mRequest, mSaveFile);
            }
            else {
                DLogger.w(Utils.getDownloaderTAG(mRequest), "删除目标文件，FileBytes(%d), File(%s)", mRequest.downloadInfo.fileBytes, mSaveFile.getAbsolutePath());

                mSaveFile.delete();
            }
        }

        // 临时文件存在
        if (mTempFile.exists()) {
            if (mTempFile.length() > 0) {
                try {
                    // 已下载完
                    if (mTempFile.length() == mRequest.downloadInfo.fileBytes) {
                        if (copyFile()) {
                            DLogger.w(Utils.getDownloaderTAG(mRequest), "临时文件存在且copy为目标文件");

                            return mHawk.fileCheckCallback.onFileCheck(mRequest, mSaveFile);
                        }
                    }
                    // 断点下载
                    else if (mTempFile.length() == mRequest.downloadInfo.rangeBytes) {
                        return true;
                    }
                } catch (DownloadException e){
                    e.printStackTrace();
                }
            }

            DLogger.w(Utils.getDownloaderTAG(mRequest), "删除临时文件(%s)", mTempFile.getAbsolutePath());
            mTempFile.delete();
            mRequest.downloadInfo.rangeBytes = 0;
        }
        else {
            mRequest.downloadInfo.rangeBytes = 0;
        }

        return false;
    }

    private boolean copyFile() throws DownloadException {
        try {
            return mTempFile.renameTo(mSaveFile);
        } catch (Exception e) {
            e.printStackTrace();
        }

        DLogger.w(Utils.getDownloaderTAG(mRequest), "重命名临时文件出错, save file = %s", mSaveFile.getAbsolutePath());

        throw new DownloadException(Downloads.Status.STATUS_FILE_ERROR);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        DLogger.w(Utils.getDownloaderTAG(mRequest), "Thread finalize()");
    }

}
