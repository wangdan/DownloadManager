package com.tcl.downloader;

/**
 * Created by wangdan on 16/6/15.
 */
public interface IDownloadSubject {

    void attach(IDownloadObserver observer);

    void detach(IDownloadObserver observer);

    void notifyDownload(String uri, DownloadController.DownloadStatus status);

}
