package org.aiwen.downloader;

/**
 * Created by wangdan on 16/12/23.
 */
public interface IDownloadObserver {

    Request getRequest();

    void onStatusChanged(Request request);

}
