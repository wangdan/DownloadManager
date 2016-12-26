package org.aiwen.downloader;

/**
 * Created by 王dan on 2016/12/19.
 */

public class DownloadInfo {

    final Request request;

    String title;// 标题

    String destination;// 副标题

    int visibility = Request.VISIBILITY_VISIBLE;// 通知栏可见

    int status = -1;// 下载的状态

    String error = "";// 失败消息

    long rangeBytes;// 文件已下载长度

    long fileBytes = -1;// 文件总长度

    int numFailed;// 下载失败次数

    long lastMod;// 最后修改时间(ms)

    long retryAfter;// 等待这个时间后再重试

    public DownloadInfo(Request request) {
        this.request = request;
    }

    void writeToDatabase() {
        Hawk hawk = Hawk.getInstance();

        if (hawk != null) {
            hawk.notifyStatus(request);

            hawk.db.update(request);
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

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public long getRangeBytes() {
        return rangeBytes;
    }

    public long getFileBytes() {
        return fileBytes;
    }

    public int getNumFailed() {
        return numFailed;
    }

    public long getLastMod() {
        return lastMod;
    }

    public long getRetryAfter() {
        return retryAfter;
    }

}
