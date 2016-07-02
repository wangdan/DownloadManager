/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.aisen.downloader.provider;

import android.app.AlarmManager;
import android.app.DownloadManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.text.TextUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.aisen.downloader.DLogger;
import org.aisen.downloader.R;
import org.aisen.downloader.DownloadController;
import org.aisen.downloader.downloads.Downloads;

import org.aisen.downloader.utils.IndentingPrintWriter;

import java.io.File;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static android.text.format.DateUtils.MINUTE_IN_MILLIS;

/**
 * Performs background downloads as requested by applications that use
 * {@link DownloadManager}. Multiple start commands can be issued at this
 * service, and it will continue running until no downloads are being actively
 * processed. It may schedule alarms to resume downloads in future.
 * <p>
 * Any database updates important enough to initiate tasks should always be
 * delivered through {@link Context#startService(Intent)}.
 */
public class DownloadService extends Service {

    public static final String TAG = Constants.TAG + "_DownloadService";

    // TODO: migrate WakeLock from individual DownloadThreads out into
    // DownloadReceiver to protect our entire workflow.

    private static final boolean DEBUG_LIFECYCLE = false;

    @VisibleForTesting
    SystemFacade mSystemFacade;

    private AlarmManager mAlarmManager;

    /** Observer to get notified when the content observer's data changes */
    private DownloadManagerContentObserver mObserver;

    /** Class to handle Notification Manager updates */
    private DownloadNotifier mNotifier;

    /**
     * The Service's view of the list of downloads, mapping download IDs to the corresponding info
     * object. This is kept independently from the content provider, and the Service only initiates
     * downloads based on this data, so that it can deal with situation where the data in the
     * content provider changes or disappears.
     */
    @GuardedBy("mDownloads")
    private final Map<Long, DownloadInfo> mDownloads = Maps.newHashMap();
    private final Map<Long, Boolean> mDownloadCompleted = Maps.newHashMap();// 保存已完成的状态

    private ExecutorService mExecutor;

