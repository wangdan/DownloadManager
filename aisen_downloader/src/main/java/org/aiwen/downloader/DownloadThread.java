package org.aiwen.downloader;

import android.net.Uri;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Response;

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

    public DownloadThread(Request request, DownloadService service) {
        mRequest = request;
        mRequest.downloadInfo.status = Downloads.Status.STATUS_PENDING;
        mService = service;
        mService.threadIncrement();
    }

    @Override
    public void run() {
        try {
            mRequest.trace = new Trace(mRequest);

            mRequest.downloadInfo.status = Downloads.Status.STATUS_RUNNING;

            DLogger.d(Utils.getDownloaderTAG(mRequest), "开始下载(%s)", mRequest.uri);
            executeDownload(mRequest);
            DLogger.d(Utils.getDownloaderTAG(mRequest), "结束下载(%s)", mRequest.uri);

            mRequest.downloadInfo.status = Downloads.Status.STATUS_SUCCESS;
        } catch (DownloadException e) {
            e.printStackTrace();
        }

        mService.threadDecrement();
        mService.stopIfNeed();
    }

    // 开始下载
    private void executeDownload(Request request) throws DownloadException {
        final Uri uri = request.uri;
        final DownloadInfo downloadInfo = request.downloadInfo;
        final Trace trace = request.trace;

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
        final Trace trace = request.trace;

        // 创建临时文件
        File tempFile = FileManager.createTempFile(request);
        final FileOutputStream out;
        try {
            out = new FileOutputStream(tempFile);
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

        InputStream in = null;
        try {
            in = response.body().byteStream();
            byte[] readBuffer = new byte[8 * 1024];
            int readLen = -1;

            trace.benginRead();
            trace.beginSpeedCount();
            while ((readLen = in.read(readBuffer)) != -1) {
                out.write(readBuffer, 0, readLen);

                trace.receive(readLen);
            }
            trace.endRead();

            out.flush();
        } catch (IOException e) {
            e.printStackTrace();

            // TODO
            throw new DownloadException();
        } finally {
            Utils.close(in);
            Utils.close(out);
        }
    }

}
