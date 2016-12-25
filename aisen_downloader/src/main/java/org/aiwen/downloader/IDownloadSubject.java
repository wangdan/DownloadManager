package org.aiwen.downloader;

/**
 * Created by wangdan on 16/12/23.
 */
public interface IDownloadSubject {

    void attach(IDownloadObserver observer);

    void detach(IDownloadObserver observer);

    void notifyStatus(Request request);

}
