package org.aisen.downloader;

/**
 * Created by wangdan on 16/6/15.
 */
public interface IDownloadObserver {

    String downloadURI();

    void onDownloadInit();

    void onDownloadChanged(DownloadController.DownloadStatus status);

}
