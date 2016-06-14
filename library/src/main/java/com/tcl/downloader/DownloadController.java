package com.tcl.downloader;

import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 用于刷新UI
 *
 * Created by wangdan on 16/6/14.
 */
public final class DownloadController {

    private final static Hashtable<String, Vector<DownloadProxy>> mDownloadProxy = new Hashtable<>();

    private final static Hashtable<String, DownloadStatus> mDownloadStatus = new Hashtable<>();

    private final static DownloadStatusThread mDownloadStatusThread = new DownloadStatusThread();

    private final static Hashtable<String, String> uriKeyMap = new Hashtable<>();

    static {
        mDownloadStatusThread.start();
    }

    private DownloadController() {

    }

    public synchronized static void register(String url, DownloadProxy callback) {
        if (TextUtils.isEmpty(url) || callback == null) {
            return;
        }

        String key = callback.generateKey(url);
        uriKeyMap.put(url, key);
        if (TextUtils.isEmpty(key)) {
            return;
        }

        Vector<DownloadProxy> downloadProxyVector = mDownloadProxy.get(key);
        if (!mDownloadProxy.contains(key)) {
            downloadProxyVector = new Vector<>();
            mDownloadProxy.put(key, downloadProxyVector);
        }

        if (!downloadProxyVector.contains(callback)) {
            downloadProxyVector.add(callback);
        }

        DownloadStatus downloadStatus = mDownloadStatus.get(key);
        if (downloadStatus != null) {
            refreshDownloasStatus(key, downloadStatus);
        }
        else {
            new QueryStatusTask(key, url).executeOnExecutor(mQueryStatusTaskPool);
        }
    }

    public synchronized static void unregister(String url, DownloadProxy callback) {
        if (TextUtils.isEmpty(url) || callback == null) {
            return;
        }

        String key = callback.generateKey(url);
        if (TextUtils.isEmpty(key)) {
            return;
        }

        Vector<DownloadProxy> downloadProxyVector = mDownloadProxy.get(key);
        if (downloadProxyVector != null) {
            if (downloadProxyVector.contains(callback)) {
                mDownloadProxy.remove(callback);
            }
            if (downloadProxyVector.size() == 0) {
                mDownloadProxy.remove(downloadProxyVector);

                mDownloadStatus.remove(key);

                uriKeyMap.remove(url);
            }
        }
    }

    private static void refreshDownloasStatus(String key, DownloadStatus status) {

        Vector<DownloadProxy> downloadProxyVector = mDownloadProxy.get(key);
        if (downloadProxyVector != null && downloadProxyVector.size() > 0) {
            for (DownloadProxy proxy : downloadProxyVector) {
                // 更新状态
                proxy.onNewStatus(status);

                // 失败
                if (status.status == DownloadManager.STATUS_FAILED) {
                    proxy.onFailed(status.error);
                }
                // 成功
                else if (status.status == DownloadManager.STATUS_SUCCESSFUL) {
                    proxy.onSuccessful();
                }
                // 暂停
                else if (status.status == DownloadManager.STATUS_PAUSED) {
                    proxy.onPaused();
                }
                // 等待
                else if (status.status == DownloadManager.STATUS_PENDING) {
                    proxy.onPending();
                }
                // 下载中
                else if (status.status == DownloadManager.STATUS_RUNNING) {
                    proxy.onProgress(status.progress, status.total);
                }
            }
        }
    }

    static void onDestory() {
        mDownloadStatusThread.onDestory();
    }

    static final ExecutorService mQueryStatusTaskPool = Executors.newSingleThreadExecutor();
    static class QueryStatusTask extends AsyncTask<Void, Void, DownloadStatus> {

        String url;
        String key;

        QueryStatusTask(String key, String url) {
            this.key = key;
            this.url = url;
        }

        @Override
        protected DownloadStatus doInBackground(Void... params) {
            DownloadStatus downloadStatus = mDownloadStatus.get(key);

            if (downloadStatus == null) {
                DownloadManager.Query query = new DownloadManager.Query();
                DownloadManager downloadManager = DownloadManager.getInstance();
                if (downloadManager != null) {
                    Cursor c = downloadManager.query(query);
                    if (c != null) {
                        try {
                            if (c.moveToFirst()) {
                                do {
                                    String uri = c.getString(c.getColumnIndex(DownloadManager.COLUMN_URI));
                                    if (!TextUtils.isEmpty(uri) && uri.equals(url)) {
                                        downloadStatus = DownloadStatusThread.getStatus(c);

                                        break;
                                    }
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
            }

            return downloadStatus;
        }

        @Override
        protected void onPostExecute(DownloadStatus downloadStatus) {
            super.onPostExecute(downloadStatus);

            refreshDownloasStatus(key, downloadStatus);
        }

    }

    static class DownloadStatusThread extends Thread {

        private boolean destory = false;

        private Handler mHandler = new Handler(Looper.getMainLooper()) {

            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                String key = msg.getData().getString("key");
                DownloadStatus status = (DownloadStatus) msg.getData().getSerializable("status");

                refreshDownloasStatus(key, status);
            }

        };

        static DownloadStatus getStatus(Cursor c) {
            DownloadStatus downloadStatus = new DownloadStatus();

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
                                    // 获取最新的状态
                                    DownloadStatus downloadStatus = getStatus(c);

                                    String uri = c.getString(c.getColumnIndex(DownloadManager.COLUMN_URI));
                                    String key = uriKeyMap.get(uri);

                                    // 刷新UI
                                    Message message = mHandler.obtainMessage();
                                    message.getData().putString("key", key);
                                    message.getData().putSerializable("status", downloadStatus);
                                    message.sendToTarget();

                                    if (!mDownloadStatus.containsKey(key)) {
                                        mDownloadStatus.put(key, downloadStatus);
                                    }
                                    break;
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
                        Thread.sleep(50);
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

        public static final int STATUS_INIT = 1 << 5;

        public int status = STATUS_INIT;

        public int error = -1;

        public long total = -1;

        public long progress = -1;

    }

    public interface DownloadProxy {

        void onNewStatus(DownloadStatus status);

        void onInit();

        void onProgress(long progress, long total);

        void onPaused();

        void onPending();

        void onSuccessful();

        void onFailed(int error);

        String generateKey(String uri);

    }

}
