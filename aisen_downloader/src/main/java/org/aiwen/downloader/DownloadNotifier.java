package org.aiwen.downloader;

import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import org.aisen.downloader.R;
import org.aiwen.downloader.utils.Constants;
import org.aiwen.downloader.utils.LongSparseLongArray;

import java.text.NumberFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by wangdan on 16/12/26.
 */
class DownloadNotifier {

    private static final String TAG = Constants.TAG +  "_Notifier";

    private final int TYPE_ACTIVE = 1;
    private final int TYPE_WAITING = 2;
    private final int TYPE_COMPLETE = 3;

    /**
     * Currently active notifications, mapped from clustering tag to timestamp
     * when first shown.
     *
     * @see #buildNotificationTag(DownloadInfo)
     */
    private final HashMap<String, Long> mActiveNotifs = Maps.newHashMap();

    /**
     * Current speed of active downloads, mapped from {@link DownloadInfo#request#id}
     * to speed in bytes per second.
     */
    private final LongSparseLongArray mDownloadSpeed = new LongSparseLongArray();

    /**
     * Last time speed was reproted, mapped from {@link DownloadInfo#request#id} to
     * {@link SystemClock#elapsedRealtime()}.
     */
    private final LongSparseLongArray mDownloadTouch = new LongSparseLongArray();

    private final NotificationManager mNotifManager;

    public DownloadNotifier(Context context) {
        mNotifManager = (NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE);
    }

    public void cancelAll() {
        mNotifManager.cancelAll();
    }

    /**
     * Notify the current speed of an active download, used for calculating
     * estimated remaining time.
     */
    public void notifyDownloadSpeed(long id, long bytesPerSecond) {
        DLogger.v(TAG, "notifyDownloadSpeed(id = %s, bytesPerSecond = %s)", id + "", bytesPerSecond + "");

        synchronized (mDownloadSpeed) {
            if (bytesPerSecond != 0) {
                mDownloadSpeed.put(id, bytesPerSecond);
                mDownloadTouch.put(id, SystemClock.elapsedRealtime());
            } else {
                mDownloadSpeed.delete(id);
                mDownloadTouch.delete(id);
            }
        }
    }

    /**
     * Update {@link NotificationManager} to reflect the given set of
     * {@link DownloadInfo}, adding, collapsing, and removing as needed.
     */
    public void updateWith(Collection<DownloadInfo> downloads) {
        synchronized (mActiveNotifs) {
            updateWithLocked(downloads);
        }
    }

