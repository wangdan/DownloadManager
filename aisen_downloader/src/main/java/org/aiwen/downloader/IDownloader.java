package org.aiwen.downloader;

import java.io.OutputStream;

/**
 * Created by 王dan on 2016/12/19.
 */

public interface IDownloader {

    void download(Request request) throws DownloadException;

}
