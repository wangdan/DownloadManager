package org.aiwen.downloader.utils;

import org.aiwen.downloader.DLogger;
import org.aiwen.downloader.DownloadException;
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

    public static void printStackTrace(Exception e) throws DownloadException {
        DLogger.e(Constants.TAG, e + "");
        DLogger.printExc(Utils.class, e);

        if (e instanceof DownloadException) {
            throw (DownloadException) e;
        }
    }

}
