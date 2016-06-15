package com.tcl.downloader.sample.ui.fragment;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.alibaba.fastjson.JSON;
import com.tcl.downloader.sample.R;
import com.tcl.downloader.sample.support.sdk.SDK;
import com.tcl.downloader.sample.support.sdk.bean.AppBean;
import com.tcl.downloader.sample.support.sdk.bean.AppBeans;

import org.aisen.android.network.task.TaskException;
import org.aisen.android.support.paging.IPaging;
import org.aisen.android.support.paging.PageIndexPaging;
import org.aisen.android.ui.fragment.ARecycleViewFragment;
import org.aisen.android.ui.fragment.itemview.IITemView;
import org.aisen.android.ui.fragment.itemview.IItemViewCreator;

import java.util.List;

/**
 * Created by wangdan on 16/5/13.
 */
public class AppListFragment extends ARecycleViewFragment<AppBean, AppBeans> {

    public static AppListFragment newInstance() {
        return new AppListFragment();
    }

    @Override
    public IItemViewCreator<AppBean> configItemViewCreator() {
        return new IItemViewCreator<AppBean>() {

            @Override
            public View newContentView(LayoutInflater layoutInflater, ViewGroup viewGroup, int i) {
                return layoutInflater.inflate(R.layout.item_app, viewGroup, false);
            }

            @Override
            public IITemView<AppBean> newItemView(View view, int i) {
                return new AppListItemView(getActivity(), view);
            }

        };
    }

    @Override
    public void requestData(RefreshMode refreshMode) {
        new AppListTask(refreshMode != RefreshMode.update ? RefreshMode.reset : refreshMode).execute();
    }

    @Override
    protected IPaging<AppBean, AppBeans> newPaging() {
        return new PageIndexPaging<>();
    }

    class AppListTask extends APagingTask<Void, Void, AppBeans> {

        public AppListTask(RefreshMode mode) {
            super(mode);
        }

        @Override
        protected List<AppBean> parseResult(AppBeans appBeans) {
            return appBeans.getItems();
        }

        @Override
        protected AppBeans workInBackground(RefreshMode refreshMode, String s, String s1, Void... voids) throws TaskException {
            int page = 1;
            if (!TextUtils.isEmpty(s1)) {
                page = Integer.parseInt(s1);
            }

            AppBeans beans = SDK.newInstance().getAppBeans(page);
            AppBean bean = beans.getItems().get(0);
            beans.getItems().add(0, JSON.parseObject(JSON.toJSONString(bean), AppBean.class));
            return beans;
        }

    }

}
