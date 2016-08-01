package org.aisen.download.db;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.text.TextUtils;

import org.aisen.download.core.Helpers;
import org.aisen.download.core.SystemFacade;
import org.aisen.download.downloads.Downloads;
import org.aisen.download.manager.Request;
import org.aisen.download.ui.DownloadNotifier;
import org.aisen.download.ui.SizeLimitActivity;
import org.aisen.download.utils.ConnectivityManagerUtils;
import org.aisen.download.utils.Constants;
import org.aisen.download.utils.Utils;

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

    public long mId;
    public String mKey;// 组合主键
    public String mUri;// 下载地址
    public String mFilePath;// 最终保存文件地址
    public String mMimeType;
//    public int mDestination;
    public int mVisibility;
    public int mControl;
    public int mStatus;
    public String mErrorMsg;
    public int mNumFailed;
    public int mRetryAfter;
    public long mTotalBytes;
    public long mCurrentBytes;
//    public boolean mDeleted;
    public int mAllowedNetworkTypes;
    public boolean mAllowRoaming;
    public int mBypassRecommendedSizeLimit;
    public String mUserAgent;
    public String mETag;
    public String mTitle;
    public String mDescription;

    private final Context mContext;
    private final SystemFacade mSystemFacade;
    private final DownloadNotifier mNotifier;

    public int mFuzz;

    private DownloadInfo(Context context, SystemFacade systemFacade, DownloadNotifier notifier) {
        mContext = context;
        mSystemFacade = systemFacade;
        mNotifier = notifier;
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

        public DownloadInfo newDownloadInfo(Context context, SystemFacade systemFacade, DownloadNotifier notifier) {
            final DownloadInfo info = new DownloadInfo(context, systemFacade, notifier);

            updateFromDatabase(info);

            return info;
        }

        public void updateFromDatabase(DownloadInfo info) {
            info.mId = getLong(Downloads.Impl._ID);
            info.mUri = getString(Downloads.Impl.COLUMN_URI);
            info.mFilePath = getString(Downloads.Impl._DATA);
            info.mMimeType = Utils.normalizeMimeType(getString(Downloads.Impl.COLUMN_MIME_TYPE));
            info.mVisibility = getInt(Downloads.Impl.COLUMN_VISIBILITY);
            info.mStatus = getInt(Downloads.Impl.COLUMN_STATUS);
            // 新增错误原因
            info.mErrorMsg = getString(Downloads.Impl.COLUMN_ERROR_MSG);
            info.mNumFailed = getInt(Downloads.Impl.COLUMN_FAILED_CONNECTIONS);
            int retryRedirect = getInt(Constants.RETRY_AFTER_X_REDIRECT_COUNT);
            info.mRetryAfter = retryRedirect & 0xfffffff;
            info.mUserAgent = getString(Downloads.Impl.COLUMN_USER_AGENT);
            info.mTotalBytes = getLong(Downloads.Impl.COLUMN_TOTAL_BYTES);
            info.mCurrentBytes = getLong(Downloads.Impl.COLUMN_CURRENT_BYTES);
            info.mETag = getString(Constants.ETAG);
            info.mAllowedNetworkTypes = getInt(Downloads.Impl.COLUMN_ALLOWED_NETWORK_TYPES);
            info.mAllowRoaming = getInt(Downloads.Impl.COLUMN_ALLOW_ROAMING) != 0;
            info.mTitle = getString(Downloads.Impl.COLUMN_TITLE);
            info.mDescription = getString(Downloads.Impl.COLUMN_DESCRIPTION);
            info.mBypassRecommendedSizeLimit =
                    getInt(Downloads.Impl.COLUMN_BYPASS_RECOMMENDED_SIZE_LIMIT);

            synchronized (this) {
                info.mControl = getInt(Downloads.Impl.COLUMN_CONTROL);
            }
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

}
