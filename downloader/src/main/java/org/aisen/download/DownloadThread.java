package org.aisen.download;

import android.content.ContentValues;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Process;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Pair;

import org.aisen.download.core.DBHelper;
import org.aisen.download.core.DownloadInfo;
import org.aisen.download.core.DownloadInfo.NetworkState;
import org.aisen.download.core.Downloads;
import org.aisen.download.core.StopRequestException;
import org.aisen.download.core.SystemFacade;
import org.aisen.download.ui.DownloadNotifier;
import org.aisen.download.utils.ConnectivityManagerUtils;
import org.aisen.download.utils.Constants;
import org.aisen.download.utils.DLogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;

import static android.text.format.DateUtils.SECOND_IN_MILLIS;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_MOVED_PERM;
import static java.net.HttpURLConnection.HTTP_MOVED_TEMP;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_PARTIAL;
import static java.net.HttpURLConnection.HTTP_PRECON_FAILED;
import static java.net.HttpURLConnection.HTTP_SEE_OTHER;
import static java.net.HttpURLConnection.HTTP_UNAVAILABLE;
import static org.aisen.download.core.Downloads.Impl.STATUS_BAD_REQUEST;
import static org.aisen.download.core.Downloads.Impl.STATUS_CANCELED;
import static org.aisen.download.core.Downloads.Impl.STATUS_CANNOT_RESUME;
import static org.aisen.download.core.Downloads.Impl.STATUS_FILE_ERROR;
import static org.aisen.download.core.Downloads.Impl.STATUS_HTTP_DATA_ERROR;
import static org.aisen.download.core.Downloads.Impl.STATUS_SUCCESS;
import static org.aisen.download.core.Downloads.Impl.STATUS_TOO_MANY_REDIRECTS;
import static org.aisen.download.core.Downloads.Impl.STATUS_UNHANDLED_HTTP_CODE;
import static org.aisen.download.core.Downloads.Impl.STATUS_UNKNOWN_ERROR;
import static org.aisen.download.core.Downloads.Impl.STATUS_WAITING_FOR_NETWORK;
import static org.aisen.download.core.Downloads.Impl.STATUS_WAITING_TO_RETRY;

/**
 * Created by wangdan on 16/7/30.
 */
public class DownloadThread implements Runnable {

    private static final String TAG = Constants.TAG + "_DownloadThread";

    private static final int HTTP_REQUESTED_RANGE_NOT_SATISFIABLE = 416;
    private static final int HTTP_TEMP_REDIRECT = 307;

    private static final int DEFAULT_TIMEOUT = (int) (20 * SECOND_IN_MILLIS);

    private DBHelper mDbHelper;
    private final SystemFacade mSystemFacade;
    private final DownloadNotifier mNotifier;

    private final Object mLock = new Object();

    private final long mId;

    private final DownloadInfo mInfo;

    private ContentValues buildContentValues() {
        final ContentValues values = new ContentValues();

//        values.put(Downloads.Impl.COLUMN_URI, mInfo.mUri);
        values.put(Downloads.Impl._DATA, mInfo.mFilePath);
        values.put(Downloads.Impl.COLUMN_STATUS, mInfo.mStatus);
        if (mInfo.mStatus == Downloads.Impl.STATUS_RUNNING) {
            mInfo.mControl = Downloads.Impl.CONTROL_RUN;

            values.put(Downloads.Impl.COLUMN_CONTROL, Downloads.Impl.CONTROL_RUN);
        }
        else if (mInfo.mStatus == Downloads.Impl.STATUS_PAUSED_BY_APP) {
            mInfo.mControl = Downloads.Impl.CONTROL_PAUSED;

            values.put(Downloads.Impl.COLUMN_CONTROL, Downloads.Impl.CONTROL_PAUSED);
        }
        else {
            mInfo.mControl = Downloads.Impl.CONTROL_NONE;

            values.put(Downloads.Impl.COLUMN_CONTROL, Downloads.Impl.CONTROL_NONE);
        }
        values.put(Downloads.Impl.COLUMN_FAILED_CONNECTIONS, mInfo.mNumFailed);
        values.put(Downloads.Impl.COLUMN_TOTAL_BYTES, mInfo.mTotalBytes);
        values.put(Downloads.Impl.COLUMN_CURRENT_BYTES, mInfo.mCurrentBytes);
        values.put(Constants.ETAG, mInfo.mETag);

        mInfo.mLastMod = mSystemFacade.currentTimeMillis();
        values.put(Downloads.Impl.COLUMN_LAST_MODIFICATION, mInfo.mLastMod);
        values.put(Downloads.Impl.COLUMN_ERROR_MSG, mInfo.mErrorMsg);

        return values;
    }

