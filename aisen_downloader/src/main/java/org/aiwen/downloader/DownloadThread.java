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
    private final Hawk mHawk;
    private File mTempFile;

    /**
     * Details from the last time we pushed a database update.
     */
    private long mLastUpdateBytes = 0;
    private long mLastUpdateTime = 0;

    public DownloadThread(Hawk hawk, Request request, DownloadService service) {
        mHawk = hawk;
        mRequest = request;
        mRequest.thread = this;
        mRequest.downloadInfo.status = Downloads.Status.STATUS_PENDING;
        mHawk.trace.peddingThread.incrementAndGet();
        mService = service;
        mService.threadIncrement();
    }

    @Override
    public void run() {
        mHawk.trace.peddingThread.decrementAndGet();
        mHawk.trace.concurrentThread.incrementAndGet();

        try {
            // 创建临时文件
            mTempFile = FileManager.createTempFile(mRequest);

            mRequest.trace = new ThreadTrace(mRequest);

            mRequest.downloadInfo.status = Downloads.Status.STATUS_RUNNING;
            mRequest.downloadInfo.writeToDatabase();

            DLogger.d(Utils.getDownloaderTAG(mRequest), "开始下载(%s)", mRequest.uri);
            executeDownload(mRequest);
            DLogger.d(Utils.getDownloaderTAG(mRequest), "结束下载(%s)", mRequest.uri);

            mRequest.downloadInfo.status = Downloads.Status.STATUS_SUCCESS;
        } catch (DownloadException e) {
            e.printStackTrace();

            mRequest.downloadInfo.status = e.getStatus();
            mRequest.downloadInfo.error = e.getError();
            mRequest.downloadInfo.writeToDatabase();
        } finally {
            mHawk.trace.concurrentThread.decrementAndGet();
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

        try {
            trace.beginConnect();
            Response response = mOkHttpClient.newCall(builder.build()).execute();
            trace.endConnect();

            if (response.isSuccessful()) {
                transferData(request, response);
            }
            else {
                // TODO
                throw new DownloadException();
            }
        } catch (IOException e) {
            e.printStackTrace();

            // TODO
            throw new DownloadException();
        }
    }

    // 缓存数据
    private void transferData(Request request, Response response) throws DownloadException {
        final DownloadInfo downloadInfo = request.downloadInfo;
        final ThreadTrace trace = request.trace;

        final FileOutputStream out;
        try {
            out = new FileOutputStream(mTempFile, true);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            // TODO
            throw new DownloadException();
        }

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
        DLogger.d(Utils.getDownloaderTAG(request), "Total-Length = %d, Content-Length = %d", totalLen, contentLen);
        if (totalLen != -1) {
            downloadInfo.fileBytes = totalLen;
        }

        mRequest.downloadInfo.writeToDatabase();

        InputStream in = null;
        try {
            in = response.body().byteStream();
            byte[] readBuffer = new byte[8 * 1024];
            int readLen = -1;

            trace.benginRead();
            trace.beginSpeedCount();
            while ((readLen = in.read(readBuffer)) != -1) {
                out.write(readBuffer, 0, readLen);

                mRequest.downloadInfo.rangeBytes += readLen;
                trace.receive(readLen);

                final long now = SystemClock.elapsedRealtime();
                final long currentBytes = downloadInfo.rangeBytes;

                // 更新DB下载进度
                final long bytesDelta = currentBytes - mLastUpdateBytes;
                final long timeDelta = now - mLastUpdateTime;
                if (bytesDelta > Constants.MIN_PROGRESS_STEP && timeDelta > Constants.MIN_PROGRESS_TIME) {
                    mRequest.downloadInfo.writeToDatabase();

                    mLastUpdateBytes = currentBytes;
                    mLastUpdateTime = now;
                }
            }
            trace.endRead();

            out.flush();
        } catch (IOException e) {
            e.printStackTrace();

            // TODO
            throw new DownloadException();
        } finally {
            mRequest.downloadInfo.writeToDatabase();

            Utils.close(in);
            Utils.close(out);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        DLogger.w(Utils.getDownloaderTAG(mRequest), "Thread finalize()");
    }

}
