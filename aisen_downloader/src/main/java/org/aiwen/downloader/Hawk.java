package org.aiwen.downloader;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by 王dan on 2016/12/17.
 */
public class Hawk {

    private static Hawk mInstance;

    /**
     * 在Application中初始化
     *
     * @param context
     */
    public static void setupWithConfig(Context context, Configuration config) {
        if (mInstance == null) {
            synchronized (Hawk.class) {
                mInstance = new Hawk(context, config);
            }
        }
    }

    @NonNull
    public static Hawk getInstance() {
        return mInstance;
    }

    private final Configuration mConfig;

    private final Context mContext;

    private final DownloadDB mDB;

    volatile ConcurrentHashMap<String, Request> mRequestMap;// 正在进行的请求

    private Hawk(Context context, Configuration config) {
        if (context != context.getApplicationContext()) {
            context = context.getApplicationContext();
        }

        mContext = context;
        mConfig = config;
        mRequestMap = new ConcurrentHashMap<>();
        mDB = new DownloadDB(context);

        DLogger.w("Hawk new instance");
    }

    Configuration getConfiguration() {
        return mConfig;
    }

    public static Request create(Uri uri, Uri fileUri) {
        String key = KeyGenerator.generateMD5(uri.toString());

        if (mInstance != null && mInstance.mRequestMap.containsKey(key)) {
            return mInstance.mRequestMap.get(key);
        }

        return new Request(uri, fileUri);
    }

    IDownloader createDownloader(Request request) {
        return new OkHttpDownloader();
    }

    /**
     * 开始下载
     *
     * @param request
     */
    public void enqueue(Request request) {
        // 已经有正在进行的请求了
        if (mRequestMap.containsKey(request.key)
                || mDB.exist(request)) {
            // 更新下载
            mDB.update(request);
        }
        else {
            // 新增下载
            mDB.insert(request);
        }

        // 放在内存中
        if (!mRequestMap.containsKey(request.key)) {
            mRequestMap.put(request.key, request);
        }

        DownloadService.request(mContext);
    }

}
