package org.aisen.download;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import org.aisen.download.core.DownloadInfo;
import org.aisen.download.core.Downloads;
import org.aisen.download.utils.Constants;
import org.aisen.download.utils.Utils;

import java.util.UUID;

/**
 * Created by wangdan on 16/7/30.
 */
public class DownloadManager {

    public static final String TAG = Constants.TAG + "_DownloadManager";

    /**
     * Current status of the download, as one of the STATUS_* constants.
     */
    public final static String COLUMN_STATUS = Downloads.Impl.COLUMN_STATUS;

    /**
     * Provides more detail on the status of the download.  Its meaning depends on the value of
     * {@link #COLUMN_STATUS}.
     *
     * When {@link #COLUMN_STATUS} is {@link #STATUS_FAILED}, this indicates the type of error that
     * occurred.  If an HTTP error occurred, this will hold the HTTP status code as defined in RFC
     * 2616.  Otherwise, it will hold one of the ERROR_* constants.
     *
     * When {@link #COLUMN_STATUS} is {@link #STATUS_PAUSED}, this indicates why the download is
     * paused.  It will hold one of the PAUSED_* constants.
     *
     * If {@link #COLUMN_STATUS} is neither {@link #STATUS_FAILED} nor {@link #STATUS_PAUSED}, this
     * column's value is undefined.
     *
     * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec6.html#sec6.1.1">RFC 2616
     * status codes</a>
     */
    public final static String COLUMN_REASON = "reason";

    /**
     * Value of {@link #COLUMN_STATUS} when the download is waiting to start.
     */
    public final static int STATUS_PENDING = 1 << 0;

    /**
     * Value of {@link #COLUMN_STATUS} when the download is currently running.
     */
    public final static int STATUS_RUNNING = 1 << 1;

    /**
     * Value of {@link #COLUMN_STATUS} when the download is waiting to retry or resume.
     */
    public final static int STATUS_PAUSED = 1 << 2;

    /**
     * Value of {@link #COLUMN_STATUS} when the download has successfully completed.
     */
    public final static int STATUS_SUCCESSFUL = 1 << 3;

    /**
     * Value of {@link #COLUMN_STATUS} when the download has failed (and will not be retried).
     */
    public final static int STATUS_FAILED = 1 << 4;

    /**
     * 等待中...，
     * 代表这三个状态 STATUS_WAITING_TO_RETRY、STATUS_WAITING_FOR_NETWORK、STATUS_QUEUED_FOR_WIFI
     * 见代码 Utils.translateStatus()
     */
    public final static int STATUS_WAITING = 1 << 5;

    /**
     * Value of COLUMN_ERROR_CODE when the download has completed with an error that doesn't fit
     * under any other error code.
     */
    public final static int ERROR_UNKNOWN = 1000;

    /**
     * Value of {@link #COLUMN_REASON} when a storage issue arises which doesn't fit under any
     * other error code. Use the more specific {@link #ERROR_INSUFFICIENT_SPACE} and
     * {@link #ERROR_DEVICE_NOT_FOUND} when appropriate.
     */
    public final static int ERROR_FILE_ERROR = 1001;

    /**
     * Value of {@link #COLUMN_REASON} when an HTTP code was received that download manager
     * can't handle.
     */
    public final static int ERROR_UNHANDLED_HTTP_CODE = 1002;

    /**
     * Value of {@link #COLUMN_REASON} when an error receiving or processing data occurred at
     * the HTTP level.
     */
    public final static int ERROR_HTTP_DATA_ERROR = 1004;

    /**
     * Value of {@link #COLUMN_REASON} when there were too many redirects.
     */
    public final static int ERROR_TOO_MANY_REDIRECTS = 1005;

    /**
     * Value of {@link #COLUMN_REASON} when there was insufficient storage space. Typically,
     * this is because the SD card is full.
     */
    public final static int ERROR_INSUFFICIENT_SPACE = 1006;

    /**
     * Value of {@link #COLUMN_REASON} when no external storage device was found. Typically,
     * this is because the SD card is not mounted.
     */
    public final static int ERROR_DEVICE_NOT_FOUND = 1007;

    /**
     * Value of {@link #COLUMN_REASON} when some possibly transient error occurred but we can't
     * resume the download.
     */
    public final static int ERROR_CANNOT_RESUME = 1008;

    /**
     * Value of {@link #COLUMN_REASON} when the requested destination file already exists (the
     * download manager will not overwrite an existing file).
     */
    public final static int ERROR_FILE_ALREADY_EXISTS = 1009;

    /**
     * Value of {@link #COLUMN_REASON} when the download has failed because of
     *
     * @hide
     */
    public final static int ERROR_BLOCKED = 1010;

    /**
     * Value of {@link #COLUMN_REASON} when the download is paused because some network error
     * occurred and the download manager is waiting before retrying the request.
     */
    public final static int PAUSED_WAITING_TO_RETRY = 1;

    /**
     * Value of {@link #COLUMN_REASON} when the download is waiting for network connectivity to
     * proceed.
     */
    public final static int PAUSED_WAITING_FOR_NETWORK = 2;

    /**
     * Value of {@link #COLUMN_REASON} when the download exceeds a size limit for downloads over
     * the mobile network and the download manager is waiting for a Wi-Fi connection to proceed.
     */
    public final static int PAUSED_QUEUED_FOR_WIFI = 3;

    /**
     * Value of {@link #COLUMN_REASON} when the download is paused for some other reason.
     */
    public final static int PAUSED_UNKNOWN = 4;

    private static DownloadManager mDownloadManager;

    private Context mContext;

    public static synchronized void setup(Context context, int maxThread) {
        if (mDownloadManager == null) {
            mDownloadManager = new DownloadManager(context, maxThread);
        }
    }

    public static DownloadManager getInstance() {
        return mDownloadManager;
    }

    private DownloadManager(Context context, int maxThread) {
        this.mContext = context;
    }

    public String generateKey(Uri uri, Uri fileUri) {
        return Utils.generateMD5(uri.toString() + fileUri.toString());
    }

    /**
     * 开始下载
     *
     * @param request
     * @return
     */
    public String enqueue(Request request) {
        DownloadService.runAction(mContext, new EnqueueAction(request));

        return request.mKey;
    }

    /**
     * 暂停下载
     *
     * @param key
     * @return
     */
    public void pause(String key) {
        if (TextUtils.isEmpty(key))
            return;

        DownloadService.runAction(mContext, new PauseAction(key));
    }

    /**
     * 继续下载
     *
     * @param key
     * @return
     */
    public void resume(String key) {
        if (TextUtils.isEmpty(key))
            return;

        DownloadService.runAction(mContext, new ResumeAction(key));
    }

    /**
     * 删除下载
     *
     * @param key
     * @return
     */
    public void remove(String key) {
    }

    /**
     * 查询下载状态
     *
     * @param key
     * @return
     */
    public DownloadInfo query(String key) {
        return null;
    }

    static abstract class Action {

        abstract String key();

    }

    final static class EnqueueAction extends Action {

        final Request request;

        final String uuid;

        EnqueueAction(Request request) {
            this.request = request;
            this.uuid = UUID.randomUUID().toString();
        }

        @Override
        String key() {
            return request.mKey;
        }

    }

    final static class PauseAction extends Action {

        final String key;

        PauseAction(String key) {
            this.key = key;
        }

        @Override
        String key() {
            return key;
        }

    }

    final static class ResumeAction extends Action {

        final String key;

        ResumeAction(String key) {
            this.key = key;
        }

        @Override
        String key() {
            return key;
        }

    }

}
