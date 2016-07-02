package org.aisen.downloader.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by wangdan on 16/6/15.
 */
public class Utils {

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

    public static String getMetaDataValue(Context context, String name) {
        PackageManager packageManager = context.getPackageManager();
        try {
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            if (applicationInfo != null && applicationInfo.metaData != null) {
                return applicationInfo.metaData.get(name).toString();
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        throw new RuntimeException("请配置MATE_DATA[" + name + "]");
    }

}
