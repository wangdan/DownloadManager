package org.aisen.download.utils;

/**
 * Created by wangdan on 16/7/30.
 */
public final class Constants {

    public static final String TAG = "DownloadManager";

    public static final String TEMP_SUFFIX = ".atmp";

    /** The intent that gets sent when the service must wake up for a retry */
    public static final String ACTION_RETRY = "android.intent.action.DOWNLOAD_WAKEUP";

    /**
     * The maximum number of redirects.
     */
    public static final int MAX_REDIRECTS = 5; // can't be more than 7.

    /** The buffer size used to stream the data */
    public static final int BUFFER_SIZE = 8192;

    /** The minimum amount of progress that has to be done before the progress bar gets updated */
    public static final int MIN_PROGRESS_STEP = 65536;

    /** The minimum amount of time that has to elapse before the progress bar gets updated, in ms */
    public static final long MIN_PROGRESS_TIME = 300;// 2000

    /**
     * The time between a failure and the first retry after an IOException.
     * Each subsequent retry grows exponentially, doubling each time.
     * The time is in seconds.
     */
    public static final int RETRY_FIRST_DELAY = 30;

    /**
     * The number of times that the download manager will retry its network
     * operations when no progress is happening before it gives up.
     */
    public static final int MAX_RETRIES = 5;

    /** The column that is used for the downloads's ETag */
    public static final String ETAG = "etag";

}
