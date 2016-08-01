package org.aisen.download.utils;

import android.os.Build;
import android.text.TextUtils;

/**
 * Created by wangdan on 16/7/30.
 */
public final class Constants {

    public static final String TAG = "DownloadManager";

    /** The default user agent used for downloads */
    public static final String DEFAULT_USER_AGENT;

    static {
        final StringBuilder builder = new StringBuilder();

        final boolean validRelease = !TextUtils.isEmpty(Build.VERSION.RELEASE);
        final boolean validId = !TextUtils.isEmpty(Build.ID);
        final boolean includeModel = "REL".equals(Build.VERSION.CODENAME)
                && !TextUtils.isEmpty(Build.MODEL);

        builder.append("AndroidDownloadManager");
        if (validRelease) {
            builder.append("/").append(Build.VERSION.RELEASE);
        }
        builder.append(" (Linux; U; Android");
        if (validRelease) {
            builder.append(" ").append(Build.VERSION.RELEASE);
        }
        if (includeModel || validId) {
            builder.append(";");
            if (includeModel) {
                builder.append(" ").append(Build.MODEL);
            }
            if (validId) {
                builder.append(" Build/").append(Build.ID);
            }
        }
        builder.append(")");

        DEFAULT_USER_AGENT = builder.toString();
    }

    /**
     * Name of directory on cache partition containing in-progress downloads.
     */
    public static final String DIRECTORY_CACHE_RUNNING = "partial_downloads";

    /**
     * When a number has to be appended to the filename, this string is used to separate the
     * base filename from the sequence number
     */
    public static final String FILENAME_SEQUENCE_SEPARATOR = "-";

    /** A magic filename that is allowed to exist within the system cache */
    public static final String RECOVERY_DIRECTORY = "recovery";

    /** The default base name for downloaded files if we can't get one at the HTTP level */
    public static final String DEFAULT_DL_FILENAME = "downloadfile";

    /** The default extension for html files if we can't get one at the HTTP level */
    public static final String DEFAULT_DL_HTML_EXTENSION = ".html";

    /** The default extension for text files if we can't get one at the HTTP level */
    public static final String DEFAULT_DL_TEXT_EXTENSION = ".txt";

    /** The default extension for binary files if we can't get one at the HTTP level */
    public static final String DEFAULT_DL_BINARY_EXTENSION = ".bin";

    public static final String TEMP_SUFFIX = ".at";

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
