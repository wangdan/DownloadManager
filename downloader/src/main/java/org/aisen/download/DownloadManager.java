package org.aisen.download;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import org.aisen.download.utils.Constants;
import org.aisen.download.utils.DLogger;
import org.aisen.download.utils.Utils;

import java.util.UUID;

/**
 * Created by wangdan on 16/7/30.
 */
public class DownloadManager {

    public static final String TAG = Constants.TAG + "_DownloadManager";

    /**
     * the download is waiting to start.
     */
    public final static int STATUS_PENDING = 1 << 0;

    /**
     * the download is currently running.
     */
    public final static int STATUS_RUNNING = 1 << 1;

    /**
     * the download is waiting to retry or resume.
     */
    public final static int STATUS_PAUSED = 1 << 2;

    /**
     * the download has successfully completed.
     */
    public final static int STATUS_SUCCESSFUL = 1 << 3;

    /**
     * the download has failed (and will not be retried).
     */
    public final static int STATUS_FAILED = 1 << 4;

    /**
     * 等待中...，
     * 代表这三个状态 STATUS_WAITING_TO_RETRY、STATUS_WAITING_FOR_NETWORK、STATUS_QUEUED_FOR_WIFI
     * 见代码 Utils.translateStatus()
     */
    public final static int STATUS_WAITING = 1 << 5;

    /**
     * the download is paused because some network error
     * occurred and the download manager is waiting before retrying the request.
     */
    public final static int PAUSED_WAITING_TO_RETRY = 1;

    /**
     * the download is waiting for network connectivity to
     * proceed.
     */
    public final static int PAUSED_WAITING_FOR_NETWORK = 2;

    /**
     * the download exceeds a size limit for downloads over
     * the mobile network and the download manager is waiting for a Wi-Fi connection to proceed.
     */
    public final static int PAUSED_QUEUED_FOR_WIFI = 3;

    /**
     * the download is paused for some other reason.
     */
    public final static int PAUSED_UNKNOWN = 4;

    /**
     * Value of COLUMN_ERROR_CODE when the download has completed with an error that doesn't fit
     * under any other error code.
     */
    public final static int ERROR_UNKNOWN = 1000;

    /**
     * a storage issue arises which doesn't fit under any
     * other error code. Use the more specific {@link #ERROR_INSUFFICIENT_SPACE} and
     * when appropriate.
     */
    public final static int ERROR_FILE_ERROR = 1001;

    /**
     * an HTTP code was received that download manager
     * can't handle.
     */
    public final static int ERROR_UNHANDLED_HTTP_CODE = 1002;

    /**
     * an error receiving or processing data occurred at
     * the HTTP level.
     */
    public final static int ERROR_HTTP_DATA_ERROR = 1004;

    /**
     * there were too many redirects.
     */
    public final static int ERROR_TOO_MANY_REDIRECTS = 1005;

    /**
     * there was insufficient storage space. Typically,
     * this is because the SD card is full.
     */
    public final static int ERROR_INSUFFICIENT_SPACE = 1006;

    /**
     * some possibly transient error occurred but we can't
     * resume the download.
     */
    public final static int ERROR_CANNOT_RESUME = 1008;

    /**
     * the requested destination file already exists (the
     * download manager will not overwrite an existing file).
     */
    public final static int ERROR_FILE_ALREADY_EXISTS = 1009;

    /**
     * the download has failed because of
     *
     * @hide
     */
    public final static int ERROR_BLOCKED = 1010;

    public static final int DEFAULT_MAX_ALLOWED = 3;

    private static DownloadManager mDownloadManager;

    private Context mContext;
    private final DownloadController mController;
    private final int mMaxAllowed;

    public static synchronized void setup(Context context, boolean debug, int maxThread) {
        if (mDownloadManager == null) {
            mDownloadManager = new DownloadManager(context, maxThread);
        }

        DLogger.setup(context, debug);
    }

    public static DownloadManager getInstance() {
        return mDownloadManager;
    }

    private DownloadManager(Context context, int maxAllowed) {
        mContext = context;
        mController = new DownloadController();
        mMaxAllowed = maxAllowed;
    }

    public String generateKey(Uri uri, Uri fileUri) {
        return Utils.generateMD5(uri, fileUri);
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
        if (TextUtils.isEmpty(key))
            return;

        DownloadService.runAction(mContext, new RemoveAction(key));
    }

    public void query(Uri uri, Uri fileUri) {
        if (uri == null || fileUri == null) {
            return;
        }

        queryAndPublish(generateKey(uri, fileUri), true);
    }

    /**
     * 查询下载状态
     *
     * @param key
     * @return
     */
    private void query(String key) {
        if (TextUtils.isEmpty(key))
            return;

        queryAndPublish(key, false);
    }

    void queryAndPublish(String key, boolean publish) {
        DownloadService.runAction(mContext, new QueryAction(key, publish));
    }

    public DownloadController getController() {
        return mController;
    }

    public int getMaxAllowed() {
        return mMaxAllowed;
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

    final static class QueryAction extends Action {

        final String key;

        final boolean publish;

        QueryAction(String key, boolean publish) {
            this.key = key;
            this.publish = publish;
        }

        @Override
        String key() {
            return key;
        }

    }

    final static class RemoveAction extends Action {

        final String key;

        RemoveAction(String key) {
            this.key = key;
        }

        @Override
        String key() {
            return key;
        }

    }

}
