package org.aisen.download;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.google.common.collect.Maps;

import org.aisen.download.core.DBHelper;
import org.aisen.download.core.DownloadInfo;
import org.aisen.download.core.Downloads;
import org.aisen.download.core.RealSystemFacade;
import org.aisen.download.ui.DownloadNotifier;
import org.aisen.download.utils.Constants;
import org.aisen.download.utils.DLogger;
import org.aisen.download.utils.Utils;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by wangdan on 16/8/2.
 */
public class DownloadService extends Service {

    private static final String TAG = Constants.TAG + "_DownloadService";

    private static LinkedBlockingQueue<DownloadManager.Action> mRequestQueue = new LinkedBlockingQueue<>();

    final static void runAction(Context context, DownloadManager.Action action) {
        context.startService(new Intent(context, DownloadService.class));

        synchronized (mRequestQueue) {
            mRequestQueue.add(action);
        }
    }

    private final Object mLock = new Object();

    private DBHelper mDbHelper;
    private DownloadNotifier mNotifier;
    private RealSystemFacade mSystemFacade;
    private ExecutorService mExecutor;
    private CoreThread mCoreThread;

    private final Map<String, DownloadInfo> mDownloads = Maps.newHashMap();

    @Override
    public void onCreate() {
        super.onCreate();

        mDbHelper = new DBHelper(this);
        mNotifier = new DownloadNotifier(this);
        mSystemFacade = new RealSystemFacade(this);
        mExecutor = Utils.buildDownloadExecutor(5);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int returnValue = super.onStartCommand(intent, flags, startId);

        synchronized (mLock) {
            if (mCoreThread == null || !mCoreThread.isAlive()) {
                if (mCoreThread != null) {
                    mCoreThread.running = false;
                }
                mCoreThread = new CoreThread();
                mCoreThread.start();
            }
        }

        return returnValue;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
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
                    DownloadManager.Action action = mRequestQueue.poll(10, TimeUnit.SECONDS);

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
                    DownloadInfo info = reader.newDownloadInfo(DownloadService.this, mSystemFacade, mNotifier, mDbHelper);

                    return info;
                }
            } finally {
                try {
                    cursor.close();
                } catch (Exception e) {
                    DLogger.printExc(DownloadService.class, e);
                }
            }

            return null;
        }

        private void runAction(DownloadManager.Action action) {
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
                // 查询状态
                if (action instanceof DownloadManager.QueryAction) {
                    DownloadManager.QueryAction queryAction = (DownloadManager.QueryAction) action;

                    if (queryAction.publish && DownloadManager.getInstance() != null) {
                        DownloadManager.getInstance().getController().publishDownload(new DownloadMsg(key));
                    }
                }

                return;
            }

            if (!mDownloads.containsKey(key) || mDownloads.get(key) != info) {
                mDownloads.put(key, info);
            }

            synchronized (info) {
                boolean runThread = false;

                // 下载请求
                if (action instanceof DownloadManager.EnqueueAction) {
                    runThread = true;
                }
                // 暂停请求
                else if (action instanceof DownloadManager.PauseAction) {
                    info.mControl = Downloads.Impl.CONTROL_PAUSED;

//                    if (!info.isActive()) {
                        info.mStatus = Downloads.Impl.STATUS_PAUSED_BY_APP;
//                    }

                    runThread = false;
                }
                // 开始下载
                else if (action instanceof DownloadManager.ResumeAction) {
                    info.mControl = Downloads.Impl.CONTROL_RUN;

                    runThread = true;
                }
                // 查询状态
                else if (action instanceof DownloadManager.QueryAction) {
                    DownloadManager.QueryAction queryAction = (DownloadManager.QueryAction) action;

                    if (queryAction.publish && DownloadManager.getInstance() != null) {
                        DownloadManager.getInstance().getController().publishDownload(info);
                    }
                }

                if (runThread) {
                    info.startDownloadIfReady(mExecutor);
                }
            }

            if (DownloadManager.getInstance() != null) {
                DownloadManager.getInstance().getController().publishDownload(info);
            }
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mCoreThread != null) {
            mCoreThread.running = false;

            mCoreThread = null;
        }

        mRequestQueue.clear();
    }

}