    private void updateWithLocked(Collection<DownloadInfo> downloads) {
        Hawk hawk = Hawk.getInstance();
        if (hawk == null) {
            return;
        }
        Context context = hawk.getContext();

        final Resources res = context.getResources();

        // Cluster downloads together
        final Multimap<String, DownloadInfo> clustered = ArrayListMultimap.create();
        for (DownloadInfo info : downloads) {
            final String tag = buildNotificationTag(info);
            if (tag != null) {
                DLogger.d(TAG, "buildNotificationTag, Download[%s], status[%d] , tag[%s]", info.title, info.status, tag + "");

                clustered.put(tag, info);
            }
        }

        // Build notification for each cluster
        for (String tag : clustered.keySet()) {
            final int type = getNotificationTagType(tag);
            final Collection<DownloadInfo> cluster = clustered.get(tag);

            final NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
            builder.setColor(res.getColor(
                    R.color.system_notification_accent_color));

            // Use time when cluster was first shown to avoid shuffling
            final long firstShown;
            if (mActiveNotifs.containsKey(tag)) {
                firstShown = mActiveNotifs.get(tag);
            } else {
                firstShown = System.currentTimeMillis();
                mActiveNotifs.put(tag, firstShown);
            }
            builder.setWhen(firstShown);

            // Show relevant icon
            if (type == TYPE_ACTIVE) {
                builder.setSmallIcon(android.R.drawable.stat_sys_download);
            } else if (type == TYPE_WAITING) {
                builder.setSmallIcon(android.R.drawable.stat_sys_warning);
            } else if (type == TYPE_COMPLETE) {
                builder.setSmallIcon(android.R.drawable.stat_sys_download_done);
            }

            // Build action intents
            if (type == TYPE_ACTIVE || type == TYPE_WAITING) {
                // build a synthetic uri for intent identification purposes
                final Uri uri = new Uri.Builder().scheme("active-dl").appendPath(tag).build();
                final Intent intent = new Intent(Constants.ACTION_LIST,
                        uri, context, DownloadReceiver.class);
                intent.putExtra(DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS,
                        getDownloadIds(cluster));
                builder.setContentIntent(PendingIntent.getBroadcast(context,
                        0, intent, PendingIntent.FLAG_UPDATE_CURRENT));
                builder.setOngoing(true);

            } else if (type == TYPE_COMPLETE) {
                final DownloadInfo info = cluster.iterator().next();
                final Uri uri = ContentUris.withAppendedId(
                        Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, info.request.id);
                builder.setAutoCancel(true);

                final String action;
                if (Downloads.Status.isStatusError(info.status)) {
                    action = Constants.ACTION_LIST;
                } else {
//                    if (info.destination != Downloads.Impl.DESTINATION_SYSTEMCACHE_PARTITION) {
//                        action = Constants.ACTION_OPEN;
//                    } else {
                        action = Constants.ACTION_LIST;
//                    }
                }

                final Intent intent = new Intent(action, uri, context, DownloadReceiver.class);
                intent.putExtra(DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS,
                        getDownloadIds(cluster));
                builder.setContentIntent(PendingIntent.getBroadcast(context,
                        0, intent, PendingIntent.FLAG_UPDATE_CURRENT));

                final Intent hideIntent = new Intent(Constants.ACTION_HIDE,
                        uri, context, DownloadReceiver.class);
                builder.setDeleteIntent(PendingIntent.getBroadcast(context, (int) info.request.id, hideIntent, 0));
            }

            // Calculate and show progress
            String remainingText = null;
            String percentText = null;
            if (type == TYPE_ACTIVE) {
                long current = 0;
                long total = 0;
                long speed = 0;
                synchronized (mDownloadSpeed) {
                    for (DownloadInfo info : cluster) {
                        if (info.fileBytes != -1) {
                            current += info.rangeBytes;
                            total += info.fileBytes;
                            speed += mDownloadSpeed.get(info.request.id);
                        }
                    }
                }

                if (total > 0) {
                    percentText =
                            NumberFormat.getPercentInstance().format((double) current / total);

                    if (speed > 0) {
                        final long remainingMillis = ((total - current) * 1000) / (speed * 1000);
                        remainingText = res.getString(R.string.download_remaining, formatDuration(remainingMillis, context.getResources()));
                    }

                    final int percent = (int) ((current * 100) / total);
                    builder.setProgress(100, percent, false);
                } else {
                    builder.setProgress(100, 0, true);
                }
            }

            // Build titles and description
            final Notification notif;
            if (cluster.size() == 1) {
                final DownloadInfo info = cluster.iterator().next();

                builder.setContentTitle(getDownloadTitle(res, info));

                if (type == TYPE_ACTIVE) {
                    if (!TextUtils.isEmpty(info.destination)) {
                        builder.setContentText(info.destination);
                    } else {
                        builder.setContentText(remainingText);
                    }
                    builder.setContentInfo(percentText);

                } else if (type == TYPE_WAITING) {
                    builder.setContentText(
                            res.getString(R.string.notification_need_wifi_for_size));

                } else if (type == TYPE_COMPLETE) {
                    if (Downloads.Status.isStatusError(info.status)) {
                        builder.setContentText(res.getText(R.string.notification_download_failed));
                    } else if (Downloads.Status.isStatusSuccess(info.status)) {
                        builder.setContentText(
                                res.getText(R.string.notification_download_complete));
                    }
                }

                notif = builder.build();

            } else {
                final NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle(builder);

                for (DownloadInfo info : cluster) {
                    inboxStyle.addLine(getDownloadTitle(res, info));
                }

                if (type == TYPE_ACTIVE) {
                    builder.setContentTitle(res.getQuantityString(
                            R.plurals.notif_summary_active, cluster.size(), cluster.size()));
                    builder.setContentText(remainingText);
                    builder.setContentInfo(percentText);
                    inboxStyle.setSummaryText(remainingText);

                } else if (type == TYPE_WAITING) {
                    builder.setContentTitle(res.getQuantityString(
                            R.plurals.notif_summary_waiting, cluster.size(), cluster.size()));
                    builder.setContentText(
                            res.getString(R.string.notification_need_wifi_for_size));
                    inboxStyle.setSummaryText(
                            res.getString(R.string.notification_need_wifi_for_size));
                }

                notif = inboxStyle.build();
            }

            mNotifManager.notify(tag, 0, notif);
        }

        // Remove stale tags that weren't renewed
        final Iterator<String> it = mActiveNotifs.keySet().iterator();
        while (it.hasNext()) {
            final String tag = it.next();
            if (!clustered.containsKey(tag)) {
                mNotifManager.cancel(tag, 0);
                it.remove();
            }
        }
    }

