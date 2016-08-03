package org.aisen.download;

import android.net.Uri;

import org.aisen.download.core.DownloadInfo;
import org.aisen.download.core.Downloads;

import java.io.Serializable;

/**
 * Created by wangdan on 16/8/3.
 */
public final class DownloadMsg implements Serializable {

    private static final long serialVersionUID = 1749484931688943470L;

    private final String key;// 组合主键
    private final String uri;// 下载地址
    private final String filePath;// 最终保存文件地址
    private final int control;// 下载、暂停等控制操作
    private final int status;// 状态
    private final String errorMsg;// 失败消息
    private final long totalBytes;// 文件大小
    private final long currentBytes;// 下载进度

    DownloadMsg(String key) {
        this.key = key;
        uri = null;
        filePath = null;
        control = -1;
        status = -1;
        errorMsg = null;
        totalBytes = 0;
        currentBytes = 0;
    }

    DownloadMsg(DownloadInfo info) {
        key = info.mKey;
        uri = info.mUri;
        filePath = info.mFilePath;
        control = info.mControl;
        status = info.mStatus;
        errorMsg = info.mErrorMsg;
        totalBytes = info.mTotalBytes;
        currentBytes = info.mCurrentBytes;
    }

    public boolean isNull() {
        return uri == null && filePath == null;
    }

    public String getKey() {
        return key;
    }

    public int getStatus() {
        return translateStatus(status);
    }

    public Uri getUri() {
        return Uri.parse(uri);
    }

    public Uri getFilePath() {
        return Uri.parse(filePath);
    }

    public long getCurrent() {
        return currentBytes;
    }

    public long getTotal() {
        return totalBytes;
    }

    public long getReason() {
        switch (translateStatus(status)) {
            case DownloadManager.STATUS_FAILED:
                return getErrorCode(status);

            case DownloadManager.STATUS_WAITING:
                return getPausedReason(status);

            default:
                return 0; // arbitrary value when status is not an error
        }
    }

    public String status2String() {
        return Downloads.Impl.statusToString(status);
    }

    private long getPausedReason(int status) {
        switch (status) {
            case Downloads.Impl.STATUS_WAITING_TO_RETRY:
                return DownloadManager.PAUSED_WAITING_TO_RETRY;

            case Downloads.Impl.STATUS_WAITING_FOR_NETWORK:
                return DownloadManager.PAUSED_WAITING_FOR_NETWORK;

            case Downloads.Impl.STATUS_QUEUED_FOR_WIFI:
                return DownloadManager.PAUSED_QUEUED_FOR_WIFI;

            default:
                return DownloadManager.PAUSED_UNKNOWN;
        }
    }

    private long getErrorCode(int status) {
        if ((400 <= status && status < Downloads.Impl.MIN_ARTIFICIAL_ERROR_STATUS)
                || (500 <= status && status < 600)) {
            // HTTP status code
            return status;
        }

        switch (status) {
            case Downloads.Impl.STATUS_FILE_ERROR:
                return DownloadManager.ERROR_FILE_ERROR;

            case Downloads.Impl.STATUS_UNHANDLED_HTTP_CODE:
            case Downloads.Impl.STATUS_UNHANDLED_REDIRECT:
                return DownloadManager.ERROR_UNHANDLED_HTTP_CODE;

            case Downloads.Impl.STATUS_HTTP_DATA_ERROR:
                return DownloadManager.ERROR_HTTP_DATA_ERROR;

            case Downloads.Impl.STATUS_TOO_MANY_REDIRECTS:
                return DownloadManager.ERROR_TOO_MANY_REDIRECTS;

            case Downloads.Impl.STATUS_INSUFFICIENT_SPACE_ERROR:
                return DownloadManager.ERROR_INSUFFICIENT_SPACE;

            case Downloads.Impl.STATUS_DEVICE_NOT_FOUND_ERROR:
                return DownloadManager.ERROR_DEVICE_NOT_FOUND;

            case Downloads.Impl.STATUS_CANNOT_RESUME:
                return DownloadManager.ERROR_CANNOT_RESUME;

            case Downloads.Impl.STATUS_FILE_ALREADY_EXISTS_ERROR:
                return DownloadManager.ERROR_FILE_ALREADY_EXISTS;

            default:
                return DownloadManager.ERROR_UNKNOWN;
        }
    }

    private int translateStatus(int status) {
        return Downloads.Impl.translateStatus(status);
    }

}
