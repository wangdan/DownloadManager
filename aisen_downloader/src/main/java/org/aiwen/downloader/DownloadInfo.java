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

    public DownloadInfo(Request request) {
        mRequest = request;
    }

    void writeToDatabase() {
        Hawk hawk = Hawk.getInstance();
        if (hawk != null) {
            hawk.db.update(mRequest);
        }
    }

}
