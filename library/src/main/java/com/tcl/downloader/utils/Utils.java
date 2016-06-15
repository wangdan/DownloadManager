package com.tcl.downloader.utils;

import com.tcl.downloader.DownloadManager;
import com.tcl.downloader.downloads.Downloads;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by wangdan on 16/6/15.
 */
public class Utils {

    public static int translateStatus(int status) {
        switch (status) {
            case Downloads.Impl.STATUS_PENDING:
                return DownloadManager.STATUS_PENDING;

            case Downloads.Impl.STATUS_RUNNING:
                return DownloadManager.STATUS_RUNNING;

            case Downloads.Impl.STATUS_PAUSED_BY_APP:
            case Downloads.Impl.STATUS_WAITING_TO_RETRY:
            case Downloads.Impl.STATUS_WAITING_FOR_NETWORK:
            case Downloads.Impl.STATUS_QUEUED_FOR_WIFI:
                return DownloadManager.STATUS_PAUSED;

            case Downloads.Impl.STATUS_SUCCESS:
                return DownloadManager.STATUS_SUCCESSFUL;

            default:
                assert Downloads.Impl.isStatusError(status);
                return DownloadManager.STATUS_FAILED;
        }
    }

    public static String generateMD5(String key) {
        try {
            MessageDigest e = MessageDigest.getInstance("MD5");
            e.update(key.getBytes());
            byte[] bytes = e.digest();
            StringBuilder sb = new StringBuilder();

            for(int i = 0; i < bytes.length; ++i) {
                String hex = Integer.toHexString(255 & bytes[i]);
                if(hex.length() == 1) {
                    sb.append('0');
                }

                sb.append(hex);
            }

            return sb.toString();
        } catch (NoSuchAlgorithmException var6) {
            return String.valueOf(key.hashCode());
        }
    }

}
