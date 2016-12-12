package com.tcl.downloader.sample.ui.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
import org.aisen.download.DownloadManager;
import org.aisen.download.DownloadProxy;

import java.io.Serializable;
import java.util.List;

/**
 * Created by wangdan on 16/5/13.
 */
public class AppsListFragment extends ARecycleViewFragment<AppBean, AppBeans, Serializable> {

    public static AppsListFragment newInstance(int type) {
        AppsListFragment fragment = new AppsListFragment();

        Bundle args = new Bundle();
        args.putInt("type", type);
        fragment.setArguments(args);

        return fragment;
    }

    private DownloadProxy mDownloadProxy;

    private int type;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDownloadProxy = new DownloadProxy();

        type = getArguments().getInt("type", 1);
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
                return new AppsItemView(getActivity(), view, mDownloadProxy);
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

            switch (type) {
                case 1:
                    return SDK.newInstance().getCompetitive(page);
                case 2:
                    return SDK.newInstance().getRanking(page);
                case 3:
                    return SDK.newInstance().getApps(page);
                case 4:
                    return SDK.newInstance().getGames(page);
                default:
                    return SDK.newInstance().getApps(page);
            }
        }

    }

    @Override
    public void onResume() {
        super.onResume();

        if (DownloadManager.getInstance() != null) {
            DownloadManager.getInstance().getController().register(mDownloadProxy);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (DownloadManager.getInstance() != null) {
            DownloadManager.getInstance().getController().unregister(mDownloadProxy);
        }
    }

}
