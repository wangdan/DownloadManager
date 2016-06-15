package com.tcl.downloader.sample.ui.fragment;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.tcl.downloader.DownloadController;
import com.tcl.downloader.DownloadManager;
import com.tcl.downloader.IDownloadObserver;
import com.tcl.downloader.IDownloadSubject;
import com.tcl.downloader.sample.R;
import com.tcl.downloader.sample.support.sdk.bean.AppBean;
import com.tcl.downloader.sample.ui.widget.ProgressButton;

import org.aisen.android.common.utils.Logger;
import org.aisen.android.component.bitmaploader.BitmapLoader;
import org.aisen.android.component.bitmaploader.core.ImageConfig;
import org.aisen.android.support.inject.ViewInject;
import org.aisen.android.ui.fragment.adapter.ARecycleViewItemView;

import java.io.File;
import java.text.DecimalFormat;

/**
 * Created by wangdan on 16/5/13.
 */
public class AppListItemView extends ARecycleViewItemView<AppBean> implements View.OnClickListener, IDownloadObserver {

    static final String TAG = "AppItemView";

    @ViewInject(id = R.id.icon)
    ImageView mIcon;
    @ViewInject(id = R.id.name)
    TextView mTitle;
    @ViewInject(id = R.id.star_ratingbar)
    RatingBar mRatingBar;
    @ViewInject(id = R.id.action_download_button)
    ProgressButton mActionButton;
    @ViewInject(id = R.id.action_layout)
    View mActionView;
    @ViewInject(id = R.id.version)
    TextView mVersion;
    @ViewInject(id = R.id.introduce)
    TextView mIntroduce;
    @ViewInject(id = R.id.divider_self)
    View mSelfDivider;
    @ViewInject(id = R.id.margin_self)
    View mSelfMargin;
    @ViewInject(id = R.id.margin_self_left)
    View mSelfLeftMargin;
    @ViewInject(id = R.id.ranking_icon)
    TextView mRankingIcon;
    @ViewInject(id = R.id.search_ranking_icon)
    TextView mSearchRankingIcon;
    @ViewInject(id = R.id.icon_layout)
    View mIconView;

    private IDownloadSubject mProxy;
    private AppBean mApp;

    public AppListItemView(Context context, View itemView, IDownloadSubject proxy) {
        super(context, itemView);

        this.mProxy = proxy;
    }

    @Override
    public void onBindData(View view, AppBean app, int i) {
        mApp = app;
        mProxy.attach(this);

        mTitle.setText(app.getName());
        mVersion.setVisibility(View.VISIBLE);
        mVersion.setText(formartDownloadSize(getContext(), app.getDownload_count(),
                app.getSize()));
        mRatingBar.setVisibility(View.VISIBLE);
        mRatingBar.setRating(app.getStars());

        mIntroduce.setText(app.getmEditRecommend() + "");

        mActionButton.setTag(app);
        mActionButton.setBorder(true);
        mActionButton.setBackgroundDrawable();
        mActionButton.setNormalColor(getContext().getResources().getColor(R.color.rgb_e4e4e4));
        mActionButton.setOnClickListener(this);

        if (!TextUtils.isEmpty(app.getIcon_url())) {
            ImageConfig config = new ImageConfig();
            config.setLoadingRes(R.drawable.ic_list_app_default);
            config.setLoadfaildRes(R.drawable.ic_list_app_default);
            BitmapLoader.getInstance().display(null, app.getIcon_url(), mIcon, config);
        }
        mSelfLeftMargin.setVisibility(View.GONE);
    }

    public static String formartDownloadSize(Context context, long downloadTimes, long size) {
        String downloadCount = "";
        if (downloadTimes < 1000) {
            downloadCount = context.getString(R.string.unit_person,
                    String.valueOf(downloadTimes));
        } else if (downloadTimes >= 1000 && downloadTimes < 10000) {
            downloadCount = context.getString(
                    R.string.unit_thousand_person,
                    new DecimalFormat("#").format(downloadTimes / 1000));
        } else if (downloadTimes >= 10000 && downloadTimes < 10000000) {
            downloadCount = context.getString(
                    R.string.unit_ten_thousand_person,
                    new DecimalFormat("#").format(downloadTimes / 10000));
        } else if (downloadTimes >= 10000000 && downloadTimes < 100000000) {
            downloadCount = context.getString(
                    R.string.unit_thousand_ten_thousand_person,
                    new DecimalFormat("#").format(downloadTimes / 10000000));
        } else if (downloadTimes >= 100000000 && downloadTimes < 100000000000l) {
            downloadCount = context.getString(
                    R.string.unit_ten_ten_thousand_person,
                    new DecimalFormat("#").format(downloadTimes / 100000000));
        } else if (downloadTimes >= 100000000000l) {
            downloadCount = context.getString(
                    R.string.unit_thousand_ten_ten_thousand_person,
                    new DecimalFormat("#").format(downloadTimes / 100000000000l));
        }
        if (size > 0) {
            return (downloadCount + "  " + (String.format("%.1fM",
                    size / 1024.0 / 1024.0)));
        }
        return downloadCount;
    }

    @Override
    public void onClick(View v) {
        if (v == mActionButton) {
            AppBean app = (AppBean) mActionButton.getTag();

            DownloadManager downloadManager = DownloadManager.getInstance();
            Uri uri = Uri.parse(app.getApk_url());
            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.setVisibleInDownloadsUi(true);// 文件可以被系统的Downloads应用扫描到并管理
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
            request.setTitle(app.getName());
            request.setDestinationUri(Uri.fromFile(new File(getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/" + app.getName() +  ".apk")));
            final long reference = downloadManager.enqueue(request);
        }
    }

    @Override
    public String downloadURI() {
        if (mApp != null) {
            return mApp.getApk_url();
        }

        return null;
    }

    @Override
    public void onDownloadPrepare() {
        mActionButton.setText("下载");
    }

    @Override
    public void onDownloadChanged(DownloadController.DownloadStatus status) {
        if (mApp != null) {
            if (status.progress > 0 && status.total > 0) {
                long progress = status.progress;
                long total = status.total;

                Logger.v(TAG, "app[%s], status[%d], progress[%s]", mApp.getName(), status.status, Math.round(progress * 100.0f / total) + "%");
            }
            else {
                Logger.v(TAG, "app[%s], status[%d], error[%d]", mApp.getName(), status.status, status.error);
            }
        }

        // 失败
        if (status.status == DownloadManager.STATUS_FAILED) {
            mActionButton.setText("失败");
        }
        // 成功
        else if (status.status == DownloadManager.STATUS_SUCCESSFUL) {
            mActionButton.setText("已下载");
        }
        // 暂停
        else if (status.status == DownloadManager.STATUS_PAUSED) {
            mActionButton.setText("暂停");
        }
        // 等待
        else if (status.status == DownloadManager.STATUS_PENDING) {
            mActionButton.setText("等待");
        }
        // 下载中
        else if (status.status == DownloadManager.STATUS_RUNNING) {
            long progress = status.progress;
            long total = status.total;

            mActionButton.setNormalColor(getContext().getResources().getColor(
                    R.color.download_btn_progress));
            mActionButton.setPressColor(getContext().getResources().getColor(
                    R.color.download_btn_progress));
            mActionButton.setText(Math.round(progress * 100.0f / total) + "%");
            mActionButton.setProgress(Math.round(progress * 100.0f / total));
            mActionButton.setProgressState(true);
        }
    }

}
