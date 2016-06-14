package com.tcl.downloader.sample.base;

import com.tcl.downloader.DownloadManager;

import org.aisen.android.common.context.GlobalContext;
import org.aisen.android.common.utils.Logger;
import org.aisen.android.component.bitmaploader.BitmapLoader;

/**
 * Created by wangdan on 16/5/7.
 */
public class SampleApplication extends GlobalContext {

    @Override
    public void onCreate() {
        super.onCreate();

        Logger.DEBUG = true;

        BitmapLoader.newInstance(this, String.valueOf(getExternalFilesDir("images")));

        new DownloadManager.Builder(this).setDebug(true).build();
    }

}
