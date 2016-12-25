package org.aiwen.downloader.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.SystemClock;

import org.aiwen.downloader.DLogger;
import org.aiwen.downloader.Hawk;
import org.aiwen.downloader.Request;

import java.io.Closeable;

/**
 * Created by 王dan on 2016/12/21.
 */

public class Utils {

    public static void close(Closeable closeable) {
        try {
            if (closeable != null)
                closeable.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getDownloaderTAG(Request request) {
        return Constants.TAG + "_Thread_" + request.key;
    }

    public static long now() {
        return System.currentTimeMillis();
    }

    public static long realtime() {
        return SystemClock.elapsedRealtime();
    }

    public static void printStackTrace(Exception e) {
        DLogger.e(Constants.TAG, e + "");
        DLogger.printExc(Utils.class, e);
    }

    public static boolean isNetworkActive() {
        Hawk hawk = Hawk.getInstance();
        if (hawk != null) {
            ConnectivityManager connectivityManager = (ConnectivityManager) hawk.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

            return networkInfo != null && networkInfo.isConnected();
        }

        return false;
    }

    public static boolean isWifiActive() {
        Hawk hawk = Hawk.getInstance();
        if (hawk != null) {
            ConnectivityManager connectivityManager = (ConnectivityManager) hawk.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

            return networkInfo != null &&
                        networkInfo.getType() == ConnectivityManager.TYPE_WIFI &&
                        networkInfo.isConnected();
        }

        return false;
    }

}
