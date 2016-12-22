package org.aiwen.downloader;

/**
 * Created by 王dan on 2016/12/19.
 */

public class Configuration {

    private int maxConcurrentDownloadsAllowed = 3;// 最大下载任务

    public int getMaxConcurrentDownloadsAllowed() {
        return maxConcurrentDownloadsAllowed;
    }

    public void setMaxConcurrentDownloadsAllowed(int maxConcurrentDownloadsAllowed) {
        this.maxConcurrentDownloadsAllowed = maxConcurrentDownloadsAllowed;
    }

}
