package com.tcl.downloader.sample;

import android.app.Application;
import android.util.Log;

import com.tcl.downloader.DownloadManager;
import com.tcl.downloader.utils.Logger;

/**
 * Created by wangdan on 16/5/7.
 */
public class SampleApplication extends Application {

    @Override
    public void onCreate() {
        Log.d("===", "start SampleApplication onCreate");

        super.onCreate();

        Log.d("===", "end SampleApplication onCreate");

        Logger.setup(this);
        DownloadManager.setup(getContentResolver(), getPackageName());
    }
}
