package org.aisen.download;

import android.net.Uri;

/**
 * Created by wangdan on 16/6/15.
 */
public interface IDownloadObserver {

    /**
     * 下载地址
     *
     * @return
     */
    Uri downloadURI();

    /**
     * 下载文件地址
     *
     * @return
     */
    Uri downloadFileURI();

    /**
     * 下载状态广播
     *
     * @param downloadMsg
     */
    void onPublish(DownloadMsg downloadMsg);

}
