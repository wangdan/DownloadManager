package org.aiwen.downloader;


import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import org.aiwen.downloader.utils.Constants;

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

    boolean isRunning() {
        return thread != null;
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

    public ContentValues getContentValues() {
        ContentValues contentValues = new ContentValues();

        contentValues.put(Downloads.Impl.COLUMN_KEY, key);
        // 状态
        contentValues.put(Downloads.Impl.COLUMN_STATUS, downloadInfo.status);
        // URI
        contentValues.put(Downloads.Impl.COLUMN_URI, uri.toString());
        // 下载进度
        contentValues.put(Downloads.Impl.COLUMN_CURRENT_BYTES, downloadInfo.rangeBytes);
        // 文件大小
        contentValues.put(Downloads.Impl.COLUMN_TOTAL_BYTES, downloadInfo.fileBytes);

        return contentValues;
    }

    @Override
    public String toString() {
        return new StringBuffer()
                .append("key = ").append(key)
                .append(", id = ").append(id)
                .append(", status = ").append(downloadInfo.status)
                .append(", rangeBytes = ").append(downloadInfo.rangeBytes)
                .append(", fileBytes = ").append(downloadInfo.fileBytes)
                .append(", uri = ").append(uri.toString())
                .toString();
    }

}
