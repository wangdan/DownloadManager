package org.aiwen.downloader;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by wangdan on 16/12/23.
 */
class RequestSubject implements IDownloadSubject {

    private final List<SoftReference<IDownloadObserver>> observers;

    public RequestSubject() {
        observers = new ArrayList<>();
    }

    @Override
    public void attach(IDownloadObserver observer) {
        synchronized (this) {
            boolean exist = false;

            for (int i = 0; i < observers.size(); i++) {
                if (observers.get(i).get() == observer) {
                    exist = true;

                    break;
                }
            }

            if (!exist) {
                observers.add(new SoftReference<>(observer));
            }
        }
    }

    @Override
    public void detach(IDownloadObserver observer) {
        synchronized (this) {
            for (int i = 0; i < observers.size(); i++) {
                if (observers.get(i).get() == observer) {
                    observers.remove(i);

                    break;
                }
            }
        }
    }

    @Override
    public void notifyStatus(Request request) {
        for (int i = 0; i < observers.size(); i++) {
            IDownloadObserver observer = observers.get(i).get();

            if (observer != null) {
                observer.onStatusChanged(request);
            }
        }
    }

    boolean empty() {
        return observers.size() == 0;
    }

}
