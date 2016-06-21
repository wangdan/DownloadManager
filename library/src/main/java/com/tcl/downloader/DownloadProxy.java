package com.tcl.downloader;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wangdan on 16/6/15.
 */
public class DownloadProxy implements IDownloadSubject {

    static final String TAG = "DownloadProxy";

    private final List<IDownloadObserver> observers;

    public DownloadProxy() {
        observers = new ArrayList<>();
    }

    @Override
    final public void attach(IDownloadObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);

            // 查询一次状态
            if (!TextUtils.isEmpty(observer.downloadURI()))
                DownloadController.queryStatus(observer.downloadURI());
        }
    }

    @Override
    final public void detach(IDownloadObserver observer) {
        if (observer != null && !observers.contains(observer))
            observers.remove(observer);
    }

    @Override
    public void notifyDownload(String uri, DownloadController.DownloadStatus status) {
        if (status != null)
            DLogger.v(TAG, "status[%d], progress[%s], total[%s], uri[%s]", status.status, status.progress + "", status.total + "", uri);

        for (IDownloadObserver observer : observers) {
            if (!TextUtils.isEmpty(observer.downloadURI()) && observer.downloadURI().equals(uri)) {
                if (status == null || status.status == -1 || status.deleted) {
                    observer.onDownloadPrepare();
                }
                else {
                    observer.onDownloadChanged(status);
                }
            }
        }
    }

}
