package org.aiwen.downloader;


import android.content.ContentValues;
import android.net.Uri;

/**
 * 下载请求
 *
 * Created by 王dan on 2016/12/17.
 */
public final class Request {

    long id;// DB中的ID

    public final String key;// 每一个Request唯一Key

    Uri uri;// 下载请求的URI

    Uri fileUri;// 文件保存路径

    ThreadTrace trace;

    final DownloadInfo downloadInfo;

    Request(Uri uri, Uri fileUri) {
        this.uri = uri;
        this.fileUri = fileUri;
        key = KeyGenerator.generateMD5(this.uri.toString());
        downloadInfo = new DownloadInfo();
    }

    public ContentValues getContentValues() {
        ContentValues contentValues = new ContentValues();

        contentValues.put(Downloads.Impl.COLUMN_KEY, key);
        contentValues.put(Downloads.Impl.COLUMN_STATUS, downloadInfo.status);
        contentValues.put(Downloads.Impl.COLUMN_URI, uri.toString());

        return contentValues;
    }

    @Override
    public String toString() {
        return new StringBuffer()
                .append("key = ").append(key)
                .append(", id = ").append(id)
                .append(", status = ").append(downloadInfo.status)
                .append(", uri = ").append(uri.toString())
                .toString();
    }

}
