package com.tcl.downloader;

import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.LruCache;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
    // 死循环查询下载状态的线程
    private final static DownloadStatusThread mDownloadStatusThread = new DownloadStatusThread();

    static {
        mDownloadStatusThread.start();
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
        DownloadStatus downloadStatus = mStatusCache.get(generateMD5(uri));
        if (downloadStatus != null) {
            refreshDownloasStatus(uri, downloadStatus);
        }
        else {
            new QueryStatusTask(uri).start();
        }

        return downloadStatus;
    }

    synchronized static void addStatus(String uri, DownloadStatus status) {
        String key = generateMD5(uri);
        if (mStatusCache.get(key) == null) {
            mStatusCache.put(generateMD5(uri), status);

            DLogger.v(TAG, "new status[%s]", status.toString());
        }
        else if (mStatusCache.get(key) != status) {
            mStatusCache.get(key).copy(status);
        }
    }

    private static void refreshDownloasStatus(String uri, DownloadStatus status) {
        for (IDownloadSubject proxy : mDownloadProxy) {
            proxy.notifyDownload(uri, status);
        }
    }

    static void onDestory() {
        mDownloadStatusThread.onDestory();
    }

    static final ExecutorService mQueryStatusTaskPool = Executors.newSingleThreadExecutor();
    static class QueryStatusTask extends AsyncTask<Void, Void, DownloadStatus> {

        String uri;

        QueryStatusTask(String uri) {
            this.uri = uri;
        }

        @Override
        protected DownloadStatus doInBackground(Void... params) {
            DownloadStatus downloadStatus = mStatusCache.get(generateMD5(uri));

            if (downloadStatus == null) {
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterByURI(uri);
                DownloadManager downloadManager = DownloadManager.getInstance();
                if (downloadManager != null) {
                    Cursor c = downloadManager.query(query);
                    if (c != null) {
                        try {
                            if (c.moveToFirst()) {
                                downloadStatus = DownloadStatusThread.getStatus(uri, c);

                                addStatus(uri, downloadStatus);
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

            refreshDownloasStatus(uri, downloadStatus);
        }

        void start() {
            executeOnExecutor(mQueryStatusTaskPool);
        }

    }

    static class DownloadStatusThread extends Thread {

        private boolean destory = false;

        private Handler mHandler = new Handler(Looper.getMainLooper()) {

            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                String uri = msg.getData().getString("uri");
                DownloadStatus status = (DownloadStatus) msg.getData().getSerializable("status");

                refreshDownloasStatus(uri, status);
            }

        };

        static DownloadStatus getStatus(String uri, Cursor c) {
            DownloadStatus downloadStatus = mStatusCache.get(generateMD5(uri));
            if (downloadStatus == null) {
                downloadStatus = new DownloadStatus();
            }

            downloadStatus.status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
            downloadStatus.progress = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
            downloadStatus.total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
            //                                    downloadStatus.error = DownloadManager.column_

            return downloadStatus;
        }

        @Override
        public void run() {
            super.run();

            while (!destory) {
                DownloadManager.Query query = new DownloadManager.Query();
                DownloadManager downloadManager = DownloadManager.getInstance();
                if (downloadManager != null) {
                    Cursor c = downloadManager.query(query);
                    if (c != null) {
                        try {
                            if (c.moveToFirst()) {
                                do {
                                    String uri = c.getString(c.getColumnIndex(DownloadManager.COLUMN_URI));

                                    if (TextUtils.isEmpty(uri)) {
                                        continue;
                                    }

                                    // 获取最新的状态
                                    DownloadStatus downloadStatus = getStatus(uri, c);
                                    addStatus(uri, downloadStatus);

                                    DLogger.v(TAG, "status[%d], progress[%s], total[%s], uri[%s]", downloadStatus.status, downloadStatus.progress + "", downloadStatus.total + "", uri);

                                    // 刷新UI
                                    Message message = mHandler.obtainMessage();
                                    message.getData().putString("uri", uri);
                                    message.getData().putSerializable("status", downloadStatus);
                                    message.sendToTarget();
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

                    try {
                        Thread.sleep(200);
                    } catch (Throwable e) {
                        DLogger.printExc(DownloadController.class, e);
                    }
                }
            }
        }

        void onDestory() {
            destory = true;
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

//    public interface DownloadProxy {
//
//        void onNewStatus(DownloadStatus status);

//        void onInit();
//
//        void onProgress(long progress, long total);
//
//        void onPaused();
//
//        void onPending();
//
//        void onSuccessful();
//
//        void onFailed(int error);
//
//        String generateKey(String uri);

//    }

    static String generateMD5(String key) {
        try {
            MessageDigest e = MessageDigest.getInstance("MD5");
            e.update(key.getBytes());
            byte[] bytes = e.digest();
            StringBuilder sb = new StringBuilder();

            for(int i = 0; i < bytes.length; ++i) {
                String hex = Integer.toHexString(255 & bytes[i]);
                if(hex.length() == 1) {
                    sb.append('0');
                }

                sb.append(hex);
            }

            return sb.toString();
        } catch (NoSuchAlgorithmException var6) {
            return String.valueOf(key.hashCode());
        }
    }

}
