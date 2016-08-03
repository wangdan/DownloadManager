package org.aisen.download.core;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import org.aisen.download.DownloadThread;
import org.aisen.download.core.Downloads.Impl;
import org.aisen.download.Request;
import org.aisen.download.ui.DownloadNotifier;
import org.aisen.download.ui.SizeLimitActivity;
import org.aisen.download.utils.ConnectivityManagerUtils;
import org.aisen.download.utils.Constants;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Created by wangdan on 16/7/30.
 */
public class DownloadInfo {

    /**
     * For intents used to notify the user that a download exceeds a size threshold, if this extra
     * is true, WiFi is required for this download size; otherwise, it is only recommended.
     */
    public static final String EXTRA_IS_WIFI_REQUIRED = "isWifiRequired";

    private boolean mIsPublicApi = true;

    public long mId;// DB ID
    public String mKey;// 组合主键
    public String mUri;// 下载地址
    public String mFilePath;// 最终保存文件地址
    public int mVisibility;// 通知栏
    public int mControl;// 下载、暂停等控制操作
    public int mStatus;// 状态
    public String mErrorMsg;// 失败消息
    public long mLastMod;// 最后修改时间
    public int mNumFailed;// 重连失败次数
    public long mTotalBytes;// 文件大小
    public long mCurrentBytes;// 下载进度
    public int mAllowedNetworkTypes;// 允许网络状态
    public boolean mAllowRoaming;// 是否能漫游下载
    public int mBypassRecommendedSizeLimit;// 移动网络限制
    public String mETag;
    public String mTitle;// 通知栏标题
    public String mDescription;// 通知栏描述

    private final Context mContext;
    private final DBHelper mDbHelper;
    private final SystemFacade mSystemFacade;
    private final DownloadNotifier mNotifier;

    private Future<?> mSubmittedTask;
    private DownloadThread mThread;

    public int mFuzz;

    private DownloadInfo(Context context, SystemFacade systemFacade, DownloadNotifier notifier, DBHelper dbHelper) {
        mContext = context;
        mSystemFacade = systemFacade;
        mNotifier = notifier;
        mDbHelper = dbHelper;
        mFuzz = Helpers.sRandom.nextInt(1001);
    }

    /**
     * Constants used to indicate network state for a specific download, after
     * applying any requested constraints.
     */
    public enum NetworkState {
        /**
         * The network is usable for the given download.
         */
        OK,

        /**
         * There is no network connectivity.
         */
        NO_CONNECTION,

        /**
         * The download exceeds the maximum size for this network.
         */
        UNUSABLE_DUE_TO_SIZE,

        /**
         * The download exceeds the recommended maximum size for this network,
         * the user must confirm for this download to proceed without WiFi.
         */
        RECOMMENDED_UNUSABLE_DUE_TO_SIZE,

        /**
         * The current connection is roaming, and the download can't proceed
         * over a roaming connection.
         */
        CANNOT_USE_ROAMING,

        /**
         * The app requesting the download specific that it can't use the
         * current network connection.
         */
        TYPE_DISALLOWED_BY_REQUESTOR,

        /**
         * Current network is blocked for requesting application.
         */
        BLOCKED;
    }

    /**
     * Returns whether this download is allowed to use the network.
     */
    public NetworkState checkCanUseNetwork(long totalBytes) {
        final NetworkInfo info = mSystemFacade.getActiveNetworkInfo();
        if (info == null || !info.isConnected()) {
            return NetworkState.NO_CONNECTION;
        }
        if (NetworkInfo.DetailedState.BLOCKED.equals(info.getDetailedState())) {
            return NetworkState.BLOCKED;
        }
        if (mSystemFacade.isNetworkRoaming() && !isRoamingAllowed()) {
            return NetworkState.CANNOT_USE_ROAMING;
        }
        return checkIsNetworkTypeAllowed(info.getType(), totalBytes);
    }

    /**
     * 允许漫游
     *
     * @return
     */
    private boolean isRoamingAllowed() {
        return true;
    }

    /**
     * Check if this download can proceed over the given network type.
     * @param networkType a constant from ConnectivityManager.TYPE_*.
     * @return one of the NETWORK_* constants
     */
    private NetworkState checkIsNetworkTypeAllowed(int networkType, long totalBytes) {
        if (mIsPublicApi) {
            final int flag = translateNetworkTypeToApiFlag(networkType);
            final boolean allowAllNetworkTypes = mAllowedNetworkTypes == ~0;
            if (!allowAllNetworkTypes && (flag & mAllowedNetworkTypes) == 0) {
                return NetworkState.TYPE_DISALLOWED_BY_REQUESTOR;
            }
        }
        return checkSizeAllowedForNetwork(networkType, totalBytes);
    }