    private ExecutorService buildDownloadExecutor(Context context) {
        final int maxConcurrent = context.getResources().getInteger(R.integer.config_MaxConcurrentDownloadsAllowed);

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

//    private DownloadScanner mScanner;

    private HandlerThread mUpdateThread;
    private Handler mUpdateHandler;

    private volatile int mLastStartId;

    /**
     * Receives notifications when the data in the content provider changes
     */
    private class DownloadManagerContentObserver extends ContentObserver {
        public DownloadManagerContentObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(final boolean selfChange) {
            enqueueUpdate();
        }
    }

    /**
     * Returns an IBinder instance when someone wants to connect to this
     * service. Binding to this service is not allowed.
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public IBinder onBind(Intent i) {
        throw new UnsupportedOperationException("Cannot bind to Download Manager Service");
    }

    /**
     * Initializes the service when it is first created
     */
    @Override
    public void onCreate() {
        super.onCreate();

        mExecutor = buildDownloadExecutor(this);

        DLogger.v(TAG, "Service onCreate");

        if (mSystemFacade == null) {
            mSystemFacade = new RealSystemFacade(this);
        }

        mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        mUpdateThread = new HandlerThread(TAG + "-UpdateThread");
        mUpdateThread.start();
        mUpdateHandler = new Handler(mUpdateThread.getLooper(), mUpdateCallback);

//        mScanner = new DownloadScanner(this);

        mNotifier = new DownloadNotifier(this);
        mNotifier.cancelAll();

        mObserver = new DownloadManagerContentObserver();
        getContentResolver().registerContentObserver(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI,
                true, mObserver);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int returnValue = super.onStartCommand(intent, flags, startId);
        DLogger.v(TAG, "Service onStart");
        mLastStartId = startId;
        enqueueUpdate();
        return returnValue;
    }

    @Override
    public void onDestroy() {
        getContentResolver().unregisterContentObserver(mObserver);
//        mScanner.shutdown();
        mUpdateThread.quit();
        DLogger.v(TAG, "Service onDestroy");
        super.onDestroy();
    }

    /**
     * Enqueue an {@link #updateLocked()} pass to occur in future.
     */
    public void enqueueUpdate() {
        if (mUpdateHandler != null) {
            mUpdateHandler.removeMessages(MSG_UPDATE);
            mUpdateHandler.obtainMessage(MSG_UPDATE, mLastStartId, -1).sendToTarget();
        }
    }

    /**
     * Enqueue an {@link #updateLocked()} pass to occur after delay, usually to
     * catch any finished operations that didn't trigger an update pass.
     */
    private void enqueueFinalUpdate() {
        mUpdateHandler.removeMessages(MSG_FINAL_UPDATE);
        mUpdateHandler.sendMessageDelayed(
                mUpdateHandler.obtainMessage(MSG_FINAL_UPDATE, mLastStartId, -1),
                5 * MINUTE_IN_MILLIS);
    }

    private static final int MSG_UPDATE = 1;
    private static final int MSG_FINAL_UPDATE = 2;

    private Handler.Callback mUpdateCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

            final int startId = msg.arg1;
            if (DEBUG_LIFECYCLE) DLogger.v(TAG, "Updating for startId " + startId);

            // Since database is current source of truth, our "active" status
            // depends on database state. We always get one final update pass
            // once the real actions have finished and persisted their state.

            // TODO: switch to asking real tasks to derive active state
            // TODO: handle media scanner timeouts

            final boolean isActive;
            synchronized (mDownloads) {
                isActive = updateLocked();
            }

            if (msg.what == MSG_FINAL_UPDATE) {
                // Dump thread stacks belonging to pool
                for (Map.Entry<Thread, StackTraceElement[]> entry :
                        Thread.getAllStackTraces().entrySet()) {
                    if (entry.getKey().getName().startsWith("pool")) {
                        DLogger.d(TAG, entry.getKey() + ": " + Arrays.toString(entry.getValue()));
                    }
                }

                // Dump speed and update details
                mNotifier.dumpSpeeds();

                DLogger.w(TAG, "Final update pass triggered, isActive=" + isActive
                        + "; someone didn't update correctly.");
            }

            if (isActive) {
                // Still doing useful work, keep service alive. These active
                // tasks will trigger another update pass when they're finished.

                // Enqueue delayed update pass to catch finished operations that
                // didn't trigger an update pass; these are bugs.
                enqueueFinalUpdate();

            } else {
                // No active tasks, and any pending update messages can be
                // ignored, since any updates important enough to initiate tasks
                // will always be delivered with a new startId.

                if (stopSelfResult(startId)) {
                    if (DEBUG_LIFECYCLE) DLogger.v(TAG, "Nothing left; stopped");
                    getContentResolver().unregisterContentObserver(mObserver);
//                    mScanner.shutdown();
                    mUpdateThread.quit();
                }
            }

            return true;
        }
    };

    /**
     * Update {@link #mDownloads} to match {@link DownloadProvider} state.
     * Depending on current download state it may enqueue {@link DownloadThread}
     * instances, request {@link DownloadScanner} scans, update user-visible
     * notifications, and/or schedule future actions with {@link AlarmManager}.
     * <p>
     * Should only be called from {@link #mUpdateThread} as after being
     * requested through {@link #enqueueUpdate()}.
     *
     * @return If there are active tasks being processed, as of the database
     *         snapshot taken in this update.
     */
    private boolean updateLocked() {
        final long now = mSystemFacade.currentTimeMillis();

        boolean isActive = false;
        long nextActionMillis = Long.MAX_VALUE;

        final Set<Long> staleIds = Sets.newHashSet(mDownloads.keySet());

        final ContentResolver resolver = getContentResolver();
        final Cursor cursor = resolver.query(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI,
                null, null, null, null);
        try {
            final DownloadInfo.Reader reader = new DownloadInfo.Reader(resolver, cursor);
            final int idColumn = cursor.getColumnIndexOrThrow(Downloads.Impl._ID);
            if (cursor.getCount() == 0) {
                DLogger.v(TAG, "cursor.getCount() = 0");
            }
            while (cursor.moveToNext()) {
                final long id = cursor.getLong(idColumn);
                staleIds.remove(id);

                DownloadInfo info = mDownloads.get(id);
                if (info != null) {
                    updateDownload(reader, info, now);

                    DLogger.v(TAG, "updateDownload, app[%s], status[%d]", info.mTitle, info.mStatus);
                } else {
                    info = insertDownloadLocked(reader, now);

                    DLogger.v(TAG, "insertDownloadLocked, app[%s], status[%d]", info.mTitle, info.mStatus);
                }

                String localUri = cursor.getString(cursor.getColumnIndexOrThrow(Downloads.Impl._DATA));
                // 下载异常，且临时文件被删掉了
                if (TextUtils.isEmpty(localUri) && Downloads.Impl.isStatusError(info.mStatus)) {
                    info.mDeleted = true;
                }
                // 如果文件被删除
                else if (!TextUtils.isEmpty(localUri) && !new File(localUri).exists()) {
                    info.mDeleted = true;
                }

                if (info.mDeleted) {
                    DLogger.w(TAG, "remove id[%s], name[%s], localUri[%s]", id + "", info.mTitle, info.mFileName);

                    staleIds.add(id);
                    info.mStatus = -1;

                    DownloadController.refreshDownloadInfo(info);

                    // Delete download if requested, but only after cleaning up
                    if (!TextUtils.isEmpty(info.mMediaProviderUri)) {
                        resolver.delete(Uri.parse(info.mMediaProviderUri), null, null);
                    }

                    deleteFileIfExists(info.mFileName);
                    resolver.delete(info.getAllDownloadsUri(), null, null);
                } else {
                    // Kick off download task if ready
                    final boolean activeDownload = info.startDownloadIfReady(mExecutor);

                    // Kick off media scan if completed
                    final boolean activeScan = false; // info.startScanIfReady(mScanner);

                    if (DEBUG_LIFECYCLE && (activeDownload || activeScan)) {
                        DLogger.v(TAG, "Download " + info.mId + ": activeDownload=" + activeDownload
                                + ", activeScan=" + activeScan);
                    }

                    isActive |= activeDownload;
                    isActive |= activeScan;
                    isActive |= info.mControl == Downloads.Impl.CONTROL_PAUSED;
                }

                // Keep track of nearest next action
                nextActionMillis = Math.min(info.nextActionMillis(now), nextActionMillis);
            }
        } finally {
            cursor.close();
        }

        // Update notifications visible to user
        Collection<DownloadInfo> downloadInfos = mDownloads.values();
        // 更新状态给UI
        for (DownloadInfo downloadInfo : downloadInfos) {
            if (isNotifyStatus(downloadInfo.mId, downloadInfo.mStatus)) {
                DownloadController.refreshDownloadInfo(downloadInfo);
            }
        }

        // Clean up stale downloads that disappeared
        for (Long id : staleIds) {
            deleteDownloadLocked(id);
        }

        // Update notifications visible to user
        downloadInfos = mDownloads.values();
        mNotifier.updateWith(downloadInfos);

        // Set alarm when next action is in future. It's okay if the service
        // continues to run in meantime, since it will kick off an update pass.
        if (nextActionMillis > 0 && nextActionMillis < Long.MAX_VALUE) {
            DLogger.v(TAG, "scheduling start in " + nextActionMillis + "ms");

            final Intent intent = new Intent(Constants.ACTION_RETRY);
            intent.setClass(this, DownloadReceiver.class);
            mAlarmManager.set(AlarmManager.RTC_WAKEUP, now + nextActionMillis,
                    PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_ONE_SHOT));
        }

        return isActive;
    }

    /**
     * 是否刷新UI
     *
     * @param id
     * @param status
     * @return
     */
    private boolean isNotifyStatus(long id, int status) {
        DLogger.v(TAG, "isNotifyStatus(%s, %d)", id + "", status);
        // 等待下载
        if (status == Downloads.Impl.STATUS_QUEUED_FOR_WIFI ||
                status == Downloads.Impl.STATUS_RUNNING) {
            mDownloadCompleted.put(id, false);
            return true;
        }
        // 下载出错
        else if (Downloads.Impl.isStatusError(status)) {
            mDownloadCompleted.put(id, false);
            return true;
        }
        // 下载暂停
        else if (Downloads.Impl.isStatusPaused(status)) {
            mDownloadCompleted.put(id, false);
            return true;
        }
        // 已经下载完成
        else if (Downloads.Impl.isStatusCompleted(status) && (mDownloadCompleted.get(id) == null || !mDownloadCompleted.get(id))) {
            mDownloadCompleted.put(id, true);
            return true;
        }

        return false;
    }

    /**
     * Keeps a local copy of the info about a download, and initiates the
     * download if appropriate.
     */
    private DownloadInfo insertDownloadLocked(DownloadInfo.Reader reader, long now) {
        final DownloadInfo info = reader.newDownloadInfo(this, mSystemFacade, mNotifier);
        mDownloads.put(info.mId, info);

        DLogger.v(TAG, "processing inserted download " + info.mId);

        return info;
    }

    /**
     * Updates the local copy of the info about a download.
     */
    private void updateDownload(DownloadInfo.Reader reader, DownloadInfo info, long now) {
        reader.updateFromDatabase(info);
        DLogger.v(TAG, "processing updated download " + info.mId +
                ", status: " + info.mStatus);
    }

    /**
     * Removes the local copy of the info about a download.
     */
    private void deleteDownloadLocked(long id) {
        DownloadInfo info = mDownloads.get(id);
        if (info != null) {
            if (info.mStatus == Downloads.Impl.STATUS_RUNNING) {
                info.mStatus = Downloads.Impl.STATUS_CANCELED;
            }
            if (info.mDestination != Downloads.Impl.DESTINATION_EXTERNAL && info.mFileName != null) {
                DLogger.d(TAG, "deleteDownloadLocked() deleting " + info.mFileName);
                deleteFileIfExists(info.mFileName);
            }
            mDownloads.remove(info.mId);
            mDownloadCompleted.remove(info.mId);
        }
    }

    private void deleteFileIfExists(String path) {
        if (!TextUtils.isEmpty(path)) {
            DLogger.d(TAG, "deleteFileIfExists() deleting " + path);
            final File file = new File(path);
            if (file.exists() && !file.delete()) {
                DLogger.w(TAG, "file: '" + path + "' couldn't be deleted");
            }
        }
    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        final IndentingPrintWriter pw = new IndentingPrintWriter(writer, "  ");
        synchronized (mDownloads) {
            final List<Long> ids = Lists.newArrayList(mDownloads.keySet());
            Collections.sort(ids);
            for (Long id : ids) {
                final DownloadInfo info = mDownloads.get(id);
                info.dump(pw);
            }
        }
    }
}
