package org.aisen.download;

import org.aisen.download.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by wangdan on 16/6/15.
 */
public class DownloadProxy implements IDownloadSubject {

    private final Map<String, List<IDownloadObserver>> observers;

    public DownloadProxy() {
        observers = new HashMap<>();
    }

    @Override
    final public void attach(IDownloadObserver observer) {
        if (observer == null || observer.downloadURI() == null || observer.downloadFileURI() == null)
            return;

        String key = Utils.generateMD5(observer.downloadURI(), observer.downloadFileURI());

        synchronized (observers) {
            if (!observers.containsKey(observer)) {
                observers.put(key, new ArrayList<IDownloadObserver>());
            }
            List<IDownloadObserver> observerList = observers.get(key);
            if (!observerList.contains(observer)) {
                observerList.add(observer);
            }
        }

        // 查询一次状态
        if (DownloadManager.getInstance() != null) {
            DownloadManager.getInstance().queryAndPublish(key, true);
        }
    }

    @Override
    final public void detach(IDownloadObserver observer) {
        if (observer == null || observer.downloadURI() == null || observer.downloadFileURI() == null)
            return;

        String key = Utils.generateMD5(observer.downloadURI(), observer.downloadFileURI());

        synchronized (observers) {
            if (observers.containsKey(observer)) {
                List<IDownloadObserver> observerList = observers.get(key);

                if (observerList.contains(observer)) {
                    observerList.remove(observer);
                }

                if (observerList.size() == 0) {
                    observers.remove(key);
                }
            }
        }
    }

    @Override
    public void publish(DownloadMsg downloadMsg) {
        String key = downloadMsg.getKey();

        List<IDownloadObserver> observerList = observers.get(key);
        if (observerList != null) {
            for (IDownloadObserver observer : observerList) {
                observer.onPublish(downloadMsg);
            }
        }
    }

}
