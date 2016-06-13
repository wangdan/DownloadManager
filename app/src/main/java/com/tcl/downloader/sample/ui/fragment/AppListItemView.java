package com.tcl.downloader.sample.ui.fragment;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.tcl.downloader.sample.R;
import com.tcl.downloader.sample.support.sdk.bean.AppBean;
import com.tcl.downloader.sample.ui.widget.ProgressButton;

import org.aisen.android.component.bitmaploader.BitmapLoader;
import org.aisen.android.component.bitmaploader.core.ImageConfig;
import org.aisen.android.support.inject.ViewInject;
import org.aisen.android.ui.fragment.adapter.ARecycleViewItemView;

import java.text.DecimalFormat;

/**
 * Created by wangdan on 16/5/13.
 */
public class AppListItemView extends ARecycleViewItemView<AppBean> {

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

    public AppListItemView(Context context, View itemView) {
        super(context, itemView);
    }

    @Override
    public void onBindData(View view, AppBean app, int i) {
        mTitle.setText(app.getName());
        mVersion.setVisibility(View.VISIBLE);
        mVersion.setText(formartDownloadSize(getContext(), app.getDownload_count(),
                app.getSize()));
        mRatingBar.setVisibility(View.VISIBLE);
        mRatingBar.setRating(app.getStars());

        mIntroduce.setText(app.getmEditRecommend() + "");

        mActionButton.setBorder(true);
        mActionButton.setBackgroundDrawable();
        mActionButton.setNormalColor(getContext().getResources().getColor(R.color.rgb_e4e4e4));

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

}
