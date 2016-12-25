package org.aiwen.downloader;

import android.provider.BaseColumns;

/**
 * Created by 王dan on 2016/12/17.
 */

public final class Downloads {

    public static final class Impl implements BaseColumns {

        private Impl() {}

        public static final String COLUMN_KEY = "key";

        /**
         * The name of the column where the initiating application can provided the
         * title of this download. The title will be displayed ito the user in the
         * list of downloads.
         * <P>Type: TEXT</P>
         * <P>Owner can Init/Read/Write</P>
         */
        public static final String COLUMN_TITLE = "title";

        /**
         * The name of the column where the initiating application can provide the
         * description of this download. The description will be displayed to the
         * user in the list of downloads.
         * <P>Type: TEXT</P>
         * <P>Owner can Init/Read/Write</P>
         */
        public static final String COLUMN_DESCRIPTION = "description";

        /**
         * The name of the column containing the URI of the data being downloaded.
         * <P>Type: TEXT</P>
         * <P>Owner can Init/Read</P>
         */
        public static final String COLUMN_URI = "uri";

        /**
         * The name of the column containing the filename where the downloaded data
         * was actually stored.
         * <P>Type: TEXT</P>
         * <P>Owner can Read</P>
         */
        public static final String _DATA = "_data";

        /**
         * The name of the column containing the current status of the download.
         * Applications can read this to follow the progress of each download. See
         * the STATUS_* constants for a list of legal values.
         * <P>Type: INTEGER</P>
         * <P>Owner can Read</P>
         */
        public static final String COLUMN_STATUS = "status";

        /**
         * The column with errorMsg for a failed downloaded.
         * Used only for debugging purposes.
         * <P>Type: TEXT</P>
         */
        public static final String COLUMN_ERROR_MSG = "errorMsg";

        /** The column that is used to count retries */
        public static final String COLUMN_FAILED_CONNECTIONS = "numfailed";

        /**
         * The name of the column containing the date at which some interesting
         * status changed in the download. Stored as a System.currentTimeMillis()
         * value.
         * <P>Type: BIGINT</P>
         * <P>Owner can Read</P>
         */
        public static final String COLUMN_LAST_MODIFICATION = "lastmod";

        /**
         * The name of the column containing the total size of the file being
         * downloaded.
         * <P>Type: INTEGER</P>
         * <P>Owner can Read</P>
         */
        public static final String COLUMN_TOTAL_BYTES = "total_bytes";

        /**
         * The name of the column containing the size of the part of the file that
         * has been downloaded so far.
         * <P>Type: INTEGER</P>
         * <P>Owner can Read</P>
         */
        public static final String COLUMN_CURRENT_BYTES = "current_bytes";

        /**
         * The name of the column holding a bitmask of allowed network types.  This is only used for
         * public API downloads.
         * <P>Type: INTEGER</P>
         * <P>Owner can Init/Read</P>
         */
        public static final String COLUMN_ALLOWED_NETWORK_TYPES = "allowed_network_types";

        /**
         * If true, the user has confirmed that this download can proceed over the mobile network
         * even though it exceeds the recommended maximum size.
         * <P>Type: BOOLEAN</P>
         */
        public static final String COLUMN_BYPASS_RECOMMENDED_SIZE_LIMIT = "bypass_recommended_size_limit";

    }

    public static final class Status {

        /**
         * This download hasn't stated yet
         */
        public static final int STATUS_PENDING = 190;

        /**
         * This download has started
         */
        public static final int STATUS_RUNNING = 192;

        /**
         * This download has been paused by the owning app.
         */
        public static final int STATUS_PAUSED_BY_APP = 193;

        /**
         * This download encountered some network error and is waiting before retrying the request.
         */
        public static final int STATUS_WAITING_TO_RETRY = 194;

        /**
         * This download is waiting for network connectivity to proceed.
         */
        public static final int STATUS_WAITING_FOR_NETWORK = 195;

