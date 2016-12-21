package org.aiwen.downloader;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.aiwen.downloader.utils.Constants;
import org.aiwen.downloader.utils.Utils;

import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 维护下载的服务
 *
 * Created by wangdan on 16/11/18.
 */
public class DownloadService extends Service {

    private static final String TAG = Constants.TAG + "_Service";

    public static final String ACTION_REQUEST = "org.aisen.downloader.ACTION_REQUEST";

    public static void request(Context context) {
        Intent service = new Intent(context, DownloadService.class);
        service.setAction(ACTION_REQUEST);

        context.startService(service);
    }

    static final int MSG_UPDATE = 1000;
    static final int MSG_NOTIFY = 1001;

    volatile int lastStartId;
    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private ThreadPoolExecutor mExecutor;
    private AtomicBoolean mResourceDestoryed;

    @Override
    public void onCreate() {
        super.onCreate();

        Configuration config = Hawk.getInstance().getConfiguration();

        mExecutor = new ThreadPoolExecutor(config.getMaxConcurrentDownloadsAllowed(),
                                           config.getMaxConcurrentDownloadsAllowed(),
                                           10L,
                                           TimeUnit.SECONDS,
                                           new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {

                                                private final AtomicInteger mCount = new AtomicInteger(1);

                                                @Override
                                                public Thread newThread(Runnable runnable) {
                                                    return new Thread(runnable, "DownloadThread #" + mCount.getAndIncrement());
                                                }

                                            });
        mExecutor.allowCoreThreadTimeOut(true);
        mHandlerThread = new HandlerThread("DownloadManager HandlerThread", Thread.MIN_PRIORITY);
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper(), mCallback);

        enqueueNotify(true);
        mResourceDestoryed = new AtomicBoolean(false);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        do {
            lastStartId = startId;

            if (intent == null || TextUtils.isEmpty(intent.getAction())) {
                break;
            }

            if (ACTION_REQUEST.equals(intent.getAction())) {
                enqueueUpdate(startId);
            }

        } while (false);

        return super.onStartCommand(intent, flags, startId);
    }

    public void enqueueUpdate(int startId) {
        if (mHandler != null) {
            mHandler.removeMessages(MSG_UPDATE);
            mHandler.obtainMessage(MSG_UPDATE, startId, -1).sendToTarget();
        }
    }

    public void enqueueNotify(boolean delay) {
        if (mHandler != null) {
            mHandler.removeMessages(MSG_NOTIFY);
            if (delay) {
                mHandler.sendEmptyMessageDelayed(MSG_NOTIFY, 500);
            }
            else {
                mHandler.obtainMessage(MSG_NOTIFY).sendToTarget();
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    Handler.Callback mCallback = new Handler.Callback() {

        @Override
        public boolean handleMessage(Message message) {
            int what = message.what;

            switch (what) {
                case MSG_NOTIFY:
                    handleNotify();
                    break;
                case MSG_UPDATE:
                    handleUpdate(message.arg1);
                    break;
            }

            return true;
        }

    };

    private void handleNotify() {
        // 处理是否还有下载
        Hawk hawk = Hawk.getInstance();
        if (hawk == null) {
            return;
        }

        Set<String> keys = hawk.mRequestMap.keySet();

        for (String key : keys) {
            Request request = hawk.mRequestMap.get(key);

            if (Downloads.Status.isStatusRunning(request.downloadInfo.status)) {
                if (request.trace != null) {
                    synchronized (request.trace) {
                        request.trace.endSpeedCount();
                        DLogger.v(Utils.getDownloaderTAG(request), "下载速度(%d), 平均速度(%d)", (int) request.trace.getSpeed(), (int) request.trace.getAverageSpeed());
                        request.trace.beginSpeedCount();
                    }
                }
            }
            else if (Downloads.Status.isStatusSuccess(request.downloadInfo.status)) {
                if (request.trace != null) {
                    DLogger.v(Utils.getDownloaderTAG(request), "下载结束，下载耗时 %d ms， 总耗时 %d ms，平均速度 %d kb/s", request.trace.getTime(), request.trace.getRealTime(), (int) request.trace.getAverageSpeed());
                }
            }
        }

        enqueueNotify(true);
    }

    private void handleUpdate(int startId) {
        boolean isActive = false;

        do {
            Hawk hawk = Hawk.getInstance();
            if (hawk == null) {
                break;
            }

            // 处理是否还有下载
            synchronized (hawk.mRequestMap) {
                Set<String> keys = hawk.mRequestMap.keySet();

                for (String key : keys) {
                    Request request = hawk.mRequestMap.get(key);

                    // 新建下载
                    if (request.downloadInfo.status == -1) {
                        isActive = true;

                        mExecutor.execute(new DownloadThread(startId, request, hawk.createDownloader(request), DownloadService.this));
                    }
                }
            }
        } while (false);

        if (!isActive && startId != -1) {
            stopIfNeed(startId);
        }

        DLogger.v(TAG, "isActive = " + isActive + ", startId = " + startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        DLogger.w(TAG, "DownloadService onDestory");

        if (!mResourceDestoryed.get()) {
            destoryResource();
        }
    }

    void stopIfNeed(int startId) {
        if (lastStartId == startId) {
            if (stopSelfResult(startId)) {
                enqueueNotify(false);

                destoryResource();

                DLogger.w(TAG, "stopSelfResult(%d)", startId);
            }
        }
    }

    private void destoryResource() {
        try {
            DLogger.w(TAG, "DestoryResource");

            mResourceDestoryed.set(true);

            mExecutor.shutdownNow();
            mHandlerThread.quit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
