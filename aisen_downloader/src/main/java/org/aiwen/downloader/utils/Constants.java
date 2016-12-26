package org.aiwen.downloader.utils;

/**
 * Created by 王dan on 2016/12/17.
 */

public class Constants {

    public static final String TAG = "Hawk";

    public static final String TEMP_SUFFIX = ".atp";

    /** The intent that gets sent when the service must wake up for a retry */
    public static final String ACTION_RETRY = "android.intent.action.DOWNLOAD_WAKEUP";

    /** the intent that gets sent when clicking an incomplete/failed download  */
    public static final String ACTION_LIST = "android.intent.action.DOWNLOAD_LIST";

    /** the intent that gets sent when clicking a successful download */
    public static final String ACTION_OPEN = "android.intent.action.DOWNLOAD_OPEN";

    /** the intent that gets sent when deleting the notification of a completed download */
    public static final String ACTION_HIDE = "android.intent.action.DOWNLOAD_HIDE";

    /** The minimum amount of progress that has to be done before the progress bar gets updated */
    public static final int MIN_PROGRESS_STEP = 65536;

    /** The minimum amount of time that has to elapse before the progress bar gets updated, in ms */
    public static final long MIN_PROGRESS_TIME = 1000;// 2000

    /**
     * The number of times that the download manager will retry its network
     * operations when no progress is happening before it gives up.
     */
    public static final int MAX_RETRIES = 5;

}
