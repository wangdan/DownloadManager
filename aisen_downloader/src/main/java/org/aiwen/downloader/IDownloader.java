package org.aiwen.downloader;

import org.aiwen.downloader.utils.Constants;

import java.io.OutputStream;

/**
 * Created by 王dan on 2016/12/19.
 */

public interface IDownloader {

    String TAG = Constants.TAG + "_Downloader";

    void download(Request request) throws DownloadException;

}
