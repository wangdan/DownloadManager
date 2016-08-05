package org.aisen.download.core;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import org.aisen.download.utils.Constants;

import java.util.Map;

/**
 * Created by wangdan on 16/7/30.
 */
public class DBHelper extends SQLiteOpenHelper {

    private static final int DB_VERSION = 1;

    private static final String DB_NAME = "aisen_download.db";

    private static final String DB_TABLE = "aisen_downloads";

    private final SystemFacade mSystemFacade;

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);

        mSystemFacade = new RealSystemFacade(context);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        onUpgrade(db, 0, DB_VERSION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion == 1) {
            createDownloadInfoTable(db);

            createHeadersTable(db);
        }
    }

    private void createDownloadInfoTable(SQLiteDatabase db) {
        try {
            db.execSQL("DROP TABLE IF EXISTS " + DB_TABLE);
            db.execSQL("CREATE TABLE " + DB_TABLE + "(" +
                    Downloads.Impl._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    Downloads.Impl.COLUMN_KEY + " TEXT NOT NULL, " +
                    Downloads.Impl.COLUMN_URI + " TEXT, " +
                    Downloads.Impl._DATA + " TEXT, " +
                    Downloads.Impl.COLUMN_VISIBILITY + " INTEGER, " +
                    Downloads.Impl.COLUMN_CONTROL + " INTEGER, " +
                    Downloads.Impl.COLUMN_STATUS + " INTEGER, " +
                    Downloads.Impl.COLUMN_ERROR_MSG + " TEXT, " +
                    Downloads.Impl.COLUMN_LAST_MODIFICATION + " BIGINT, " +
                    Downloads.Impl.COLUMN_FAILED_CONNECTIONS + " INTEGER, " +
                    Downloads.Impl.COLUMN_TOTAL_BYTES + " INTEGER, " +
                    Downloads.Impl.COLUMN_CURRENT_BYTES + " INTEGER, " +
                    Downloads.Impl.COLUMN_ALLOWED_NETWORK_TYPES + " INTEGER, " +
                    Downloads.Impl.COLUMN_ALLOW_ROAMING + " BOOLEAN, " +
                    Downloads.Impl.COLUMN_BYPASS_RECOMMENDED_SIZE_LIMIT + " BOOLEAN, " +
                    Constants.ETAG + " TEXT, " +
                    Downloads.Impl.COLUMN_TITLE + " TEXT, " +
                    Downloads.Impl.COLUMN_DESCRIPTION + " TEXT " +
//                    Downloads.Impl.COLUMN_COOKIE_DATA + " TEXT, " +
//                    Downloads.Impl.COLUMN_REFERER + " TEXT " +
                    ");");
        } catch (SQLException ex) {
            ex.printStackTrace();
            Log.e(Constants.TAG, "couldn't create table in downloads database");
            throw ex;
        }
    }

    private void createHeadersTable(SQLiteDatabase db) {
        try {
            db.execSQL("DROP TABLE IF EXISTS " + Downloads.Impl.RequestHeaders.HEADERS_DB_TABLE);
            db.execSQL("CREATE TABLE " + Downloads.Impl.RequestHeaders.HEADERS_DB_TABLE + "(" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    Downloads.Impl.RequestHeaders.COLUMN_DOWNLOAD_ID + " INTEGER NOT NULL," +
                    Downloads.Impl.RequestHeaders.COLUMN_HEADER + " TEXT NOT NULL," +
                    Downloads.Impl.RequestHeaders.COLUMN_VALUE + " TEXT NOT NULL" +
                    ");");
        } catch (SQLException ex) {
            ex.printStackTrace();
            Log.e(Constants.TAG, "couldn't create table in downloads database");
            throw ex;
        }
    }

    /**
     * 根据条件查询下载
     *
     * @param selection
     * @param selectionArgs
     * @param orderBy
     * @return
     */
    public Cursor query(String selection, String[] selectionArgs, String orderBy) {
        return getReadableDatabase().query(DB_TABLE, columns(), selection, selectionArgs, null, null, orderBy);
    }

    /**
     * 根据Key查询一个下载
     *
     * @param key
     * @return
     */
    public Cursor query(String key) {
        String selection = String.format(" %s = ? ", Downloads.Impl.COLUMN_KEY);
        String[] selectionArgs = new String[]{ key };

        return getReadableDatabase().query(DB_TABLE, columns(), selection, selectionArgs, null, null, null);
    }

    /**
     * 新增一个下载
     *
     * @param values
     * @return
     */
    public long insert(final ContentValues values) {
        String key = values.getAsString(Downloads.Impl.COLUMN_KEY);

        if (TextUtils.isEmpty(key)) {
            return -1;
        }

        remove(key);

        // copy some of the input values as it
        ContentValues filteredValues = new ContentValues();
        filteredValues.put(Downloads.Impl.COLUMN_LAST_MODIFICATION, mSystemFacade.currentTimeMillis());
        copyString(Downloads.Impl.COLUMN_KEY, values, filteredValues);
        copyString(Downloads.Impl.COLUMN_URI, values, filteredValues);
        copyString(Downloads.Impl._DATA, values, filteredValues);
        copyStringWithDefault(Downloads.Impl.COLUMN_TITLE, values, filteredValues, "");
        copyStringWithDefault(Downloads.Impl.COLUMN_DESCRIPTION, values, filteredValues, "");
        copyInteger(Downloads.Impl.COLUMN_VISIBILITY, values, filteredValues);
        copyInteger(Downloads.Impl.COLUMN_ALLOWED_NETWORK_TYPES, values, filteredValues);
        copyBoolean(Downloads.Impl.COLUMN_ALLOW_ROAMING, values, filteredValues);
        copyInteger(Downloads.Impl.COLUMN_STATUS, values, filteredValues);

        long rowID = getWritableDatabase().insert(DB_TABLE, null, filteredValues);

        if (rowID != -1) {
            insertRequestHeaders(getWritableDatabase(), rowID, values);
        }

        return rowID;
    }

    /**
     * 更新一个下载
     *
     * @param key
     * @param values
     * @return
     */
    public final long update(String key, ContentValues values) {
        String whereClause = String.format(" %s = ? ", Downloads.Impl.COLUMN_KEY);;
        String[] whereArgs = new String[]{ key };

        return getWritableDatabase().update(DB_TABLE, values, whereClause, whereArgs);
    }

    /**
     * 删除一个下载
     *
     * @param key
     * @return
     */
    public final long remove(String key) {
        String whereClause = String.format(" %s = ? ", Downloads.Impl.COLUMN_KEY);;
        String[] whereArgs = new String[]{ key };

        return getWritableDatabase().delete(DB_TABLE, whereClause, whereArgs);
    }

    /**
     * Insert request headers for a download into the DB.
     */
    private void insertRequestHeaders(SQLiteDatabase db, long rowId, ContentValues values) {
        ContentValues rowValues = new ContentValues();

        rowValues.put(Downloads.Impl.RequestHeaders.COLUMN_DOWNLOAD_ID, rowId);

        for (Map.Entry<String, Object> entry : values.valueSet()) {
            String key = entry.getKey();

            if (key.startsWith(Downloads.Impl.RequestHeaders.INSERT_KEY_PREFIX)) {
                String headerLine = entry.getValue().toString();

                if (headerLine.contains(":")) {
                    String[] parts = headerLine.split(":", 2);

                    rowValues.put(Downloads.Impl.RequestHeaders.COLUMN_HEADER, parts[0].trim());
                    rowValues.put(Downloads.Impl.RequestHeaders.COLUMN_VALUE, parts[1].trim());

                    db.insert(Downloads.Impl.RequestHeaders.HEADERS_DB_TABLE, null, rowValues);
                }
            }
        }
    }

    /**
     * 根据DownloadID查询Header
     *
     * @param downloadId
     * @return
     */
    public Cursor queryRequestHeaders(long downloadId) {
        String selection = String.format(" %s = ? ", Downloads.Impl.RequestHeaders.COLUMN_DOWNLOAD_ID);
        String[] selectionArgs = new String[]{ String.valueOf(downloadId) };

        return getReadableDatabase().query(Downloads.Impl.RequestHeaders.HEADERS_DB_TABLE, headerColumns(), selection, selectionArgs, null, null, null);
    }

    /**
     * DownloadInfo的字段
     *
     * @return
     */
    private String[] columns() {
        return new String[] { Downloads.Impl._ID,
                                Downloads.Impl.COLUMN_KEY,
                                Downloads.Impl.COLUMN_URI,
                                Downloads.Impl._DATA,
                                Downloads.Impl.COLUMN_VISIBILITY,
                                Downloads.Impl.COLUMN_CONTROL,
                                Downloads.Impl.COLUMN_STATUS,
                                Downloads.Impl.COLUMN_ERROR_MSG,
                                Downloads.Impl.COLUMN_LAST_MODIFICATION,
                                Downloads.Impl.COLUMN_FAILED_CONNECTIONS,
                                Downloads.Impl.COLUMN_TOTAL_BYTES,
                                Downloads.Impl.COLUMN_CURRENT_BYTES,
                                Downloads.Impl.COLUMN_ALLOWED_NETWORK_TYPES,
                                Downloads.Impl.COLUMN_ALLOW_ROAMING,
                                Downloads.Impl.COLUMN_BYPASS_RECOMMENDED_SIZE_LIMIT,
                                Constants.ETAG,
                                Downloads.Impl.COLUMN_TITLE,
                                Downloads.Impl.COLUMN_DESCRIPTION
//                                Downloads.Impl.COLUMN_COOKIE_DATA,
//                                Downloads.Impl.COLUMN_REFERER
        };
    }

    /**
     * Header的字段
     *
     * @return
     */
    private String[] headerColumns() {
        return new String[] { Downloads.Impl.RequestHeaders.COLUMN_DOWNLOAD_ID,
                                Downloads.Impl.RequestHeaders.COLUMN_HEADER,
                                Downloads.Impl.RequestHeaders.COLUMN_VALUE
        };
    }

    private static final void copyInteger(String key, ContentValues from, ContentValues to) {
        Integer i = from.getAsInteger(key);
        if (i != null) {
            to.put(key, i);
        }
    }

    private static final void copyBoolean(String key, ContentValues from, ContentValues to) {
        Boolean b = from.getAsBoolean(key);
        if (b != null) {
            to.put(key, b);
        }
    }

    private static final void copyString(String key, ContentValues from, ContentValues to) {
        String s = from.getAsString(key);
        if (s != null) {
            to.put(key, s);
        }
    }

    private static final void copyStringWithDefault(String key, ContentValues from,
                                                    ContentValues to, String defaultValue) {
        copyString(key, from, to);
        if (!to.containsKey(key)) {
            to.put(key, defaultValue);
        }
    }

}