        /**
         * This download exceeded a size limit for mobile networks and is waiting for a Wi-Fi
         * connection to proceed.
         */
        public static final int STATUS_QUEUED_FOR_WIFI = 196;

        /**
         * This download couldn't be completed due to insufficient storage
         * space.  Typically, this is because the SD card is full.
         */
        public static final int STATUS_INSUFFICIENT_SPACE_ERROR = 198;

        /**
         * This download has successfully completed.
         * Warning: there might be other status values that indicate success
         * in the future.
         * Use isSucccess() to capture the entire category.
         */
        public static final int STATUS_SUCCESS = 200;

        /**
         * This download has completed with an error.
         * Warning: there will be other status values that indicate errors in
         * the future. Use isStatusError() to capture the entire category.
         */
        public static final int STATUS_UNKNOWN_ERROR = 491;

        /**
         * This download couldn't be completed because of a storage issue.
         * Typically, that's because the filesystem is missing or full.
         * Use the more specific {@link #STATUS_INSUFFICIENT_SPACE_ERROR}
         */
        public static final int STATUS_FILE_ERROR = 492;

        /**
         * This download couldn't be completed because of an
         * error receiving or processing data at the HTTP level.
         */
        public static final int STATUS_HTTP_DATA_ERROR = 495;

        /**
         * This download couldn't be completed because of an
         * HttpException while setting up the request.
         */
        public static final int STATUS_HTTP_EXCEPTION = 496;

        /**
         * Returns whether the status is a success (i.e. 2xx).
         */
        public static boolean isStatusSuccess(int status) {
            return (status >= 200 && status < 300);
        }

        /**
         * Returns whether the status is an error (i.e. 4xx or 5xx).
         */
        public static boolean isStatusError(int status) {
            return (status >= 400 && status < 600);
        }

        public static boolean isStatusRunning(int status) {
            return status == STATUS_PENDING || status == STATUS_RUNNING;
        }

        public static String statusToString(int status) {
            switch (status) {
                case STATUS_PENDING: return "PENDING";
                case STATUS_RUNNING: return "RUNNING";
                case STATUS_PAUSED_BY_APP: return "PAUSED_BY_APP";
                case STATUS_WAITING_TO_RETRY: return "WAITING_TO_RETRY";
                case STATUS_WAITING_FOR_NETWORK: return "WAITING_FOR_NETWORK";
                case STATUS_QUEUED_FOR_WIFI: return "QUEUED_FOR_WIFI";
                case STATUS_INSUFFICIENT_SPACE_ERROR: return "INSUFFICIENT_SPACE_ERROR";
                case STATUS_SUCCESS: return "SUCCESS";
//                case STATUS_BAD_REQUEST: return "BAD_REQUEST";
//                case STATUS_NOT_ACCEPTABLE: return "NOT_ACCEPTABLE";
//                case STATUS_LENGTH_REQUIRED: return "LENGTH_REQUIRED";
//                case STATUS_PRECONDITION_FAILED: return "PRECONDITION_FAILED";
//                case STATUS_FILE_ALREADY_EXISTS_ERROR: return "FILE_ALREADY_EXISTS_ERROR";
//                case STATUS_CANNOT_RESUME: return "CANNOT_RESUME";
//                case STATUS_CANCELED: return "CANCELED";
                case STATUS_UNKNOWN_ERROR: return "UNKNOWN_ERROR";
                case STATUS_FILE_ERROR: return "FILE_ERROR";
//                case STATUS_UNHANDLED_REDIRECT: return "UNHANDLED_REDIRECT";
//                case STATUS_UNHANDLED_HTTP_CODE: return "UNHANDLED_HTTP_CODE";
                case STATUS_HTTP_DATA_ERROR: return "HTTP_DATA_ERROR";
                case STATUS_HTTP_EXCEPTION: return "HTTP_EXCEPTION";
//                case STATUS_TOO_MANY_REDIRECTS: return "TOO_MANY_REDIRECTS";
//                case STATUS_BLOCKED: return "BLOCKED";
                default: return Integer.toString(status);
            }
        }

    }

}
