package org.aiwen.downloader;


import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import org.aiwen.downloader.utils.Constants;
import org.aiwen.downloader.utils.Utils;

/**
 * 下载请求
 *
 * Created by 王dan on 2016/12/17.
 */
public final class Request {

    long id;// DB中的ID

    public final String key;// 每一个Request唯一Key

    final Uri uri;// 下载请求的URI

    final Uri fileUri;// 文件保存路径

    ThreadTrace trace;

    DownloadThread thread;

    final DownloadInfo downloadInfo;

    Request(Uri uri, Uri fileUri) {
        this.uri = uri;
        this.fileUri = fileUri;
        key = KeyGenerator.generateMD5(this.uri.toString());
        downloadInfo = new DownloadInfo(this);
    }

    boolean isReadyToDownload() {
        if (isRunning()) {
            return false;
        }

        switch (downloadInfo.status) {
            // 可以下载
            case -1:
            case Downloads.Status.STATUS_PENDING:
            case Downloads.Status.STATUS_RUNNING:
                DLogger.d(Utils.getDownloaderTAG(this), "准备下载, status(%d)", downloadInfo.status);

                return true;
            // 等待重试
            case Downloads.Status.STATUS_WAITING_TO_RETRY:
                // download was waiting for a delayed restart
                final long now = Utils.realtime();
                boolean retry = downloadInfo.restartTime(now) <= now;

                DLogger.w(Utils.getDownloaderTAG(this), "第 %d 次尝试下载重连", downloadInfo.numFailed);

                return retry;
            // 等待网络重连
            case Downloads.Status.STATUS_WAITING_FOR_NETWORK:
            case Downloads.Status.STATUS_QUEUED_FOR_WIFI:
                return Utils.isWifiActive();
            default:
                return false;
        }
    }

    boolean isRunning() {
        return thread != null;
    }

    static Request copy(Request request) {
        Request copyRequest = new Request(request.uri, request.fileUri);

        return copyRequest;
    }

    static Request create(Cursor cursor) {
        try {
            String uri = cursor.getString(cursor.getColumnIndexOrThrow(Downloads.Impl.COLUMN_URI));
            String fileUri = cursor.getString(cursor.getColumnIndexOrThrow(Downloads.Impl._DATA));

            Request request = new Request(Uri.parse(uri), Uri.parse(fileUri));
            request.set(cursor);

            DLogger.v(Constants.TAG, "SetRequest(%s)", request.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    void set(Cursor cursor) {
        try {
            id = cursor.getInt(cursor.getColumnIndexOrThrow(Downloads.Impl._ID));
            downloadInfo.status = cursor.getInt(cursor.getColumnIndexOrThrow(Downloads.Impl.COLUMN_STATUS));
            downloadInfo.rangeBytes = cursor.getLong(cursor.getColumnIndexOrThrow(Downloads.Impl.COLUMN_CURRENT_BYTES));
            downloadInfo.fileBytes = cursor.getLong(cursor.getColumnIndexOrThrow(Downloads.Impl.COLUMN_TOTAL_BYTES));

            DLogger.v(Constants.TAG, "SetRequest(%s)", toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    ContentValues getContentValues() {
        ContentValues contentValues = new ContentValues();

        contentValues.put(Downloads.Impl.COLUMN_KEY, key);
        // 状态
        contentValues.put(Downloads.Impl.COLUMN_STATUS, downloadInfo.status);
        // URI
        contentValues.put(Downloads.Impl.COLUMN_URI, uri.toString());
        // FILE URI
        contentValues.put(Downloads.Impl._DATA, fileUri.toString());
        // 下载进度
        contentValues.put(Downloads.Impl.COLUMN_CURRENT_BYTES, downloadInfo.rangeBytes);
        // 文件大小
        contentValues.put(Downloads.Impl.COLUMN_TOTAL_BYTES, downloadInfo.fileBytes);

        return contentValues;
    }

    public DownloadInfo getDownloadInfo() {
        return downloadInfo;
    }

    public ThreadTrace getTrace() {
        return trace;
    }

    @Override
    public String toString() {
        return new StringBuffer()
                .append("key = ").append(key)
                .append(", id = ").append(id)
                .append(", status = ").append(Downloads.Status.statusToString(downloadInfo.status))
                .append(", numFailed = ").append(downloadInfo.numFailed)
                .append(", retryAfter = ").append(downloadInfo.retryAfter)
                .append(", rangeBytes = ").append(downloadInfo.rangeBytes)
                .append(", fileBytes = ").append(downloadInfo.fileBytes)
                .append(", uri = ").append(uri.toString())
                .toString();
    }

}
