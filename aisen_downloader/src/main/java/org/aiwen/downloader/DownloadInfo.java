package org.aiwen.downloader;

/**
 * Created by 王dan on 2016/12/19.
 */

public class DownloadInfo {

    private final Request mRequest;

    int status = -1;// 下载的状态

    String error = "";// 失败消息

    long rangeBytes;// 文件已下载长度

    long fileBytes = -1;// 文件总长度

    int numFailed;// 下载失败次数

    long lastMod;// 最后修改时间(ms)

    long retryAfter;// 等待这个时间后再重试

    public DownloadInfo(Request request) {
        mRequest = request;
    }

    void writeToDatabase(DownloadDB db, DownloadService service) {
        if (db != null) {
            db.update(mRequest);
        }
        if (service != null) {
            service.enqueueNotify(mRequest);
        }
    }

    /**
     * 每次失败后，准备
     */
    long restartTime(long now) {
        if (numFailed == 0) {
            return now;
        }
        if (retryAfter > 0) {
            return lastMod + retryAfter;
        }

        return Long.MAX_VALUE;
    }

}
