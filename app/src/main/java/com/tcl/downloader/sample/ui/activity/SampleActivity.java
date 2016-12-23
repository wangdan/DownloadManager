package com.tcl.downloader.sample.ui.activity;

import android.os.Bundle;

import com.tcl.downloader.sample.R;
import com.tcl.downloader.sample.ui.fragment.TabsFragment;

import org.aisen.android.ui.activity.basic.BaseActivity;
import org.aisen.download.DownloadService;
import org.aiwen.downloader.Configuration;
import org.aiwen.downloader.Hawk;

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

    }

}
