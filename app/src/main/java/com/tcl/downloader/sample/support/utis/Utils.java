package com.tcl.downloader.sample.support.utis;

import android.content.Context;

import com.tcl.downloader.sample.R;

import java.text.DecimalFormat;

/**
 * Created by wangdan on 16/6/22.
 */
public class Utils {

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
