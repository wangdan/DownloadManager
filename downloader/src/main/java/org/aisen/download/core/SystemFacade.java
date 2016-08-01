package org.aisen.download.core;

import android.net.NetworkInfo;

/**
 * Created by wangdan on 16/7/30.
 */
public interface SystemFacade {

    /**
     * @see System#currentTimeMillis()
     */
    long currentTimeMillis();

    /**
     * @return Currently active network, or null if there's no active
     *         connection.
     */
    NetworkInfo getActiveNetworkInfo();

//    boolean isActiveNetworkMetered();

    /**
     * @see android.telephony.TelephonyManager#isNetworkRoaming
     */
    boolean isNetworkRoaming();

    /**
     * @return maximum size, in bytes, of downloads that may go over a mobile connection; or null if
     * there's no limit
     */
    Long getMaxBytesOverMobile();

    /**
     * @return recommended maximum size, in bytes, of downloads that may go over a mobile
     * connection; or null if there's no recommended limit.  The user will have the option to bypass
     * this limit.
     */
    Long getRecommendedMaxBytesOverMobile();

}
