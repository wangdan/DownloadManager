package org.aiwen.downloader;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.TextUtils;

/**
 * 维护下载的服务
 *
 * Created by wangdan on 16/11/18.
 */
public class DownloadService extends Service {

    public static final String ACTION_REQUEST = "org.aisen.downloader.ACTION_REQUEST";

    public static void request(Context context) {
        Intent service = new Intent(context, DownloadService.class);
        service.setAction(ACTION_REQUEST);

        context.startService(service);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        do {
            if (intent == null || TextUtils.isEmpty(intent.getAction())) {
                break;
            }

            if (ACTION_REQUEST.equals(intent.getAction())) {

            }

        } while (false);

        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
