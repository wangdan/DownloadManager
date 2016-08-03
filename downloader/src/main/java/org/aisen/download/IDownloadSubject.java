package org.aisen.download;

/**
 * Created by wangdan on 16/6/15.
 */
public interface IDownloadSubject {

    void attach(IDownloadObserver observer);

    void detach(IDownloadObserver observer);

    void publish(DownloadMsg downloadMsg);

}
