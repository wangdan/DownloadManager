package org.aiwen.downloader.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.SystemClock;

import org.aiwen.downloader.DLogger;
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

    public static long realtime() {
        return SystemClock.elapsedRealtime();
    }

    public static void printStackTrace(Exception e) {
        DLogger.e(Constants.TAG, e + "");
        DLogger.printExc(Utils.class, e);
    }

    public static boolean isNetworkActive(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        return networkInfo != null && networkInfo.isConnected();
    }

}
