package org.aiwen.downloader;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.text.TextUtils;

import org.aiwen.downloader.utils.Constants;
import org.aiwen.downloader.utils.Utils;

import static android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED;
import static android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION;

/**
 * Created by wangdan on 16/12/23.
 */
public class DownloadReceiver extends BroadcastReceiver {

    private static final String TAG = Constants.TAG + "_Receiver";

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (intent == null || TextUtils.isEmpty(intent.getAction())) {
            return;
        }

        String action = intent.getAction();

        DLogger.i(TAG, "action = %s", action);

        // 网络重连
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
            if (Utils.isWifiActive()) {
                DownloadService.retryByWIFI(context);
            }
        }
        // 重试下载
        else if (Constants.ACTION_RETRY.equals(action)) {
            String key = intent.getStringExtra("key");
            DLogger.i(TAG, "key = %s", key);
            if (!TextUtils.isEmpty(key)) {
                DownloadService.request(context, key);
            }
        }
        else if (Constants.ACTION_OPEN.equals(action)
                || Constants.ACTION_LIST.equals(action)
                || Constants.ACTION_HIDE.equals(action)) {
            final PendingResult result = goAsync();
            if (result == null) {
                handleNotificationBroadcast(context, intent);
            } else {
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        handleNotificationBroadcast(context, intent);
                        result.finish();
                    }

                }).start();
            }
        }
    }

    /**
     * Handle any broadcast related to a system notification.
     */
    private void handleNotificationBroadcast(Context context, Intent intent) {
        final String action = intent.getAction();
        if (Constants.ACTION_LIST.equals(action)) {
            final long[] ids = intent.getLongArrayExtra(
                    DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS);
//            sendNotificationClickedIntent(context, ids);

        } else if (Constants.ACTION_OPEN.equals(action)) {
            final long id = ContentUris.parseId(intent.getData());
//            openDownload(context, id);
//            hideNotification(context, id);

        } else if (Constants.ACTION_HIDE.equals(action)) {
            final long id = ContentUris.parseId(intent.getData());
            hideNotification(context, id);
        }
    }

    /**
     * Mark the given {@link DownloadManager#COLUMN_ID} as being acknowledged by
     * user so it's not renewed later.
     */
    private void hideNotification(Context context, long id) {
        Hawk hawk = Hawk.getInstance();
        if (hawk != null) {
            Request request = hawk.db.query(id);
            if (request == null) {
                return;
            }

            final int status = request.downloadInfo.status;
            final int visibility = request.downloadInfo.visibility;

            if (Downloads.Status.isStatusCompleted(status) &&
                    (visibility == VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                            || visibility == VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION)) {
                request.downloadInfo.visibility = Request.VISIBILITY_VISIBLE;

                synchronized (hawk.mRequestMap) {
                    hawk.db.update(request);
                    if (hawk.mRequestMap.containsKey(request.key)) {
                        hawk.mRequestMap.get(request.key).downloadInfo.visibility = request.downloadInfo.visibility;
                    }
                }
            }
        }
    }

}
