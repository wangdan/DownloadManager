package org.aisen.download.manager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.google.common.collect.Maps;

import org.aisen.download.core.DownloadThread;
import org.aisen.download.core.SystemFacade;
import org.aisen.download.db.DBHelper;
import org.aisen.download.db.DownloadInfo;
import org.aisen.download.manager.DownloadManager.Action;
import org.aisen.download.ui.DownloadNotifier;
import org.aisen.download.utils.Constants;
import org.aisen.download.utils.DLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by wangdan on 16/7/30.
 */
class ThreadManager {

    private static final String TAG = Constants.TAG + "_ThreadManager";

    private Context mContext;
    private DBHelper mDbHelper;
    private SystemFacade mSystemFacade;
    private DownloadNotifier mNotifier;

    private ExecutorService mExecutor;

    private CoreThread mCoreThread;
    private LinkedBlockingQueue<Action> mRequestQueue = new LinkedBlockingQueue<>();

    private final Map<String, DownloadThread> mThreadTable;

//    private final Map<String, Action> lastAction;

    private final Map<String, DownloadInfo> mDownloads = Maps.newHashMap();

    ThreadManager(Context context, DBHelper dbHelper, SystemFacade systemFacade,
                        DownloadNotifier notifier, int maxThread) {
        mContext = context;
        mDbHelper = dbHelper;
        mSystemFacade = systemFacade;
        mNotifier = notifier;
        mThreadTable = new HashMap<>();
        mExecutor = buildDownloadExecutor(maxThread);
//        lastAction = new HashMap<>();
    }

    private ExecutorService buildDownloadExecutor(int maxThread) {
        final int maxConcurrent = maxThread;

        // Create a bounded thread pool for executing downloads; it creates
        // threads as needed (up to maximum) and reclaims them when finished.
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(
                maxConcurrent, maxConcurrent, 10, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>()) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);

                if (t == null && r instanceof Future<?>) {
                    try {
                        ((Future<?>) r).get();
                    } catch (CancellationException ce) {
                        t = ce;
                    } catch (ExecutionException ee) {
                        t = ee.getCause();
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }

                if (t != null) {
                    DLogger.w(TAG, "Uncaught exception", t);
                }
            }
        };
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    public void runAction(Action action) {
//        synchronized (lastAction) {
//            lastAction.put(action.key(), action);
//        }

        synchronized (mRequestQueue) {
            if (mCoreThread == null || !mCoreThread.isAlive()) {
                if (mCoreThread != null) {
                    mCoreThread.running = false;
                }
                mCoreThread = new CoreThread();
                mCoreThread.start();
            }

            mRequestQueue.add(action);
        }
    }

    class CoreThread extends Thread {

        CoreThread() {
            DLogger.v(TAG, "New CoreThread");
        }

        boolean running = true;

        @Override
        public void run() {
            while (running) {
                try {
                    DLogger.v(TAG, "等待处理Request");
                    Action action = mRequestQueue.poll(30, TimeUnit.SECONDS);

                    if (action != null) {
                        synchronized (mDownloads) {
                            runAction(action);
                        }
                    }
                    else {
                        running = false;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();

                    DLogger.w(TAG, e);

                    running = false;
                }
            }
        }

        private DownloadInfo readDB(String key) {
            final Cursor cursor = mDbHelper.query(key);
            try {
                // 已存在数据
                if (cursor.moveToFirst()) {
                    final DownloadInfo.Reader reader = new DownloadInfo.Reader(cursor);
                    DownloadInfo info = reader.newDownloadInfo(mContext, mSystemFacade, mNotifier);

                    return info;
                }
            } finally {
                try {
                    cursor.close();
                } catch (Exception e) {
                    DLogger.printExc(ThreadManager.class, e);
                }
            }

            return null;
        }

        private void runAction(Action action) {
            String key = action.key();

            DownloadInfo info = mDownloads.get(key);

            if (info == null) {
                info = readDB(key);
            }

            if (info == null && action instanceof DownloadManager.EnqueueAction) {
                Request request = ((DownloadManager.EnqueueAction) action).request;
                ContentValues contentValues = request.toContentValues();

                if (mDbHelper.insert(contentValues) == -1l) {

                    DLogger.w(TAG, "DownloadInfo 存库失败");

                    return;
                }
                else {
                    info = readDB(key);
                }
            }

            if (info == null) {
                DLogger.w(TAG, "处理Action失败");

                return;
            }

            DownloadThread thread;
            if (action instanceof DownloadManager.EnqueueAction) {
                DownloadManager.EnqueueAction enqueueAction = (DownloadManager.EnqueueAction) action;

                if (mThreadTable.containsKey(info.mKey)) {
                    thread = mThreadTable.get(info.mKey);
                }
                else {
                    thread = new DownloadThread(mContext, mDbHelper, mSystemFacade, mNotifier, info);
                    Future<?> mSubmittedTask = mExecutor.submit(thread);
                }

                DLogger.v(TAG, "Request[%s]", enqueueAction.request.toString());
            }
            else {
                thread = null;
            }

            if (thread != null) {
                thread.notifyDownloadInfo();
            }
        }

    }

}
