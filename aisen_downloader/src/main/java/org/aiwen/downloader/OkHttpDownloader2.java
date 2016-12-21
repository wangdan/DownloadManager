package org.aiwen.downloader;

import android.os.SystemClock;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Response;

import org.aiwen.downloader.utils.Constants;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
public class OkHttpDownloader2 implements IDownloader {

    private static final String TAG = Constants.TAG + "_OKHttp";

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
        final DownloadInfo downloadInfo = request.downloadInfo;

        // 创建临时文件
        File tempFile = new File(request.fileUri.getPath() + ".dt");
        DLogger.d(getTAG(request), "创建临时文件，file(%s)", tempFile.getAbsolutePath());

        // 断点下载
        long rangeBytes = downloadInfo.rangeBytes;
        if (tempFile.exists()) {
            // 临时文件长度和缓存数据不一致，删除缓存文件重新下载
            if (tempFile.length() > 0 && tempFile.length() != rangeBytes) {
                DLogger.w(getTAG(request), "临时文件长度和缓存数据长度不一致，file(%d), range(%d)", tempFile.length(), rangeBytes);

                if (tempFile.delete()) {
                    DLogger.w(getTAG(request), "删除临时文件");
                }
                else {
                    DLogger.w(getTAG(request), "删除临时文件失败");
                }
                rangeBytes = -1;
            }
        }
        else {
            // 创建父文件夹
            File dir = tempFile.getParentFile();
            if (dir.exists()) {
                if (!dir.isDirectory()) {
                    dir.delete();
                    dir.mkdirs();
                }
            }
            else {
                dir.mkdirs();
            }
        }

        final FileOutputStream out;
        try {
            out = new FileOutputStream(tempFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            // TODO
            throw new DownloadException();
        }
        try {
            com.squareup.okhttp.Request.Builder builder = new com.squareup.okhttp.Request.Builder();
            final String url;
            if (request.uri.getScheme().equalsIgnoreCase("http") || request.uri.getScheme().equalsIgnoreCase("https")) {
                url = request.uri.toString();
            }
            else {
                url = request.uri.getPath();
            }
            builder.url(url);

            if (rangeBytes > 0) {
                builder.addHeader("Range", "bytes="+ rangeBytes + "-");
            }

            DLogger.d(getTAG(request), "开始下载(%s)", request.uri);
            Response response = mOkHttpClient.newCall(builder.build()).execute();
            if (response.isSuccessful()) {
                // Content-Length
                long contentLen = -1;
                long totalLen = -1;
                try {
                    String header = response.header("Content-Length");
                    contentLen = Long.parseLong(header);
                    totalLen = contentLen + rangeBytes;// 文件大小
                } catch (Exception e) {
                    e.printStackTrace();
                }
                DLogger.d(getTAG(request), "Total-Length = %d, Content-Length = %d", totalLen, contentLen);
                if (totalLen != -1) {
                    downloadInfo.fileBytes = totalLen;
                }

                InputStream in = response.body().byteStream();
                int writeBufferSize = 32 * 1024;
                byte[] readBuffer = new byte[8 * 1024];
                int readLen = -1;
                ByteArrayOutputStream bufferOut = new ByteArrayOutputStream();

                long start = SystemClock.elapsedRealtime();
                long readCount = 0;
                while (true) {
                    readCount++;
                    bufferOut.reset();

                    long readStart = SystemClock.elapsedRealtime();
                    do {
                        readLen = in.read(readBuffer);
                        if (readLen > 0) {
                            bufferOut.write(readBuffer, 0, readLen);
                        }

                        // TODO 判断是否下载中断
                    } while (readLen > 0 && bufferOut.size() < writeBufferSize);
                    long readTimes = SystemClock.elapsedRealtime() - readStart;
                    DLogger.v(getTAG(request), "读数据耗时 %d", readTimes);

                    byte[] writeBuffer = bufferOut.toByteArray();
                    if (writeBuffer.length > 0) {
                        long writeStart = SystemClock.elapsedRealtime();
//                        ByteArrayInputStream arrayIn = new ByteArrayInputStream(writeBuffer);
//                        int len = -1;
//                        byte[] arrayBuffer = new byte[8 * 1024];
//                        while ((len = arrayIn.read(arrayBuffer)) != -1) {
//                            out.write(arrayBuffer, 0, len);
//                        }
                        out.write(writeBuffer, 0, writeBuffer.length);

//                        arrayIn.close();

                        DLogger.v(getTAG(request), "写数据耗时 %d", SystemClock.elapsedRealtime() - writeStart);

                        if (readTimes > 0) {
                            DLogger.v(getTAG(request), "下载速度 %s kb/s", writeBuffer.length * 1.0f / 1024 / (readTimes * 1.0f / 1000));
                        }
                    }

                    if (readLen == -1) {
                        break;
                    }
                }

                // TODO 关闭流
                out.flush();
                out.close();

                // 下载结束
                DLogger.d(getTAG(request), "下载总耗时%d ms，当次下载文件长度%d，循环读取%d次", SystemClock.elapsedRealtime() - start, contentLen, readCount);
            }
            else {
                // TODO
                throw new DownloadException();
            }
        } catch (IOException e) {
            e.printStackTrace();

            // TODO
            throw new DownloadException();
        } catch (Exception e) {
            e.printStackTrace();

            // TODO
            throw new DownloadException();
        }
    }

    private String getTAG(Request request) {
        return TAG + "_" + request.key;
    }

}
