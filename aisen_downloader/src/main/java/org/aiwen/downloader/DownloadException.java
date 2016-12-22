package org.aiwen.downloader;

import static org.aiwen.downloader.Downloads.Status.STATUS_FILE_ERROR;

/**
 * Created by 王dan on 2016/12/19.
 */

public class DownloadException extends Exception {

    private static final long serialVersionUID = -5527179397794200799L;

    private final int status;

    public DownloadException() {
        status = STATUS_FILE_ERROR;
    }

    public DownloadException(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return Downloads.Status.statusToString(status);
    }

    @Override
    public String toString() {
        return super.toString() + String.format(": status(%d), error(%s)", status, getError());
    }

}
