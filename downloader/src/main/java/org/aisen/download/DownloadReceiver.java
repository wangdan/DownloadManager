/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.aisen.download;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import org.aisen.download.utils.Constants;
import org.aisen.download.utils.DLogger;

/**
 * Receives system broadcasts (boot, network connectivity)
 */
public class DownloadReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {

            DLogger.d(Constants.TAG + "_DownloadReceiver", "Intent.ACTION_BOOT_COMPLETED");

            startService(context);

        } else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
            final ConnectivityManager connManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            final NetworkInfo info = connManager.getActiveNetworkInfo();
            if (info != null && info.isConnected()) {
                DLogger.d(Constants.TAG + "_DownloadReceiver", "ConnectivityManager.CONNECTIVITY_ACTION");
                startService(context);
            }

        } else if (Constants.ACTION_RETRY.equals(action)) {
            DLogger.d(Constants.TAG + "_DownloadReceiver", "Constants.ACTION_RETRY");
            startService(context);

        }
    }

    private void startService(Context context) {
        DownloadService.retryAction(context);
    }
}