    /**
     * Blindly push update of current delta values to provider.
     */
    private void writeToDatabase() {
        mDbHelper.update(mInfo.mKey, buildContentValues());
    }

    /**
     * Push update of current delta values to provider, asserting strongly
     * that we haven't been paused or deleted.
     */
    private void writeToDatabaseOrThrow() throws StopRequestException {
        if (mDbHelper.update(mInfo.mKey, buildContentValues()) == 0) {
            throw new StopRequestException(STATUS_CANCELED, "Download deleted or missing!");
        }
    }

    /**
     * Flag indicating if we've made forward progress transferring file data
     * from a remote server.
     */
    private boolean mMadeProgress = false;

    /**
     * Details from the last time we pushed a database update.
     */
    private long mLastUpdateBytes = 0;
    private long mLastUpdateTime = 0;

    private int mNetworkType = ConnectivityManagerUtils.TYPE_NONE;

    /** Historical bytes/second speed of this download. */
    private long mSpeed;
    /** Time when current sample started. */
    private long mSpeedSampleStart;
    /** Bytes transferred since current sample started. */
    private long mSpeedSampleBytes;

    private boolean mShutdown = false;
    private HttpURLConnection mConn;

    public DownloadThread(DBHelper dbHelper, SystemFacade systemFacade, DownloadNotifier notifier,
                          DownloadInfo info) {
        mDbHelper = dbHelper;
        mSystemFacade = systemFacade;
        mNotifier = notifier;

        mId = info.mId;
        mInfo = info;
    }

    public void shutDown() {
        synchronized (mLock) {
            this.mShutdown = true;
            if (mConn != null) {
                mConn.disconnect();
            }
        }
    }

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        if (mShutdown) {
            return;
        }

