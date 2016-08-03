package org.aisen.download;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import org.aisen.download.core.DownloadInfo;
import org.aisen.download.utils.Constants;
import org.aisen.download.utils.DLogger;

import java.util.Vector;

/**
 * 用于刷新UI
 *
 * Created by wangdan on 16/6/14.
 */
public final class DownloadController {

    static final String TAG = Constants.TAG + "_DownloadController";

    // 所有注册的Proxy
    private final Vector<IDownloadSubject> mDownloadProxy = new Vector<>();

    private final Handler mHandler = new Handler(Looper.getMainLooper()) {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (msg.what == 0) {
                DownloadMsg downloadMsg = (DownloadMsg) msg.getData().getSerializable("msg");

                publishDownload(downloadMsg);
            }
        }

    };

    DownloadController() {

    }

    public synchronized void register(IDownloadSubject callback) {
        if (callback == null) {
            return;
        }

        if (!mDownloadProxy.contains(callback)) {
            mDownloadProxy.add(callback);

            DLogger.v(TAG, "register proxy[%s]", callback.toString());
        }
    }

    public synchronized void unregister(IDownloadSubject callback) {
        if (callback == null) {
            return;
        }

        boolean removed = mDownloadProxy.remove(callback);
        if (removed) {
            DLogger.v(TAG, "unregister proxy[%s]", callback.toString());
        }
    }

    void publishDownload(DownloadMsg downloadMsg) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            for (IDownloadSubject proxy : mDownloadProxy) {
                proxy.publish(downloadMsg);
            }
        }
        else {
            Message message = mHandler.obtainMessage();
            message.what = 0;
            message.getData().putSerializable("msg", downloadMsg);
            message.sendToTarget();
        }
    }

    void publishDownload(DownloadInfo downloadInfo) {
        publishDownload(new DownloadMsg(downloadInfo));
    }

}
