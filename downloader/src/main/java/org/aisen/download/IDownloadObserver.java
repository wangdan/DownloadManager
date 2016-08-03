package org.aisen.download;

import android.net.Uri;

/**
 * Created by wangdan on 16/6/15.
 */
public interface IDownloadObserver {

    Uri downloadURI();

    Uri downloadFileURI();

    void onPublish(DownloadMsg downloadMsg);

}
