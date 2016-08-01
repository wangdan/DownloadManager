package org.aisen.download.manager;

/**
 * Created by wangdan on 16/7/30.
 */

import android.content.ContentValues;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.text.TextUtils;

import org.aisen.download.downloads.Downloads;
import org.aisen.download.utils.Utils;

/**
 * This class contains all the information necessary to request a new download. The URI is the
 * only required parameter.
 *
 */
public class Request {
    /**
     * Bit flag for {@link #setAllowedNetworkTypes} corresponding to
     * {@link ConnectivityManager#TYPE_MOBILE}.
     */
    public static final int NETWORK_MOBILE = 1 << 0;

    /**
     * Bit flag for {@link #setAllowedNetworkTypes} corresponding to
     * {@link ConnectivityManager#TYPE_WIFI}.
     */
    public static final int NETWORK_WIFI = 1 << 1;

    /**
     * Bit flag for {@link #setAllowedNetworkTypes} corresponding to
     * {@link ConnectivityManager#TYPE_BLUETOOTH}.
     * @hide
     */
    public static final int NETWORK_BLUETOOTH = 1 << 2;

    final String mKey;
    final Uri mUri;
    final Uri mFileUri;
    private CharSequence mTitle;
    private CharSequence mDescription;
    private int mAllowedNetworkTypes = ~0; // default to all network types allowed
    private boolean mRoamingAllowed = true;
    private boolean mIsVisibleInDownloadsUi = true;

    /**
     * This download is visible but only shows in the notifications
     * while it's in progress.
     */
    public static final int VISIBILITY_VISIBLE = 0;

    /**
     * This download is visible and shows in the notifications while
     * in progress and after completion.
     */
    public static final int VISIBILITY_VISIBLE_NOTIFY_COMPLETED = 1;

    /**
     * This download doesn't show in the UI or in the notifications.
     */
    public static final int VISIBILITY_HIDDEN = 2;

    /**
     * This download shows in the notifications after completion ONLY.
     */
    public static final int VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION = 3;

    /** can take any of the following values: {@link #VISIBILITY_HIDDEN}
     * {@link #VISIBILITY_VISIBLE_NOTIFY_COMPLETED}, {@link #VISIBILITY_VISIBLE},
     * {@link #VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION}
     */
    private int mNotificationVisibility = VISIBILITY_VISIBLE;

    public Request(Uri uri, Uri fileUri) {
        this(null, uri, fileUri);
    }

    /**
     * @param uri the HTTP or HTTPS URI to download.
     */
    private Request(String key, Uri uri, Uri fileUri) {
        if (uri == null || fileUri == null) {
            throw new NullPointerException();
        }
        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equals("http") && !scheme.equals("https"))) {
            throw new IllegalArgumentException("Can only download HTTP/HTTPS URIs: " + uri);
        }
        if (TextUtils.isEmpty(key)) {
            key = Utils.generateMD5(uri.toString() + fileUri.toString());
        }
        mKey = key;
        mUri = uri;
        mFileUri = fileUri;
    }

    /**
     * Set the title of this download, to be displayed in notifications (if enabled).  If no
     * title is given, a default one will be assigned based on the download filename, once the
     * download starts.
     * @return this object
     */
    public Request setTitle(CharSequence title) {
        mTitle = title;
        return this;
    }

    /**
     * Set a description of this download, to be displayed in notifications (if enabled)
     * @return this object
     */
    public Request setDescription(CharSequence description) {
        mDescription = description;
        return this;
    }

    /**
     * Control whether a system notification is posted by the download manager while this
     * download is running. If enabled, the download manager posts notifications about downloads
     * through the system {@link android.app.NotificationManager}. By default, a notification is
     * shown.
     *
     * If set to false, this requires the permission
     * android.permission.DOWNLOAD_WITHOUT_NOTIFICATION.
     *
     * @param show whether the download manager should show a notification for this download.
     * @return this object
     * @deprecated use {@link #setNotificationVisibility(int)}
     */
    @Deprecated
    public Request setShowRunningNotification(boolean show) {
        return (show) ? setNotificationVisibility(VISIBILITY_VISIBLE) :
                setNotificationVisibility(VISIBILITY_HIDDEN);
    }

    /**
     * Control whether a system notification is posted by the download manager while this
     * download is running or when it is completed.
     * If enabled, the download manager posts notifications about downloads
     * through the system {@link android.app.NotificationManager}.
     * By default, a notification is shown only when the download is in progress.
     *<p>
     * It can take the following values: {@link #VISIBILITY_HIDDEN},
     * {@link #VISIBILITY_VISIBLE},
     * {@link #VISIBILITY_VISIBLE_NOTIFY_COMPLETED}.
     *<p>
     * If set to {@link #VISIBILITY_HIDDEN}, this requires the permission
     * android.permission.DOWNLOAD_WITHOUT_NOTIFICATION.
     *
     * @param visibility the visibility setting value
     * @return this object
     */
    public Request setNotificationVisibility(int visibility) {
        mNotificationVisibility = visibility;
        return this;
    }

    /**
     * Restrict the types of networks over which this download may proceed.
     * By default, all network types are allowed.
     *
     * @param flags any combination of the NETWORK_* bit flags.
     * @return this object
     */
    public Request setAllowedNetworkTypes(int flags) {
        mAllowedNetworkTypes = flags;
        return this;
    }

    /**
     * Set whether this download may proceed over a roaming connection.  By default, roaming is
     * allowed.
     * @param allowed whether to allow a roaming connection to be used
     * @return this object
     */
    public Request setAllowedOverRoaming(boolean allowed) {
        mRoamingAllowed = allowed;
        return this;
    }

    /**
     * Set whether this download should be displayed in the system's Downloads UI. True by
     * default.
     * @param isVisible whether to display this download in the Downloads UI
     * @return this object
     */
    public Request setVisibleInDownloadsUi(boolean isVisible) {
        mIsVisibleInDownloadsUi = isVisible;
        return this;
    }

    /**
     * @return ContentValues to be passed to DownloadProvider.insert()
     */
    ContentValues toContentValues() {
        ContentValues values = new ContentValues();

        values.put(Downloads.Impl.COLUMN_KEY, mKey);
        values.put(Downloads.Impl.COLUMN_URI, mUri.toString());
        values.put(Downloads.Impl._DATA, mFileUri.toString());
        putIfNonNull(values, Downloads.Impl.COLUMN_TITLE, mTitle);
        putIfNonNull(values, Downloads.Impl.COLUMN_DESCRIPTION, mDescription);
        values.put(Downloads.Impl.COLUMN_VISIBILITY, mNotificationVisibility);
        values.put(Downloads.Impl.COLUMN_ALLOWED_NETWORK_TYPES, mAllowedNetworkTypes);
        values.put(Downloads.Impl.COLUMN_ALLOW_ROAMING, mRoamingAllowed);
        values.put(Downloads.Impl.COLUMN_IS_VISIBLE_IN_DOWNLOADS_UI, mIsVisibleInDownloadsUi);

        return values;
    }

    private void putIfNonNull(ContentValues contentValues, String key, Object value) {
        if (value != null) {
            contentValues.put(key, value.toString());
        }
    }

    @Override
    public String toString() {
        return String.format("URI[%s], FilePath[%s]", mUri.toString(), mFileUri.toString());
    }
}
