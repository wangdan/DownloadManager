package com.tcl.downloader.sample.ui.activity;

import android.net.Uri;
import android.os.Bundle;

import com.tcl.downloader.sample.R;
import com.tcl.downloader.sample.ui.fragment.TabsFragment;

import org.aisen.android.ui.activity.basic.BaseActivity;
import org.aisen.download.DownloadService;
import org.aiwen.downloader.Configuration;
import org.aiwen.downloader.Hawk;
import org.aiwen.downloader.KeyGenerator;
import org.aiwen.downloader.Request;

import java.io.File;

/**
 * Created by wangdan on 16/5/7.
 */
public class SampleActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(org.aisen.android.R.layout.comm_ui_fragment_container);
        getFragmentManager().beginTransaction().add(R.id.fragmentContainer, TabsFragment.newInstance(), "SampleFragment").commit();

        getToolbar().setTitle("应用市场");

        DownloadService.retryAction(this);

        Hawk.setupWithConfig(SampleActivity.this, new Configuration());

        String[] urlArr = new String[]{ "http://apps.tclclouds.com/m3/apps/344?action=download", "http://dl.coolapkmarket.com/down/apk_file/2016/0819/org.aisen.weibo.sina-6.1.9-619.apk?_upt=83cd89b01482377853" };
        for (int i = 0; i < urlArr.length; i++) {
            String url = urlArr[i];

            Uri uri = Uri.parse(url);
            Uri fileUri = Uri.fromFile(new File(SampleActivity.this.getExternalFilesDir("apks") + File.separator + KeyGenerator.generateMD5(uri.toString()) + ".apk"));
            Request request = Hawk.create(uri, fileUri);

            Hawk.getInstance().enqueue(request);
        }
    }

}
