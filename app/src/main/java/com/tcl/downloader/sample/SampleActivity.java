package com.tcl.downloader.sample;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import com.tcl.downloader.DownloadManager;
import com.tcl.downloader.utils.Logger;

import java.io.File;

/**
 * Created by wangdan on 16/5/7.
 */
public class SampleActivity extends Activity {

    static final String TAG = "Sample";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new Thread() {

            @Override
            public void run() {
                super.run();

                DownloadManager downloadManager = DownloadManager.getInstance();
                for (int i = 0; i < 10; i++) {

//                Uri uri = Uri.parse("http://h.hiphotos.baidu.com/baike/c0%3Dbaike116%2C5%2C5%2C116%2C38/sign=282fc9a9cffcc3cea0cdc161f32cbded/e7cd7b899e510fb3601b321cde33c895d1430c3e.jpg");
                    Uri uri = Uri.parse("https://buckets.apps.tclclouds.com/appstore/apk/com.tencent.reading/com.tencent.reading.apk?random_up=1450840707000&attname=com.tencent.reading_160.apk");
                    DownloadManager.Request request = new DownloadManager.Request(uri);
                    request.setVisibleInDownloadsUi(true);// 文件可以被系统的Downloads应用扫描到并管理
                    request.setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE);
                    request.setTitle("anglababy");
                    request.setDestinationUri(Uri.fromFile(new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/anglababy.apk")));
                    final long reference = downloadManager.enqueue(request);
                    Log.e("Sample", "" + reference);
                }

                boolean queryStatus = false;
                while (queryStatus) {
                    DownloadManager.Query query = new DownloadManager.Query();
                    Cursor c = downloadManager.query(query);
                    try {
                        if (c.moveToFirst()) {
                            // 下载进度
                            long progress = c.getLong(c.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                            // 总共大小
                            long total = c.getLong(c.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                            // 下载的实际文件地址
                            String localFilename = c.getString(c.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_LOCAL_FILENAME));
                            // 下载状态
                            int status = c.getInt(c.getColumnIndex(android.app.DownloadManager.COLUMN_STATUS));

                            Logger.v(TAG, "status = " + status + ", progress = " + progress + ", total = " + total + ", address = " + localFilename);

                            // 暂停了，开始计时，超时就认为下载失败
                            if (status == android.app.DownloadManager.STATUS_PAUSED) {
                                if (progress > 0 && total > 0) {
                                    publishProgress(progress, total);
                                }
                            }
                            //正在下载，不做任何事情
                            else if (status == android.app.DownloadManager.STATUS_PENDING ||
                                    status == android.app.DownloadManager.STATUS_RUNNING) {
                                publishProgress(progress, total);
                            }
                            // 下载完成
                            else if (status == android.app.DownloadManager.STATUS_SUCCESSFUL) {

                                publishProgress(1l, 1l);

                                queryStatus = false;
                                break;
                            }
                            // 下载失败
                            else if (status == android.app.DownloadManager.STATUS_FAILED) {
                                Log.e(TAG, "Download Failed");
                            }
                        }
                    } catch (Throwable e) {
                       e.printStackTrace();
                    } finally {
                        c.close();
                    }
                }
            }

        }.start();
    }

    private void publishProgress(long progress, long total) {
        if (total > 0)
            Log.d(TAG, "进度 : " + (progress * 100) / total + "%");
    }

}
