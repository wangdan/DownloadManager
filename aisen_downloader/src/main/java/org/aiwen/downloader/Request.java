package org.aiwen.downloader;


import android.content.ContentValues;
import android.net.Uri;

/**
 * 下载请求
 *
 * Created by 王dan on 2016/12/17.
 */
public final class Request {

    private int mId;// DB中的ID

    private final String mKey;// 每一个Request唯一Key

    private final Uri mUri;// 下载请求的URI

    private int status = -1;// 下载的状态

    Request(Uri uri) {
        this.mUri = uri;
        mKey = KeyGenerator.generateMD5(mUri.toString());
    }

    void setId(int id) {
        mId = id;
    }

    public int getId() {
        return mId;
    }

    public String getKey() {
        return mKey;
    }

    public Uri getURI() {
        return mUri;
    }

    public ContentValues getContentValues() {
        ContentValues contentValues = new ContentValues();

        contentValues.put(Downloads.Impl.COLUMN_KEY, mKey);
        contentValues.put(Downloads.Impl.COLUMN_STATUS, status);
        contentValues.put(Downloads.Impl.COLUMN_URI, mUri.toString());

        return contentValues;
    }

    @Override
    public String toString() {
        return new StringBuffer()
                .append("key = ").append(mKey)
                .append(", id = ").append(mId)
                .append(", status = ").append(status)
                .append(", uri = ").append(mUri.toString())
                .toString();
    }

}
