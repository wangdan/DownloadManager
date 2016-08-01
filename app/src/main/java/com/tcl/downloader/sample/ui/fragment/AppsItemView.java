package com.tcl.downloader.sample.ui.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.tcl.downloader.sample.R;
import com.tcl.downloader.sample.support.sdk.bean.AppBean;
import com.tcl.downloader.sample.support.utis.Utils;
import com.tcl.downloader.sample.ui.widget.ProgressButton;

import org.aisen.android.common.utils.Logger;
import org.aisen.android.common.utils.SystemUtils;
import org.aisen.android.component.bitmaploader.BitmapLoader;
import org.aisen.android.component.bitmaploader.core.ImageConfig;
import org.aisen.android.support.inject.ViewInject;
import org.aisen.android.ui.fragment.adapter.ARecycleViewItemView;
import org.aisen.download.Request;
import org.aisen.downloader.DLogger;
import org.aisen.downloader.DownloadController;
import org.aisen.downloader.DownloadManager;
import org.aisen.downloader.IDownloadObserver;
import org.aisen.downloader.IDownloadSubject;
import org.aisen.downloader.downloads.Downloads;

import java.io.File;

/**
 * Created by wangdan on 16/5/13.
 */
public class AppsItemView extends ARecycleViewItemView<AppBean> implements View.OnClickListener, IDownloadObserver {

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
    private DownloadController.DownloadStatus mStatus;

    public AppsItemView(Context context, View itemView, IDownloadSubject proxy) {
        super(context, itemView);

        this.mProxy = proxy;
    }

    @Override
    public void onBindData(View view, AppBean app, int i) {
        mApp = app;
        mProxy.attach(this);

        mTitle.setText(app.getName());
        mVersion.setVisibility(View.VISIBLE);
        mVersion.setText(Utils.formartDownloadSize(getContext(), app.getDownload_count(),
                app.getSize()));
        mRatingBar.setVisibility(View.VISIBLE);
        mRatingBar.setRating(app.getStars());

        mIntroduce.setText(app.getmEditRecommend() + "");

        mActionButton.setTag(app);
        mActionButton.setBorder(true);
        mActionButton.setBackgroundDrawable();
        mActionButton.setOnClickListener(this);

        if (!TextUtils.isEmpty(app.getIcon_url())) {
            ImageConfig config = new ImageConfig();
            config.setLoadingRes(R.drawable.ic_list_app_default);
            config.setLoadfaildRes(R.drawable.ic_list_app_default);
            BitmapLoader.getInstance().display(null, app.getIcon_url(), mIcon, config);
        }
        mSelfLeftMargin.setVisibility(View.GONE);
    }

    @Override
    public void onClick(View v) {
        if (v == mActionButton) {
            final DownloadManager downloadManager = DownloadManager.getInstance();

            // 初始化状态，开始下载
            if (mStatus == null || mStatus.status == -1) {
                AppBean app = (AppBean) mActionButton.getTag();

                Uri uri = Uri.parse(app.getApk_url());
                Uri fileUri = Uri.fromFile(new File(SystemUtils.getSdcardPath() + "/" + app.getName() +  "123.apk"));
                Request r = new Request(uri, fileUri);
                org.aisen.download.DownloadManager.getInstance().enqueue(r);




                DownloadManager.Request request = new DownloadManager.Request(uri);
//                request.setVisibleInDownloadsUi(true);// 文件可以被系统的Downloads应用扫描到并管理
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
                request.setTitle(app.getName());
                request.setDestinationUri(Uri.fromFile(new File(SystemUtils.getSdcardPath() + "/" + app.getName() +  ".apk")));
                final long reference = downloadManager.enqueue(request);
                DLogger.d(TAG, "enqueue reference[%s]", reference + "");
            }
            // 暂停状态，继续下载
            else if (mStatus.status == DownloadManager.STATUS_PAUSED) {
                downloadManager.resume(mStatus.id);
            }
            // 下载状态，暂停下载
            else if (mStatus.status == DownloadManager.STATUS_RUNNING ||
                            mStatus.status == DownloadManager.STATUS_WAITING) {
                downloadManager.pause(mStatus.id);
            }
            // 已下载状态，清除下载
            else if (mStatus.status == DownloadManager.STATUS_SUCCESSFUL) {
                new AlertDialog.Builder(getContext())
                                    .setMessage(String.format("%s 已下载，是否清除并删除文件？", mApp.getName()))
                                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {

                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Toast.makeText(getContext(), String.format("删除 %d 条数据", downloadManager.remove(mStatus.id)), Toast.LENGTH_SHORT).show();
                                        }

                                    })
                                    .setNegativeButton("取消", null)
                                    .show();
            }
            else {
                Toast.makeText(getContext(),
                                String.format("处理错误，请检查代码... AppInfo[%s], Status[%s]", mApp.getName(), Downloads.Impl.statusToString(mStatus.status)),
                                Toast.LENGTH_SHORT).show();
            }
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
    public void onDownloadInit() {
        Logger.d(TAG, "onDownloadInit[%s]", mApp.getName());

        mStatus = null;
        mActionButton.setText("下载");
        mActionButton.setProgress(0);
        setButtonNormal();
    }

    @Override
    public void onDownloadChanged(DownloadController.DownloadStatus status) {
        mStatus = status;

        // 失败
        if (status.status == DownloadManager.STATUS_FAILED) {
            mActionButton.setText("失败");

            setButtonNormal();
        }
        // 成功
        else if (status.status == DownloadManager.STATUS_SUCCESSFUL) {
            mActionButton.setText("已下载");
            mActionButton.setProgress(100);

            setButtonNormal();
        }
        // 暂停
        else if (status.status == DownloadManager.STATUS_PAUSED) {
            mActionButton.setText("继续");

            setButtonProgress(status.progress, status.total);
        }
        // 等待
        else if (status.status == DownloadManager.STATUS_PENDING ||
                        status.status == DownloadManager.STATUS_WAITING) {
            mActionButton.setText("等待");

            setButtonProgress(status.progress, status.total);
        }
        // 下载中
        else if (status.status == DownloadManager.STATUS_RUNNING) {
            mActionButton.setText(Math.round(status.progress * 100.0f / status.total) + "%");

            setButtonProgress(status.progress, status.total);
        }

        // 打印日志
        if (mApp != null) {
            if (status.progress > 0 && status.total > 0) {
                long progress = status.progress;
                long total = status.total;

                Logger.v(TAG, "app[%s], status[%s], progress[%s], local_uri[%s], reason[%s]", mApp.getName(), Downloads.Impl.statusToString(status.status), Math.round(progress * 100.0f / total) + "%", status.localUri, status.reason);
            }
            else {
                Logger.v(TAG, "app[%s], status[%s], reason[%s], local_uri[%s]", mApp.getName(), Downloads.Impl.statusToString(status.status), status.reason , status.localUri);
            }
        }
    }

    private void setButtonNormal() {
        mActionButton.setNormalColor(getContext().getResources().getColor(
                R.color.download_btn_normal));
        mActionButton.setPressColor(getContext().getResources().getColor(
                R.color.download_btn_normal));
        mActionButton.setProgressState(false);
        mActionButton.invalidate();
    }

    private void setButtonProgress(long progress, long total) {
        mActionButton.setProgressState(true);
        mActionButton.setNormalColor(getContext().getResources().getColor(
                R.color.download_btn_progress));
        mActionButton.setPressColor(getContext().getResources().getColor(
                R.color.download_btn_progress));
        mActionButton.setProgress(Math.round(progress * 100.0f / total));
        mActionButton.invalidate();
    }

}
