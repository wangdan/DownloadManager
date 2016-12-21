package org.aiwen.downloader;

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
 * OKHttp下载器
 *
 * Created by 王dan on 2016/12/19.
 */
public class OkHttpDownloader implements IDownloader {

    private final static OkHttpClient mOkHttpClient = new OkHttpClient();

    public final static int CONN_TIMEOUT = 30000;
    public final static int READ_TIMEOUT = 30000;

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

    @Override
    public void download(Request request) throws DownloadException {
        request.trace = new Trace(request);

        request.downloadInfo.status = Downloads.Status.STATUS_RUNNING;

        DLogger.d(Utils.getDownloaderTAG(request), "开始下载(%s)", request.uri);
        executeDownload(request);
        DLogger.d(Utils.getDownloaderTAG(request), "结束下载(%s)", request.uri);

        request.downloadInfo.status = Downloads.Status.STATUS_SUCCESS;
    }

    // 开始下载
    private void executeDownload(Request request) throws DownloadException {
        com.squareup.okhttp.Request.Builder builder = new com.squareup.okhttp.Request.Builder();
        final String url;
        if (request.uri.getScheme().equalsIgnoreCase("http") || request.uri.getScheme().equalsIgnoreCase("https")) {
            url = request.uri.toString();
        }
        else {
            url = request.uri.getPath();
        }
        builder.url(url);

        // 断点下载
        if (request.downloadInfo.rangeBytes > 0) {
            builder.addHeader("Range", "bytes="+ request.downloadInfo.rangeBytes + "-");
        }

        try {
            request.trace.beginConnect();
            Response response = mOkHttpClient.newCall(builder.build()).execute();
            request.trace.endConnect();

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
            totalLen = contentLen + request.downloadInfo.rangeBytes;// 文件大小
        } catch (Exception e) {
            e.printStackTrace();
        }
        DLogger.d(Utils.getDownloaderTAG(request), "Total-Length = %d, Content-Length = %d", totalLen, contentLen);
        if (totalLen != -1) {
            request.downloadInfo.fileBytes = totalLen;
        }

        InputStream in = null;
        try {
            in = response.body().byteStream();
            byte[] readBuffer = new byte[8 * 1024];
            int readLen = -1;

            request.trace.benginRead();
            request.trace.beginSpeedCount();
            while ((readLen = in.read(readBuffer)) != -1) {
                out.write(readBuffer, 0, readLen);

                request.trace.receive(readLen);
            }
            request.trace.endRead();

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
