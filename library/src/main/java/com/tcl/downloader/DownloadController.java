package com.tcl.downloader;

import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.LruCache;

import com.tcl.downloader.downloads.Downloads;
import com.tcl.downloader.provider.Constants;
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

    static final String TAG = Constants.TAG + "_DownloadController";

    // 所有注册的Proxy
    private final static Vector<IDownloadSubject> mDownloadProxy = new Vector<>();
    // 下载状态缓存，最多缓存50个
    private final static LruCache<String, DownloadStatus> mStatusCache = new LruCache<>(100);

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
            DLogger.v(TAG, "queryStatus, notifyDownloadStatus(%s, %s)",
                                Downloads.Impl.translateStatus(downloadStatus.status), uri);

            notifyDownloadStatus(uri, downloadStatus);
        }
        else {
            DLogger.v(TAG, "queryStatus, QueryStatusTask(%s)", uri);

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
                if (status != null)
                    DLogger.v(TAG, "notifyDownloadStatus, Info[%s], Status[%s]", status.title, Downloads.Impl.statusToString(status.status));
                else
                    DLogger.v(TAG, "notifyDownloadStatus, DownloadStatus is null");

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

    public static void refreshDownloadDeleted(DownloadInfo downloadInfo) {
        String uri = downloadInfo.mUri;
        DownloadStatus status = mStatusCache.get(Utils.generateMD5(uri));

        synchronized (mStatusCache) {
            if (status == null) {
                status = new DownloadStatus();
                addStatus(uri, status);
            }

            status.status = -1;
            status.progress = -1;
            status.total = -1;
            status.id = downloadInfo.mId;
            status.uri = downloadInfo.mUri;
            status.destination = 0;
            status.title = downloadInfo.mTitle;
            status.description = null;
            status.localUri = downloadInfo.mFileName;
            status.deleted = true;
        }

        notifyDownloadStatus(uri, status);
    }

    public static void refreshDownloadInfo(DownloadInfo downloadInfo) {
        String uri = downloadInfo.mUri;
        DownloadStatus status = mStatusCache.get(Utils.generateMD5(uri));

        synchronized (mStatusCache) {
            if (status == null) {
                status = new DownloadStatus();
                addStatus(uri, status);
            }

            status.status = Downloads.Impl.translateStatus(downloadInfo.mStatus);
            status.progress = downloadInfo.mCurrentBytes;
            status.total = downloadInfo.mTotalBytes;
            status.id = downloadInfo.mId;
            status.uri = downloadInfo.mUri;
            status.destination = downloadInfo.mDestination;
            status.title = downloadInfo.mTitle;
            status.description = downloadInfo.mDescription;
            status.localUri = downloadInfo.mFileName;
            status.deleted = downloadInfo.mDeleted;
            status.paused = downloadInfo.mControl;
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
                                do {
                                    downloadStatus = mStatusCache.get(Utils.generateMD5(uri));
                                    if (downloadStatus == null) {
                                        downloadStatus = new DownloadStatus();
                                        addStatus(uri, downloadStatus);
                                    }

                                    downloadStatus.id = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_ID));
                                    downloadStatus.uri = c.getString(c.getColumnIndexOrThrow(DownloadManager.COLUMN_URI));
//                                downloadStatus.fileName = c.getString(c.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_FILENAME));
                                    downloadStatus.destination = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_DESCRIPTION));
                                    downloadStatus.status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                                    downloadStatus.total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                                    downloadStatus.progress = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                                    downloadStatus.title = c.getString(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE));
                                    downloadStatus.description = c.getString(c.getColumnIndexOrThrow(DownloadManager.COLUMN_DESCRIPTION));
                                    downloadStatus.reason = c.getString(c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON));
                                    downloadStatus.localUri = c.getString(c.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI));
                                    downloadStatus.deleted = c.getInt(c.getColumnIndexOrThrow(Downloads.Impl.COLUMN_DELETED)) == 1;
                                    downloadStatus.reason = c.getString(c.getColumnIndexOrThrow(Downloads.Impl.COLUMN_ERROR_MSG));
                                    downloadStatus.paused = c.getInt(c.getColumnIndexOrThrow(Downloads.Impl.COLUMN_CONTROL));
                                } while (c.moveToNext());
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

            if (downloadStatus == null) {
                downloadStatus = new DownloadStatus();

                addStatus(Utils.generateMD5(uri), downloadStatus);
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

        public long id;
        public String uri;
        public String localUri;
        public int destination;
        public int status = -1;
        public long total;
        public long progress;
        public int allowedNetworkTypes;
        public String title;
        public String description;
        public String reason;
        public boolean deleted;
        public int paused;// 1 == true

//        public

        @Override
        public String toString() {
            return String.format("status[%d], reason[%s], total[%s], progress[%s], local[%s]", status, reason, total + "", progress + "", localUri);
        }

        void copy(DownloadStatus newStatus) {
            id = newStatus.id;
            uri = newStatus.uri;
            localUri = newStatus.localUri;
            destination = newStatus.destination;
            status = newStatus.status;
            total = newStatus.total;
            progress = newStatus.progress;
            allowedNetworkTypes = newStatus.allowedNetworkTypes;
            title = newStatus.title;
            description = newStatus.description;
            reason = newStatus.reason;
            deleted = newStatus.deleted;
        }

    }

}
