package org.aiwen.downloader.utils;

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

}