        try {
            synchronized (mInfo) {
                try {
                    File file = new File(Uri.parse(mInfo.mFilePath).getPath());
                    if (file.exists()) {
                        mInfo.mCurrentBytes = file.length();
                        mInfo.mTotalBytes = file.length();
                        mInfo.mStatus = Downloads.Impl.STATUS_SUCCESS;

                        publishDownload();
                        return;
                    }
                } catch (Exception ignore) {
                }

                if (mInfo.mStatus != Downloads.Impl.STATUS_RUNNING) {
                    mInfo.mStatus = Downloads.Impl.STATUS_RUNNING;
                    ContentValues values = new ContentValues();
                    values.put(Downloads.Impl.COLUMN_STATUS, mInfo.mStatus);
                    mDbHelper.update(mInfo.mKey, values);

                    publishDownload();
                }
            }

            logDebug("Starting");

            // Remember which network this download started on; used to
            // determine if errors were due to network changes.
            final NetworkInfo info = mSystemFacade.getActiveNetworkInfo();
            if (info != null) {
                mNetworkType = info.getType();
            }

            executeDownload();

            mInfo.mStatus = STATUS_SUCCESS;

            // If we just finished a chunked file, record total size
            if (mInfo.mTotalBytes == -1) {
                mInfo.mTotalBytes = mInfo.mCurrentBytes;
            }

        } catch (StopRequestException e) {
            if (!mShutdown) {
                mInfo.mStatus = e.getFinalStatus();
                mInfo.mErrorMsg = e.getMessage();

                logWarning("Stop requested with status "
                        + Downloads.Impl.statusToString(mInfo.mStatus) + ": "
                        + mInfo.mErrorMsg);

                // Nobody below our level should request retries, since we handle
                // failure counts at this level.
                if (mInfo.mStatus == STATUS_WAITING_TO_RETRY) {
                    throw new IllegalStateException("Execution should always throw final error codes");
                }

                // Some errors should be retryable, unless we fail too many times.
                if (isStatusRetryable(mInfo.mStatus)) {
                    if (mMadeProgress) {
                        mInfo.mNumFailed = 1;
                    } else {
                        mInfo.mNumFailed += 1;
                    }

                    if (mInfo.mNumFailed < Constants.MAX_RETRIES) {
                        final NetworkInfo info = mSystemFacade.getActiveNetworkInfo();
                        if (info != null && info.getType() == mNetworkType && info.isConnected()) {
                            // Underlying network is still intact, use normal backoff
                            mInfo.mStatus = STATUS_WAITING_TO_RETRY;
                        } else {
                            // Network changed, retry on any next available
                            mInfo.mStatus = STATUS_WAITING_FOR_NETWORK;
                        }

                        if ((mInfo.mETag == null && mMadeProgress)) {
                            // However, if we wrote data and have no ETag to verify
                            // contents against later, we can't actually resume.
                            mInfo.mStatus = STATUS_CANNOT_RESUME;
                        }
                    }
                }

                publishDownload();
            }
        } catch (Throwable t) {
            mInfo.mStatus = STATUS_UNKNOWN_ERROR;
            mInfo.mErrorMsg = t.toString();

            publishDownload();

            logError("Failed: " + mInfo.mErrorMsg, t);
        } finally {
            logDebug("Finished with status " + Downloads.Impl.statusToString(mInfo.mStatus));

            mNotifier.notifyDownloadSpeed(mId, 0);

            finalizeDestination();

            writeToDatabase();

            mInfo.threadFinished();

            publishDownload();
        }
    }

    /**
     * Fully execute a single download request. Setup and send the request,
     * handle the response, and transfer the data to the destination file.
     */
    private void executeDownload() throws StopRequestException {
        // 判断是否是断点续传
        try {
            File file = mInfo.getTempFile();
            // 文件存在，判断下载的文件是否和数据库保持一致，不一致先删除文件再重新下载
            if (file.exists()) {
                if (mInfo.mCurrentBytes != file.length()) {
                    file.delete();
                    mInfo.mCurrentBytes = 0;
                }
                else {
                    mInfo.mCurrentBytes = file.length();
                }
            }
            // 文件不存在，就重新下载
            else {
                mInfo.mCurrentBytes = 0;
            }
        } catch (IOException e) {
            throw new StopRequestException(STATUS_FILE_ERROR, e);
        }

        final boolean resuming = mInfo.mCurrentBytes != 0;

        URL url;
        try {
            // TODO: migrate URL sanity checking into client side of API
            url = new URL(mInfo.mUri);
        } catch (MalformedURLException e) {
            throw new StopRequestException(STATUS_BAD_REQUEST, e);
        }

        int redirectionCount = 0;
        while (redirectionCount++ < Constants.MAX_REDIRECTS) {
            // Open connection and follow any redirects until we have a useful
            // response with body.
            HttpURLConnection conn = null;
            try {
                checkConnectivity();
                conn = (HttpURLConnection) url.openConnection();
                mConn = conn;
                conn.setInstanceFollowRedirects(false);
                conn.setConnectTimeout(DEFAULT_TIMEOUT);
                conn.setReadTimeout(DEFAULT_TIMEOUT);

                addRequestHeaders(conn, resuming);

                final int responseCode = conn.getResponseCode();
                switch (responseCode) {
                    case HTTP_OK:
                        if (resuming) {
                            throw new StopRequestException(
                                    STATUS_CANNOT_RESUME, "Expected partial, but received OK");
                        }
                        parseOkHeaders(conn);
                        transferData(conn);
                        return;

                    case HTTP_PARTIAL:
                        if (!resuming) {
                            throw new StopRequestException(
                                    STATUS_CANNOT_RESUME, "Expected OK, but received partial");
                        }
                        transferData(conn);
                        return;

                    case HTTP_MOVED_PERM:
                    case HTTP_MOVED_TEMP:
                    case HTTP_SEE_OTHER:
                    case HTTP_TEMP_REDIRECT:
                        final String location = conn.getHeaderField("Location");
                        url = new URL(url, location);
                        if (responseCode == HTTP_MOVED_PERM) {
                            // Push updated URL back to database
                            mInfo.mUri = url.toString();
                        }
                        continue;

                    case HTTP_PRECON_FAILED:
                        throw new StopRequestException(
                                STATUS_CANNOT_RESUME, "Precondition failed");

                    case HTTP_REQUESTED_RANGE_NOT_SATISFIABLE:
                        throw new StopRequestException(
                                STATUS_CANNOT_RESUME, "Requested range not satisfiable");

                    case HTTP_UNAVAILABLE:
                        throw new StopRequestException(
                                HTTP_UNAVAILABLE, conn.getResponseMessage());

                    case HTTP_INTERNAL_ERROR:
                        throw new StopRequestException(
                                HTTP_INTERNAL_ERROR, conn.getResponseMessage());

                    default:
                        StopRequestException.throwUnhandledHttpError(
                                responseCode, conn.getResponseMessage());
                }

            } catch (IOException e) {
                if (e instanceof ProtocolException
                        && e.getMessage().startsWith("Unexpected status line")) {
                    throw new StopRequestException(STATUS_UNHANDLED_HTTP_CODE, e);
                } else {
                    // Trouble with low-level sockets
                    throw new StopRequestException(STATUS_HTTP_DATA_ERROR, e);
                }

            } finally {
                if (conn != null) conn.disconnect();
            }
        }

        throw new StopRequestException(STATUS_TOO_MANY_REDIRECTS, "Too many redirects");
    }

    /**
     * Transfer data from the given connection to the destination file.
     */
    private void transferData(HttpURLConnection conn) throws StopRequestException {

        // To detect when we're really finished, we either need a length, closed
        // connection, or chunked encoding.
        final boolean hasLength = mInfo.mTotalBytes != -1;
        final boolean isConnectionClose = "close".equalsIgnoreCase(
                conn.getHeaderField("Connection"));
        final boolean isEncodingChunked = "chunked".equalsIgnoreCase(
                conn.getHeaderField("Transfer-Encoding"));

        final boolean finishKnown = hasLength || isConnectionClose || isEncodingChunked;
        if (!finishKnown) {
            throw new StopRequestException(
                    STATUS_CANNOT_RESUME, "can't know size of download, giving up");
        }

        InputStream in = null;
        RandomAccessFile randomAccessFile = null;
        try {
            try {
                in = conn.getInputStream();
            } catch (IOException e) {
                throw new StopRequestException(STATUS_HTTP_DATA_ERROR, e);
            }
            File tempFile = null;
            try {
                tempFile = mInfo.getTempFile();
                // 先存临时文件
                if (!tempFile.getParentFile().exists())
                    tempFile.getParentFile().mkdirs();

                randomAccessFile = new RandomAccessFile(tempFile, "rwd");
                randomAccessFile.seek(randomAccessFile.length());
            } catch (IOException e) {
                throw new StopRequestException(STATUS_FILE_ERROR, e);
            }

            transferData(in, randomAccessFile, tempFile);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception ignore) {
            }
            try {
                if (randomAccessFile != null) {
                    randomAccessFile.close();
                }
            } catch (Exception ignore) {
            }
        }
    }

    /**
     * Transfer as much data as possible from the HTTP response to the
     * destination file.
     */
    private void transferData(InputStream in, RandomAccessFile out, File outFile)
            throws StopRequestException {
        final byte buffer[] = new byte[Constants.BUFFER_SIZE];
        while (true) {
            checkPausedOrCanceled();

            int len = -1;
            try {
                len = in.read(buffer);
            } catch (IOException e) {
                throw new StopRequestException(
                        STATUS_HTTP_DATA_ERROR, "Failed reading response: " + e, e);
            }

            if (len == -1) {
                break;
            }

            try {
                synchronized (mLock) {
                    if (!mShutdown) {
                        if (outFile != null && outFile.exists()) {
                            out.write(buffer, 0, len);
                        }
                        else {
                            throw new StopRequestException(STATUS_CANCELED, new IOException("temp file not exist"));
                        }

                        mMadeProgress = true;
                        mInfo.mCurrentBytes += len;

                        updateProgress();
                    }
                }
            } catch (IOException e) {
                throw new StopRequestException(STATUS_FILE_ERROR, e);
            }
        }

        // Finished without error; verify length if known
        if (mInfo.mTotalBytes != -1 && mInfo.mCurrentBytes != mInfo.mTotalBytes) {
            throw new StopRequestException(STATUS_HTTP_DATA_ERROR, "Content length mismatch");
        }
    }

    private void publishDownload() {
        if (DownloadManager.getInstance() != null) {
            DownloadManager.getInstance().getController().publishDownload(mInfo);
        }
    }

    /**
     * Called just before the thread finishes, regardless of status, to take any
     * necessary action on the downloaded file.
     */
    private void finalizeDestination() {
        if (Downloads.Impl.isStatusError(mInfo.mStatus)) {
            // Delete if local file
            if (!TextUtils.isEmpty(mInfo.mFilePath)) {
                File file = new File(mInfo.mFilePath);
                if (!file.exists())
                    file.delete();
            }

        } else if (Downloads.Impl.isStatusSuccess(mInfo.mStatus)) {
            // When success, open access if local file
            Uri beforeFileUri = Uri.parse(mInfo.mFilePath + Constants.TEMP_SUFFIX);
            Uri afterFileUri = Uri.parse(mInfo.mFilePath);

            final File before = new File(beforeFileUri.getPath());
            final File after = new File(afterFileUri.getPath());
            before.renameTo(after);
        }
    }

    /**
     * Check if current connectivity is valid for this request.
     */
    private void checkConnectivity() throws StopRequestException {
        final NetworkState networkUsable = mInfo.checkCanUseNetwork(mInfo.mTotalBytes);
        if (networkUsable != NetworkState.OK) {
            int status = Downloads.Impl.STATUS_WAITING_FOR_NETWORK;
            if (networkUsable == NetworkState.UNUSABLE_DUE_TO_SIZE) {
                status = Downloads.Impl.STATUS_QUEUED_FOR_WIFI;
                mInfo.notifyPauseDueToSize(true);
            } else if (networkUsable == NetworkState.RECOMMENDED_UNUSABLE_DUE_TO_SIZE) {
                status = Downloads.Impl.STATUS_QUEUED_FOR_WIFI;
                mInfo.notifyPauseDueToSize(false);
            }
            throw new StopRequestException(status, networkUsable.name());
        }
    }

    /**
     * Check if the download has been paused or canceled, stopping the request
     * appropriately if it has been.
     */
    private void checkPausedOrCanceled() throws StopRequestException {
        synchronized (mInfo) {
            if (mInfo.mControl == Downloads.Impl.CONTROL_PAUSED) {
                throw new StopRequestException(
                        Downloads.Impl.STATUS_PAUSED_BY_APP, "download paused by owner");
            }
            if (mInfo.mStatus == Downloads.Impl.STATUS_CANCELED) {
                throw new StopRequestException(Downloads.Impl.STATUS_CANCELED, "download canceled");
            }
        }
    }

    /**
     * Report download progress through the database if necessary.
     */
    private void updateProgress() throws IOException, StopRequestException {
        final long now = SystemClock.elapsedRealtime();
        final long currentBytes = mInfo.mCurrentBytes;

        final long sampleDelta = now - mSpeedSampleStart;
        if (sampleDelta > 500) {
            final long sampleSpeed = ((currentBytes - mSpeedSampleBytes) * 1000)
                    / sampleDelta;

            if (mSpeed == 0) {
                mSpeed = sampleSpeed;
            } else {
                mSpeed = ((mSpeed * 3) + sampleSpeed) / 4;
            }

            // Only notify once we have a full sample window
            if (mSpeedSampleStart != 0) {
                mNotifier.notifyDownloadSpeed(mId, mSpeed);
            }

            mSpeedSampleStart = now;
            mSpeedSampleBytes = currentBytes;

            publishDownload();
        }

        final long bytesDelta = currentBytes - mLastUpdateBytes;
        final long timeDelta = now - mLastUpdateTime;
        if (bytesDelta > Constants.MIN_PROGRESS_STEP && timeDelta > Constants.MIN_PROGRESS_TIME) {
            writeToDatabaseOrThrow();

            mLastUpdateBytes = currentBytes;
            mLastUpdateTime = now;
        }
    }

    /**
     * Process response headers from first server response. This derives its
     * filename, size, and ETag.
     */
    private void parseOkHeaders(HttpURLConnection conn) throws StopRequestException {
        final String transferEncoding = conn.getHeaderField("Transfer-Encoding");
        if (transferEncoding == null) {
            mInfo.mTotalBytes = getHeaderFieldLong(conn, "Content-Length", -1);
        } else {
            mInfo.mTotalBytes = -1;
        }

        mInfo.mETag = conn.getHeaderField("ETag");

        writeToDatabaseOrThrow();

        // Check connectivity again now that we know the total size
        checkConnectivity();

        publishDownload();
    }

    /**
     * Add custom headers for this download to the HTTP request.
     */
    private void addRequestHeaders(HttpURLConnection conn, boolean resuming) throws StopRequestException {
        for (Pair<String, String> header : mInfo.getHeaders()) {
            conn.addRequestProperty(header.first, header.second);
        }

//        // Only splice in user agent when not already defined
//        if (conn.getRequestProperty("User-Agent") == null) {
//            conn.addRequestProperty("User-Agent", mInfo.getUserAgent());
//        }

        // Defeat transparent gzip compression, since it doesn't allow us to
        // easily resume partial downloads.
        conn.setRequestProperty("Accept-Encoding", "identity");

        // Defeat connection reuse, since otherwise servers may continue
        // streaming large downloads after cancelled.
        conn.setRequestProperty("Connection", "close");

        if (resuming) {
            if (mInfo.mETag != null) {
                conn.addRequestProperty("If-Match", mInfo.mETag);
            }

            // 断点续传
            conn.addRequestProperty("Range", "bytes=" + mInfo.mCurrentBytes + "-");
        }
    }

    private void logDebug(String msg) {
        DLogger.d(TAG, "[" + mId + "] " + msg);
    }

    private void logWarning(String msg) {
        DLogger.w(TAG, "[" + mId + "] " + msg);
    }

    private void logError(String msg, Throwable t) {
        DLogger.e(TAG, "[" + mId + "] " + msg, t);
    }

    private static long getHeaderFieldLong(URLConnection conn, String field, long defaultValue) {
        try {
            return Long.parseLong(conn.getHeaderField(field));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Return if given status is eligible to be treated as
     * {@link Downloads.Impl#STATUS_WAITING_TO_RETRY}.
     */
    public static boolean isStatusRetryable(int status) {
        switch (status) {
            case STATUS_HTTP_DATA_ERROR:
            case HTTP_UNAVAILABLE:
            case HTTP_INTERNAL_ERROR:
            case STATUS_FILE_ERROR:
                return true;
            default:
                return false;
        }
    }

}