    /**
     * Translate a ConnectivityManager.TYPE_* constant to the corresponding
     * DownloadManager.Request.NETWORK_* bit flag.
     */
    private int translateNetworkTypeToApiFlag(int networkType) {
        switch (networkType) {
            case ConnectivityManager.TYPE_MOBILE:
                return Request.NETWORK_MOBILE;

            case ConnectivityManager.TYPE_WIFI:
                return Request.NETWORK_WIFI;

            case ConnectivityManager.TYPE_BLUETOOTH:
                return Request.NETWORK_BLUETOOTH;

            default:
                return 0;
        }
    }

    /**
     * Check if the download's size prohibits it from running over the current network.
     * @return one of the NETWORK_* constants
     */
    private NetworkState checkSizeAllowedForNetwork(int networkType, long totalBytes) {
        if (totalBytes <= 0) {
            // we don't know the size yet
            return NetworkState.OK;
        }

        if (ConnectivityManagerUtils.isNetworkTypeMobile(networkType)) {
            Long maxBytesOverMobile = mSystemFacade.getMaxBytesOverMobile();
            if (maxBytesOverMobile != null && totalBytes > maxBytesOverMobile) {
                return NetworkState.UNUSABLE_DUE_TO_SIZE;
            }
            if (mBypassRecommendedSizeLimit == 0) {
                Long recommendedMaxBytesOverMobile = mSystemFacade
                        .getRecommendedMaxBytesOverMobile();
                if (recommendedMaxBytesOverMobile != null
                        && totalBytes > recommendedMaxBytesOverMobile) {
                    return NetworkState.RECOMMENDED_UNUSABLE_DUE_TO_SIZE;
                }
            }
        }

        return NetworkState.OK;
    }

