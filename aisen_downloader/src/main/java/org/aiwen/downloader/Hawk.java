package org.aiwen.downloader;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by 王dan on 2016/12/17.
 */
public class Hawk implements IDownloadSubject {

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

    private final ConcurrentMap<String, RequestSubject> mRequestSubjects;

    final DownloadDB db;

    final HawkTrace trace;

    volatile ConcurrentHashMap<String, Request> mRequestMap;// 正在进行的请求

    private Hawk(Context context, Configuration config) {
        if (context != context.getApplicationContext()) {
            context = context.getApplicationContext();
        }

        mContext = context;
        mConfig = config;
        mRequestMap = new ConcurrentHashMap<>();
        db = new DownloadDB(context);
        trace = new HawkTrace();
        mRequestSubjects = new ConcurrentHashMap<>();

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

    /**
     * 开始下载
     *
     * @param request
     */
    public void enqueue(Request request) {
        // 已经有正在进行的请求了
        synchronized (mRequestMap) {
            Request copyRequest = mRequestMap.get(request.key);
            if (copyRequest == null) {
                copyRequest = Request.copy(request);
            }

            if (mRequestMap.containsKey(copyRequest.key)
                    || db.exist(copyRequest)) {
                // 更新下载
                db.update(copyRequest);
            }
            else {
                // 新增下载
                db.insert(copyRequest);
            }

            // 放在内存中
            if (!mRequestMap.containsKey(copyRequest.key)) {
                mRequestMap.put(copyRequest.key, copyRequest);
            }

            DownloadService.request(mContext, copyRequest.key);
        }
    }

    public HawkTrace getTrace() {
        return trace;
    }

    @Override
    public void attach(IDownloadObserver observer) {
        Request request = observer.getRequest();

        RequestSubject requestSubject = mRequestSubjects.get(request.key);
        if (requestSubject == null) {
            requestSubject = new RequestSubject();

            mRequestSubjects.put(request.key, requestSubject);
        }

        requestSubject.attach(observer);
    }

    @Override
    public void detach(IDownloadObserver observer) {
        Request request = observer.getRequest();

        RequestSubject requestSubject = mRequestSubjects.get(request.key);
        if (requestSubject == null) {
            return;
        }

        requestSubject.detach(observer);

        if (requestSubject.empty()) {
            mRequestSubjects.remove(request.key);
        }
    }

    @Override
    public void notifyStatus(Request request) {
        RequestSubject requestSubject = mRequestSubjects.get(request.key);
        if (requestSubject != null) {
            requestSubject.notifyStatus(request);
        }
    }

}
