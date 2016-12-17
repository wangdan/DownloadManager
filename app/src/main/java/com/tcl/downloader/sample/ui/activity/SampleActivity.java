package com.tcl.downloader.sample.ui.activity;

import android.net.Uri;
import android.os.Bundle;

import com.tcl.downloader.sample.R;
import com.tcl.downloader.sample.ui.fragment.TabsFragment;

import org.aisen.android.ui.activity.basic.BaseActivity;
import org.aisen.download.DownloadService;
import org.aiwen.downloader.Hawk;
import org.aiwen.downloader.Request;

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

        for (int i = 0; i < 10; i++) {
            new Thread() {

                @Override
                public void run() {
                    super.run();

                    try {
                        Thread.sleep(100 * new Random().nextInt(5));
                    } catch (Exception e) {
                    }

                    Request request = Hawk.create(Uri.parse("http://diycode.b0.upaiyun.com/photo/2016/33e66af9c0d1dfed88f09f7780405000.png"));

                    Hawk.getInstance(SampleActivity.this).enqueue(request);
                }

            }.start();
        }
    }

}
