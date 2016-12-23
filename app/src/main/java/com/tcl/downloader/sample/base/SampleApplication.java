package com.tcl.downloader.sample.base;

import org.aisen.android.common.context.GlobalContext;
import org.aisen.android.common.utils.Logger;
import org.aisen.android.component.bitmaploader.BitmapLoader;
import org.aisen.android.ui.activity.basic.BaseActivity;
import org.aisen.download.DownloadManager;
import org.aiwen.downloader.Configuration;
import org.aiwen.downloader.Hawk;

/**
 * Created by wangdan on 16/5/7.
 */
public class SampleApplication extends GlobalContext {

    @Override
    public void onCreate() {
        super.onCreate();

        Logger.DEBUG = true;

        BaseActivity.setHelper(SampleActivityHelper.class);

        BitmapLoader.newInstance(this, String.valueOf(getExternalFilesDir("images")));

        DownloadManager.setup(this, true, 3);

        Hawk.setupWithConfig(this, new Configuration());
    }

}