    private static CharSequence getDownloadTitle(Resources res, DownloadInfo info) {
        if (!TextUtils.isEmpty(info.title)) {
            return info.title;
        } else {
            return res.getString(R.string.download_unknown_title);
        }
    }

    private long[] getDownloadIds(Collection<DownloadInfo> infos) {
        final long[] ids = new long[infos.size()];
        int i = 0;
        for (DownloadInfo info : infos) {
            ids[i++] = info.request.id;
        }
        return ids;
    }


    private String buildNotificationTag(DownloadInfo info) {
        if (info.status == Downloads.Status.STATUS_QUEUED_FOR_WIFI) {
            return TYPE_WAITING + ":" + "org.aiwen.downloader";
        } else if (isActiveAndVisible(info)) {
            return TYPE_ACTIVE + ":" + "org.aiwen.downloader";
        } else if (isCompleteAndVisible(info)) {
            // Complete downloads always have unique notifs
            return TYPE_COMPLETE + ":" + info.request.id;
        } else {
            return null;
        }
    }

    private static int getNotificationTagType(String tag) {
        return Integer.parseInt(tag.substring(0, tag.indexOf(':')));
    }

    private static boolean isActiveAndVisible(DownloadInfo download) {
        return download.status == Downloads.Status.STATUS_RUNNING &&
                (download.visibility == Request.VISIBILITY_VISIBLE
                        || download.visibility == Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
    }

    private static boolean isCompleteAndVisible(DownloadInfo download) {
        return Downloads.Status.isStatusCompleted(download.status) &&
                (download.visibility == Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                        || download.visibility == Request.VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION);
    }

    public static CharSequence formatDuration(long millis, Resources res) {
//        final Resources res = Resources.getSystem();
        if (millis >= DateUtils.HOUR_IN_MILLIS) {
            final int hours = (int) ((millis + 1800000) / DateUtils.HOUR_IN_MILLIS);
            return res.getQuantityString(
                    R.plurals.duration_hours, hours, hours);
        } else if (millis >= DateUtils.MINUTE_IN_MILLIS) {
            final int minutes = (int) ((millis + 30000) / DateUtils.MINUTE_IN_MILLIS);
            return res.getQuantityString(
                    R.plurals.duration_minutes, minutes, minutes);
        } else {
            final int seconds = (int) ((millis + 500) / DateUtils.SECOND_IN_MILLIS);
            return res.getQuantityString(
                    R.plurals.duration_seconds, seconds, seconds);
        }
    }

}
