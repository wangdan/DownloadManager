package com.tcl.downloader;

import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.LruCache;

import com.tcl.downloader.provider.DownloadInfo;
import com.tcl.downloader.utils.Utils;

import java.io.Serializable;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 用于刷新UI
 *
 * Created by wangdan on 16/6/14.
 */
public final class DownloadController {

    static final String TAG = "DownloadController";

    // 所有注册的Proxy
    private final static Vector<IDownloadSubject> mDownloadProxy = new Vector<>();
    // 下载状态缓存，最多缓存50个
    private final static LruCache<String, DownloadStatus> mStatusCache = new LruCache<>(50);

    private final static Handler mHandler = new Handler(Looper.getMainLooper()) {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (msg.what == 0) {
                String uri = msg.getData().getString("uri");
                DownloadStatus status = (DownloadStatus) msg.getData().getSerializable("status");

                notifyDownloadStatus(uri, status);
            }
        }

    };

    static {
//        mDownloadStatusThread.start();
    }

    private DownloadController() {

    }

    public synchronized static void register(IDownloadSubject callback) {
        if (callback == null) {
            return;
        }

        if (!mDownloadProxy.contains(callback)) {
            mDownloadProxy.add(callback);

            DLogger.v(TAG, "register proxy[%s]", callback.toString());
        }
    }

    public synchronized static void unregister(DownloadProxy callback) {
        if (callback == null) {
            return;
        }

        boolean removed = mDownloadProxy.remove(callback);
        if (removed) {
            DLogger.v(TAG, "unregister proxy[%s]", callback.toString());
        }
    }

    // 查询一次Status状态
    static DownloadStatus queryStatus(String uri) {
        DownloadStatus downloadStatus = mStatusCache.get(Utils.generateMD5(uri));
        if (downloadStatus != null) {
            notifyDownloadStatus(uri, downloadStatus);
        }
        else {
            new QueryStatusTask(uri).start();
        }

        return downloadStatus;
    }

    static void addStatus(String uri, DownloadStatus status) {
        synchronized (mStatusCache) {
            String key = Utils.generateMD5(uri);
            if (mStatusCache.get(key) == null) {
                mStatusCache.put(Utils.generateMD5(uri), status);

                DLogger.v(TAG, "new status[%s]", status.toString());
            }
            else if (mStatusCache.get(key) != status) {
                mStatusCache.get(key).copy(status);
            }
        }
    }

    private static void notifyDownloadStatus(String uri, DownloadStatus status) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            for (IDownloadSubject proxy : mDownloadProxy) {
                proxy.notifyDownload(uri, status);
            }
        }
        else {
            Message message = mHandler.obtainMessage();
            message.what = 0;
            message.getData().putString("uri", uri);
            message.getData().putSerializable("status", status);
            message.sendToTarget();
        }
    }

    public static void refreshDownloadInfo(DownloadInfo downloadInfo) {
        String uri = downloadInfo.mUri;
        DownloadStatus status = mStatusCache.get(Utils.generateMD5(uri));

        synchronized (mStatusCache) {
            if (status == null) {
                status = new DownloadStatus();
                addStatus(uri, status);
            }

            status.status = Utils.translateStatus(downloadInfo.mStatus);
            status.progress = downloadInfo.mCurrentBytes;
            status.total = downloadInfo.mTotalBytes;

        }

        notifyDownloadStatus(uri, status);
    }

    static final ExecutorService mQueryStatusTaskPool = Executors.newSingleThreadExecutor();
    static class QueryStatusTask extends AsyncTask<Void, Void, DownloadStatus> {

        String uri;

        QueryStatusTask(String uri) {
            this.uri = uri;
        }

        @Override
        protected DownloadStatus doInBackground(Void... params) {
            DownloadStatus downloadStatus = mStatusCache.get(Utils.generateMD5(uri));

            if (downloadStatus == null) {
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterByURI(uri);
                DownloadManager downloadManager = DownloadManager.getInstance();
                if (downloadManager != null) {
                    Cursor c = downloadManager.query(query);
                    if (c != null) {
                        try {
                            if (c.moveToFirst()) {
                                downloadStatus = mStatusCache.get(Utils.generateMD5(uri));
                                if (downloadStatus == null) {
                                    downloadStatus = new DownloadStatus();
                                    addStatus(uri, downloadStatus);
                                }

                                downloadStatus.status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
                                downloadStatus.progress = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                                downloadStatus.total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                            }
                        } catch (Throwable e) {
                            DLogger.printExc(DownloadController.class, e);
                        } finally {
                            try {
                                c.close();
                            } catch (Throwable e) {
                                DLogger.printExc(DownloadController.class, e);
                            }
                        }
                    }
                }
            }

            return downloadStatus;
        }

        @Override
        protected void onPostExecute(DownloadStatus downloadStatus) {
            super.onPostExecute(downloadStatus);

            notifyDownloadStatus(uri, downloadStatus);
        }

        void start() {
            executeOnExecutor(mQueryStatusTaskPool);
        }

    }

    public static class DownloadStatus implements Serializable {

        private static final long serialVersionUID = 6348384894928694134L;

        public int status = -1;

        public int error = -1;

        public long total = -1;

        public long progress = -1;

        @Override
        public String toString() {
            return String.format("status[%d], error[%d], total[%s], progress[%s]", status, error, total + "", progress + "");
        }

        void copy(DownloadStatus newStatus) {
            status = newStatus.status;
            error = newStatus.error;
            total = newStatus.total;
            progress = newStatus.progress;
        }

    }

}
