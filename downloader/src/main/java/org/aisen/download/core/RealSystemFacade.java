package org.aisen.download.core;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.aisen.download.utils.Constants;
import org.aisen.download.utils.DLogger;

/**
 * Created by wangdan on 16/7/30.
 */
public class RealSystemFacade implements SystemFacade {
    private Context mContext;

    public RealSystemFacade(Context context) {
        mContext = context;
    }

    @Override
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    @Override
    public NetworkInfo getActiveNetworkInfo() {
        ConnectivityManager connectivity =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity == null) {
            Log.w(Constants.TAG, "couldn't get connectivity manager");
            return null;
        }

        final NetworkInfo activeInfo = connectivity.getActiveNetworkInfo();
        if (activeInfo == null) {
            DLogger.v(Constants.TAG, "network is not available");
        }
        return activeInfo;
    }

//    @Override
//    public boolean isActiveNetworkMetered() {
//        final ConnectivityManager conn = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);;
//        return conn.isActiveNetworkMetered();
//    }

    @Override
    public boolean isNetworkRoaming() {
        ConnectivityManager connectivity =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity == null) {
            Log.w(Constants.TAG, "couldn't get connectivity manager");
            return false;
        }

        NetworkInfo info = connectivity.getActiveNetworkInfo();
        boolean isMobile = (info != null && info.getType() == ConnectivityManager.TYPE_MOBILE);
        TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        boolean isRoaming = isMobile && tm.isNetworkRoaming();
        if (isRoaming) {
            DLogger.v(Constants.TAG, "network is roaming");
        }
        return isRoaming;
    }

    @Override
    public Long getMaxBytesOverMobile() {
        return DownloadManager.getMaxBytesOverMobile(mContext);
    }

    @Override
    public Long getRecommendedMaxBytesOverMobile() {
        return DownloadManager.getRecommendedMaxBytesOverMobile(mContext);
    }

}
