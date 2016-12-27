package org.aiwen.downloader;

import java.io.File;

/**
 * Created by 王dan on 2016/12/19.
 */

public class Configuration {

    private int maxConcurrentDownloadsAllowed = 3;// 最大下载任务

    IFileCheckCallback fileCheckCallback;// 文件处理回调

    IStatusCallback statusCallback;// 所有下载Request状态回调

    public Configuration() {
        fileCheckCallback = new DefFileCheckCallback();
    }

    public int getMaxConcurrentDownloadsAllowed() {
        return maxConcurrentDownloadsAllowed;
    }

    public void setMaxConcurrentDownloadsAllowed(int maxConcurrentDownloadsAllowed) {
        this.maxConcurrentDownloadsAllowed = maxConcurrentDownloadsAllowed;
    }

    public void setFileCheckCallback(IFileCheckCallback fileCheckCallback) {
        this.fileCheckCallback = fileCheckCallback;
    }

    public void setStatusCallback(IStatusCallback statusCallback) {
        this.statusCallback = statusCallback;
    }

    /**
     * 当下载文件已存在时，回调验证文件的合法性，如果返回false:删除本地文件
     *
     */
    public interface IFileCheckCallback {

        /**
         * 验证文件是否合法
         *
         * @param file
         * @return true:文件合法
         */
        boolean onFileCheck(Request request, File file);

    }

    public interface IStatusCallback {

        void onStatusChanged(Request request);

    }

    private static class DefFileCheckCallback implements IFileCheckCallback {

        @Override
        public boolean onFileCheck(Request request, File file) {
            return true;
        }

    }

}
