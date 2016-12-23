package org.aiwen.downloader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import org.aiwen.downloader.utils.Constants;

/**
 * Created by wangdan on 16/12/23.
 */
public class DownloadReceiver extends BroadcastReceiver {

    private static final String TAG = Constants.TAG + "_Receiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || TextUtils.isEmpty(intent.getAction())) {
            return;
        }

        String action = intent.getAction();

        DLogger.i(TAG, "action = %s", action);

        if (Constants.ACTION_RETRY.equals(action)) {
            String key = intent.getStringExtra("key");
            DLogger.i(TAG, "key = %s", key);
            if (!TextUtils.isEmpty(key)) {
                DownloadService.request(context, key);
            }
        }
    }

}
