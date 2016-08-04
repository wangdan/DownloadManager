package org.aisen.download;

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
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

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by wangdan on 16/8/2.
 */
public class DownloadService extends Service implements IDownloadSubject {

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
    private ThreadPoolExecutor mExecutor;
    private CoreThread mCoreThread;

    private final Map<String, DownloadInfo> mDownloads = Maps.newHashMap();

    private Handler mHandle = new Handler(Looper.getMainLooper()) {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (msg.what == 0) {
                mNotifier.updateWith(mDownloads.values());
            }
        }

    };

    @Override
    public void onCreate() {
        super.onCreate();

        mDbHelper = new DBHelper(this);
        mNotifier = new DownloadNotifier(this);
        mNotifier.cancelAll();
        mSystemFacade = new RealSystemFacade(this);
        int maxThread = DownloadManager.DEFAULT_MAX_ALLOWED;
        if (DownloadManager.getInstance() != null) {
            maxThread = DownloadManager.getInstance().getMaxAllowed();
        }
        mExecutor = Utils.buildDownloadExecutor(maxThread);

        if (DownloadManager.getInstance() != null) {
            DownloadManager.getInstance().getController().register(this);
        }
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

    @Override
    public void attach(IDownloadObserver observer) {

    }

    @Override
    public void detach(IDownloadObserver observer) {

    }

    @Override
    public void publish(DownloadMsg downloadMsg) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            mNotifier.updateWith(mDownloads.values());
        }
        else {
            mHandle.sendEmptyMessage(0);
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

            while(true) {
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
//                    if (action instanceof DownloadManager.QueryAction) {
//                        DownloadManager.QueryAction queryAction = (DownloadManager.QueryAction) action;
//
//                        if (queryAction.publish && DownloadManager.getInstance() != null) {
//                            DownloadManager.getInstance().getController().publishDownload(new DownloadMsg(key));
//                        }
//                    }

                    DownloadManager.getInstance().getController().publishDownload(new DownloadMsg(action.key()));

                    return;
                }

                // 下载成功
                if (info.mStatus == Downloads.Impl.STATUS_SUCCESS) {
                    Uri uri = Uri.parse(info.mFilePath);
                    if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
                        File file = new File(uri.getPath());
                        if (!file.exists()) {
                            mDbHelper.remove(key);

                            mDownloads.remove(key);

                            continue;
                        }
                    }
                }

                // 文件已存在


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
                        if (info.isActive()) {
                            info.networkShutdown();
                        }

                        info.mControl = Downloads.Impl.CONTROL_PAUSED;
                        info.mStatus = Downloads.Impl.STATUS_PAUSED_BY_APP;
                    }
                    // 继续下载
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
                    // 删除下载
                    else if (action instanceof DownloadManager.RemoveAction) {
                        mDownloads.remove(key);
                        mDbHelper.remove(key);

                        // 删除临时文件
                        try {
                            File temp = info.getTempFile();
                            if (temp.exists()) {
                                temp.delete();
                            }
                        } catch (IOException ignore) {
                        }

                        if (info.mStatus == Downloads.Impl.STATUS_SUCCESS) {
                            Uri uri = Uri.parse(info.mFilePath);
                            if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
                                File file = new File(uri.getPath());
                                if (file.exists()) {
                                    file.delete();
                                }
                            }
                        }
                        else if (info.isActive()) {
                            info.networkShutdown();
                        }

                        continue;
                    }

                    if (runThread) {
                        info.startDownloadIfReady(mExecutor);
                    }
                }

                if (DownloadManager.getInstance() != null) {
                    DownloadManager.getInstance().getController().publishDownload(info);
                }

                break;
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

        if (DownloadManager.getInstance() != null) {
            DownloadManager.getInstance().getController().unregister(this);
        }
    }

}
