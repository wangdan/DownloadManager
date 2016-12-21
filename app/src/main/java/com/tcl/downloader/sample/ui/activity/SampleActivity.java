package com.tcl.downloader.sample.ui.activity;

import android.net.Uri;
import android.os.Bundle;

import com.tcl.downloader.sample.R;
import com.tcl.downloader.sample.support.utis.SdcardPermissionAction;
import com.tcl.downloader.sample.ui.fragment.TabsFragment;

import org.aisen.android.common.utils.SystemUtils;
import org.aisen.android.ui.activity.basic.BaseActivity;
import org.aisen.download.DownloadService;
import org.aiwen.downloader.Configuration;
import org.aiwen.downloader.Hawk;
import org.aiwen.downloader.KeyGenerator;
import org.aiwen.downloader.Request;

import java.io.File;
import java.util.Random;

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

        Uri uri = Uri.parse("http://apps.tclclouds.com/m3/apps/344?action=download");
        Uri fileUri = Uri.fromFile(new File(SampleActivity.this.getExternalFilesDir("apks") + File.separator + KeyGenerator.generateMD5(uri.toString()) + ".apk"));
        Request request = Hawk.create(uri, fileUri);

        Hawk.getInstance().enqueue(request);

//        for (int i = 0; i < 1; i++) {
//            new Thread() {
//
//                @Override
//                public void run() {
//                    super.run();
//
//                    try {
//                        Thread.sleep(100 * new Random().nextInt(5));
//                    } catch (Exception e) {
//                    }
//
//                    for (int j = 0; j < 10; j++) {
//                        Uri uri = Uri.parse("http://diycode.b0.upaiyun.com/photo/2016/33e66af9c0d1dfed88f09f7780405000" + "" + ".png");
//                        Uri fileUri = Uri.fromFile(new File(SampleActivity.this.getCacheDir().getAbsolutePath() + File.separator + "text.png"));
//                        Request request = Hawk.create(uri, fileUri);
//
//                        Hawk.getInstance().enqueue(request);
//                    }
//                }
//
//            }.start();
//        }
    }

}
