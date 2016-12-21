package org.aiwen.downloader;

import org.aiwen.downloader.utils.Constants;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * 下载线程
 *
 * Created by wangdan on 16/11/18.
 */
public class DownloadThread implements Runnable {

    private static final String TAG = Constants.TAG + "_Thread";

    private final int mStartId;
    private final Request mRequest;
    private final IDownloader mDownloader;
    private final DownloadService mService;

    public DownloadThread(int startId, Request request, IDownloader downloader, DownloadService service) {
        mStartId = startId;
        mRequest = request;
        mRequest.downloadInfo.status = Downloads.Status.STATUS_PENDING;
        mDownloader = downloader;
        mService = service;
    }

    @Override
    public void run() {
        mRequest.downloadInfo.status = Downloads.Status.STATUS_RUNNING;

        try {
            mDownloader.download(mRequest);
        } catch (DownloadException e) {
            e.printStackTrace();
        }

        if (mService.lastStartId == mStartId) {
            mService.stopSelfResult(mStartId);
        }
    }

}
