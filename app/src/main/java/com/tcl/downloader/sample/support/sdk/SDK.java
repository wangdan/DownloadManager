package com.tcl.downloader.sample.support.sdk;

import com.tcl.downloader.sample.support.sdk.bean.AppBeans;

import org.aisen.android.common.setting.Setting;
import org.aisen.android.network.biz.ABizLogic;
import org.aisen.android.network.http.HttpConfig;
import org.aisen.android.network.http.Params;
import org.aisen.android.network.task.TaskException;

/**
 * Created by wangdan on 16/5/13.
 */
public class SDK extends ABizLogic {

    @Override
    protected HttpConfig configHttpConfig() {
        HttpConfig config = new HttpConfig();

        config.baseUrl = "http://apps.tclclouds.com/api";

        return config;
    }

    private SDK() {

    }

    public static SDK newInstance() {
        return new SDK();
    }

    public AppBeans getAppBeans(int page) throws TaskException {
        Setting action = newSetting("getAppBeans", "/featured", "");

        Params params = new Params();
        params.addParameter("page", String.valueOf(page));
        params.addParameter("per_page", "10");

        return doGet(action, configBasicParams(params), AppBeans.class);
    }

    private Params configBasicParams(Params params) {
        if (params == null) {
            params = new Params();
        }

        params.addParameter("encoder", "debug");

        return params;
    }

}