    public void notifyPauseDueToSize(boolean isWifiRequired) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(getAllDownloadsUri());
        intent.setClassName(SizeLimitActivity.class.getPackage().getName(),
                SizeLimitActivity.class.getName());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(EXTRA_IS_WIFI_REQUIRED, isWifiRequired);
        mContext.startActivity(intent);
    }

    public Uri getAllDownloadsUri() {
        return ContentUris.withAppendedId(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, mId);
    }

    public static class Reader {
        private Cursor mCursor;

        public Reader(Cursor cursor) {
            mCursor = cursor;
        }

        public DownloadInfo newDownloadInfo(Context context, SystemFacade systemFacade, DownloadNotifier notifier, DBHelper dbHelper) {
            final DownloadInfo info = new DownloadInfo(context, systemFacade, notifier, dbHelper);

            updateFromDatabase(info);

            return info;
        }

        public void updateFromDatabase(DownloadInfo info) {
            info.mId = getLong(Downloads.Impl._ID);
            info.mKey = getString(Impl.COLUMN_KEY);
            info.mUri = getString(Downloads.Impl.COLUMN_URI);
            info.mFilePath = getString(Downloads.Impl._DATA);
            info.mVisibility = getInt(Downloads.Impl.COLUMN_VISIBILITY);
            synchronized (this) {
                info.mControl = getInt(Downloads.Impl.COLUMN_CONTROL);
            }
            info.mStatus = getInt(Downloads.Impl.COLUMN_STATUS);
            // 新增错误原因
            info.mErrorMsg = getString(Downloads.Impl.COLUMN_ERROR_MSG);
            info.mLastMod = getLong(Downloads.Impl.COLUMN_LAST_MODIFICATION);
            info.mNumFailed = getInt(Downloads.Impl.COLUMN_FAILED_CONNECTIONS);
            info.mTotalBytes = getLong(Downloads.Impl.COLUMN_TOTAL_BYTES);
            info.mCurrentBytes = getLong(Downloads.Impl.COLUMN_CURRENT_BYTES);
            info.mAllowedNetworkTypes = getInt(Downloads.Impl.COLUMN_ALLOWED_NETWORK_TYPES);
            info.mAllowRoaming = getInt(Downloads.Impl.COLUMN_ALLOW_ROAMING) != 0;
            info.mBypassRecommendedSizeLimit =
                    getInt(Downloads.Impl.COLUMN_BYPASS_RECOMMENDED_SIZE_LIMIT);
            info.mETag = getString(Constants.ETAG);
            info.mTitle = getString(Downloads.Impl.COLUMN_TITLE);
            info.mDescription = getString(Downloads.Impl.COLUMN_DESCRIPTION);
        }

        private String getString(String column) {
            int index = mCursor.getColumnIndexOrThrow(column);
            String s = mCursor.getString(index);
            return (TextUtils.isEmpty(s)) ? null : s;
        }

        private Integer getInt(String column) {
            return mCursor.getInt(mCursor.getColumnIndexOrThrow(column));
        }

        private Long getLong(String column) {
            return mCursor.getLong(mCursor.getColumnIndexOrThrow(column));
        }
    }

    /**
     * If download is ready to start, and isn't already pending or executing,
     * create a {@link DownloadThread} and enqueue it into given
     * {@link Executor}.
     *
     * @return If actively downloading.
     */
    public boolean startDownloadIfReady(ExecutorService executor) {
        synchronized (this) {
            final boolean isReady = isReadyToDownload();
            final boolean isActive = isActive();
            if (isReady && !isActive) {
                if (mStatus != Impl.STATUS_RUNNING) {
                    mStatus = Impl.STATUS_RUNNING;
                    ContentValues values = new ContentValues();
                    values.put(Impl.COLUMN_STATUS, mStatus);
                    mDbHelper.update(mKey, values);
                }

                mThread = new DownloadThread(mDbHelper, mSystemFacade, mNotifier, this);
                mSubmittedTask = executor.submit(mThread);
            }
            return isReady;
        }
    }

    public void networkShutdown() {
        if (isActive()) {
            if (mThread != null)
                mThread.shutDown();

            mSubmittedTask = null;
        }
    }

    public boolean isActive() {
        return mSubmittedTask != null && !mSubmittedTask.isDone();
    }

    public void threadFinished() {
        mThread = null;
    }

    /**
     * Returns the time when a download should be restarted.
     */
    public long restartTime(long now) {
        if (mNumFailed == 0) {
            return now;
        }
        return mLastMod +
                Constants.RETRY_FIRST_DELAY *
                        (1000 + mFuzz) * (1 << (mNumFailed - 1));
    }

    /**
     * Returns whether this download should be enqueued.
     */
    private boolean isReadyToDownload() {
        if (mControl == Downloads.Impl.CONTROL_PAUSED) {
            // the download is paused, so it's not going to start
            return false;
        }
        else if (mControl == Impl.CONTROL_RUN && !Downloads.Impl.isStatusSuccess(mStatus)) {
            return true;
        }

        switch (mStatus) {
            case 0: // status hasn't been initialized yet, this is a new download
            case Downloads.Impl.STATUS_PENDING: // download is explicit marked as ready to start
            case Downloads.Impl.STATUS_RUNNING: // download interrupted (process killed etc) while
                // running, without a chance to update the database
                return true;

            case Downloads.Impl.STATUS_WAITING_FOR_NETWORK:
            case Downloads.Impl.STATUS_QUEUED_FOR_WIFI:
                return checkCanUseNetwork(mTotalBytes) == NetworkState.OK;

            case Downloads.Impl.STATUS_WAITING_TO_RETRY:
                // download was waiting for a delayed restart
                final long now = mSystemFacade.currentTimeMillis();
                return restartTime(now) <= now;
            case Downloads.Impl.STATUS_DEVICE_NOT_FOUND_ERROR:
                // is the media mounted?
                final Uri uri = Uri.parse(mUri);
                if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
                    final File file = new File(uri.getPath());
                    return Environment.MEDIA_MOUNTED
                            .equals(Environment.getExternalStorageState(file));
                } else {
                    Log.w("DownloadInfo", "Expected file URI on external storage: " + mUri);
                    return false;
                }
            case Downloads.Impl.STATUS_INSUFFICIENT_SPACE_ERROR:
                // avoids repetition of retrying download
                return false;
        }
        return false;
    }

    public File getTempFile() throws IOException {
        Uri fileUri = Uri.parse(mFilePath + Constants.TEMP_SUFFIX);

        if (ContentResolver.SCHEME_FILE.equals(fileUri.getScheme())) {
            // 先存临时文件
            return new File(fileUri.getPath());
        }
        else {
            throw new IOException("Invalid file : " + fileUri.toString());
        }
    }

}
