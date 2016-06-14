package com.tcl.downloader.sample.base;

import com.tcl.downloader.DownloadManager;
import com.tcl.downloader.sample.BuildConfig;

import org.aisen.android.common.context.GlobalContext;
import org.aisen.android.component.bitmaploader.BitmapLoader;

/**
 * Created by wangdan on 16/5/7.
 */
public class SampleApplication extends GlobalContext {

    @Override
    public void onCreate() {
        super.onCreate();

        BitmapLoader.newInstance(this, String.valueOf(getExternalFilesDir("images")));

        new DownloadManager.Builder(this).setDebug(BuildConfig.DEBUG).build();
    }
}
