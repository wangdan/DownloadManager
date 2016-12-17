package org.aiwen.downloader;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.support.annotation.NonNull;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by 王dan on 2016/12/17.
 */
public class Hawk {

    /**
     * 在Application中初始化
     *
     * @param context
     */
    private static void setup(Context context) {
        if (mInstance == null) {
            synchronized (Hawk.class) {
                mInstance = new Hawk(context);
                mInstance.mRequestMap = new ConcurrentHashMap<>();
            }
        }
    }

    @NonNull
    public static Hawk getInstance(Context context) {
        if (mInstance == null) {
            setup(context);
        }

        return mInstance;
    }

    private static Hawk mInstance;

    private final Context mContext;

    private final DownloadDB mDB;

    private Hawk(Context context) {
        if (context != context.getApplicationContext()) {
            context = context.getApplicationContext();
        }

        mContext = context;
        mDB = new DownloadDB(context);

        DLogger.w("Hawk new instance");
    }

    private static volatile ConcurrentHashMap<String, Request> mRequestMap;// 正在进行的请求

    public static Request create(Uri uri) {
        String key = KeyGenerator.generateMD5(uri.toString());

        if (mInstance != null && mRequestMap.containsKey(key)) {
            return mRequestMap.get(key);
        }

        return new Request(uri);
    }


    /**
     * 开始下载
     *
     * @param request
     */
    public void enqueue(Request request) {
        // 已经有正在进行的请求了
        if (mRequestMap.containsKey(request.getKey())
                || mDB.exist(request)) {
            // 更新下载
            mDB.update(request);
        }
        else {
            // 新增下载
            mDB.insert(request);
        }

        // 放在内存中
        if (!mRequestMap.containsKey(request.getKey())) {
            mRequestMap.put(request.getKey(), request);
        }

        DownloadService.request(mContext);
    }

}
