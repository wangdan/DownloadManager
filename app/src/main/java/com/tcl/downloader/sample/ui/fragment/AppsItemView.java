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
import org.aisen.download.DownloadManager;
import org.aisen.download.DownloadMsg;
import org.aisen.download.IDownloadObserver;
import org.aisen.download.IDownloadSubject;
import org.aisen.download.Request;

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
    private DownloadMsg downloadMsg;

    public AppsItemView(Context context, View itemView, IDownloadSubject proxy) {
        super(context, itemView);

        this.mProxy = proxy;
    }

    @Override
    public void onBindData(View view, AppBean app, int i) {
        synchronized (this) {
            if (mApp != null) {
                mProxy.detach(this);
            }
            mApp = app;
            mProxy.attach(this);
        }

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

        if (DownloadManager.getInstance() != null) {
            DownloadManager.getInstance().query(downloadURI(), downloadFileURI());
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mActionButton) {
            if (downloadMsg == null)
                return;

            final DownloadManager downloadManager = DownloadManager.getInstance();

            // 初始化状态，开始下载
            if (downloadMsg.isNull()) {
                AppBean app = (AppBean) mActionButton.getTag();

                Uri uri = Uri.parse(app.getApk_url());
                Uri fileUri = downloadFileURI();
                Request request = new Request(uri, fileUri);
                request.setTitle(mApp.getName());
                request.setDescription(mApp.getDescription());
                request.setNotificationVisibility(Request.VISIBILITY_VISIBLE);
                DownloadManager.getInstance().enqueue(request);
            }
            // 暂停状态，继续下载
            else if (downloadMsg.getStatus() == DownloadManager.STATUS_PAUSED) {
                downloadManager.resume(downloadMsg.getKey());
            }
            // 下载状态，暂停下载
            else if (downloadMsg.getStatus() == DownloadManager.STATUS_RUNNING ||
                            downloadMsg.getStatus() == DownloadManager.STATUS_WAITING) {
                downloadManager.pause(downloadMsg.getKey());
            }
            // 已下载状态，清除下载
            else if (downloadMsg.getStatus() == DownloadManager.STATUS_SUCCESSFUL) {
                new AlertDialog.Builder(getContext())
                                    .setMessage(String.format("%s 已下载，是否清除并删除文件？", mApp.getName()))
                                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {

                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            downloadManager.remove(downloadMsg.getKey());
                                        }

                                    })
                                    .setNegativeButton("取消", null)
                                    .show();
            }
            else {
                Toast.makeText(getContext(),
                                String.format("处理错误，请检查代码... AppInfo[%s], Status[%s]", mApp.getName(), downloadMsg.status2String()),
                                Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public Uri downloadURI() {
        if (mApp != null) {
            return Uri.parse(mApp.getApk_url());
        }

        return null;
    }

    @Override
    public Uri downloadFileURI() {
        if (mApp != null) {
            return Uri.fromFile(new File(SystemUtils.getSdcardPath() + "/" + mApp.getName() +  "123.apk"));
        }

        return null;
    }

    @Override
    public void onPublish(DownloadMsg downloadMsg) {
        this.downloadMsg = downloadMsg;

        if (downloadMsg.isNull()) {
            mActionButton.setText("下载");
            mActionButton.setProgress(0);
            setButtonNormal();
        }
        else {
            int status = downloadMsg.getStatus();

            // 失败
            if (status == DownloadManager.STATUS_FAILED) {
                mActionButton.setText("失败");

                setButtonNormal();
            }
            // 成功
            else if (status == DownloadManager.STATUS_SUCCESSFUL) {
                mActionButton.setText("已下载");
                mActionButton.setProgress(100);

                setButtonNormal();
            }
            // 暂停
            else if (status == DownloadManager.STATUS_PAUSED) {
                mActionButton.setText("继续");

                setButtonProgress(downloadMsg.getCurrent(), downloadMsg.getTotal());
            }
            else if (status == DownloadManager.STATUS_PENDING) {
                mActionButton.setText("等待");

                setButtonProgress(downloadMsg.getCurrent(), downloadMsg.getTotal());
            }
            // 等待
            else if (status == DownloadManager.STATUS_WAITING) {
                mActionButton.setText("等待");

                setButtonProgress(downloadMsg.getCurrent(), downloadMsg.getTotal());
            }
            // 下载中
            else if (status == DownloadManager.STATUS_RUNNING) {
                mActionButton.setText(Math.round(downloadMsg.getCurrent() * 100.0f / downloadMsg.getTotal()) + "%");

                setButtonProgress(downloadMsg.getCurrent(), downloadMsg.getTotal());
            }

            // 打印日志
            if (mApp != null) {
                if (downloadMsg.getCurrent() > 0 && downloadMsg.getTotal() > 0) {
                    long progress = downloadMsg.getCurrent();
                    long total = downloadMsg.getTotal();

                    Logger.v(TAG, "app[%s], status[%s], progress[%s], local_uri[%s], reason[%s]", mApp.getName(), downloadMsg.status2String(), Math.round(progress * 100.0f / total) + "%", downloadMsg.getFilePath().toString(), downloadMsg.getReason() + "");
                }
                else {
                    Logger.v(TAG, "app[%s], status[%s], reason[%s], local_uri[%s]", mApp.getName(), downloadMsg.status2String(), downloadMsg.getReason() + "", downloadMsg.getFilePath().toString());
                }
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
