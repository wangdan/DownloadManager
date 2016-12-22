package org.aiwen.downloader;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.aiwen.downloader.utils.Constants;

/**
 * Created by 王dan on 2016/12/17.
 */
class DownloadDB extends SQLiteOpenHelper {

    private static final String TAG = Constants.TAG + "_DB";

    private static final int DB_VERSION = 1;

    private static final String DB_NAME = "downloader_aisen.db";

    private static final String DB_TABLE = "downloads_aisen";

    public DownloadDB(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        onUpgrade(db, 0, DB_VERSION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion == 1) {
            createDownloadInfoTable(db);
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
                    Downloads.Impl.COLUMN_STATUS + " INTEGER, " +
                    Downloads.Impl.COLUMN_ERROR_MSG + " TEXT, " +
                    Downloads.Impl.COLUMN_LAST_MODIFICATION + " BIGINT, " +
                    Downloads.Impl.COLUMN_FAILED_CONNECTIONS + " INTEGER, " +
                    Downloads.Impl.COLUMN_TOTAL_BYTES + " INTEGER, " +
                    Downloads.Impl.COLUMN_CURRENT_BYTES + " INTEGER, " +
                    Downloads.Impl.COLUMN_ALLOWED_NETWORK_TYPES + " INTEGER, " +
                    Downloads.Impl.COLUMN_BYPASS_RECOMMENDED_SIZE_LIMIT + " BOOLEAN, " +
                    Downloads.Impl.COLUMN_TITLE + " TEXT, " +
                    Downloads.Impl.COLUMN_DESCRIPTION + " TEXT " +
                    ");");
        } catch (SQLException ex) {
            ex.printStackTrace();
            Log.e(Constants.TAG, "couldn't create table in downloads database");
            throw ex;
        }
    }

    public boolean exist(Request request) {
        String[] columns = null;
        String selection = String.format(" %s = ? ", Downloads.Impl.COLUMN_KEY);
        String[] selectionArgs = new String[]{ request.key};
        Cursor cursor = getWritableDatabase().query(DB_TABLE, columns, selection, selectionArgs, null, null, null);

        try {
            if (cursor.moveToFirst()) {

                request.set(cursor);
                DLogger.v(TAG, "exist(true), ID = %d", request.id);

                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        DLogger.v(TAG, "exist(false)");
        return false;
    }

    public void update(Request request) {
        String whereClause = String.format(" %s = ? ", Downloads.Impl.COLUMN_KEY);
        String[] whereArgs = new String[]{ request.key};
        int rowId = getWritableDatabase().update(DB_TABLE, request.getContentValues(), whereClause, whereArgs);

        DLogger.v(TAG, "rowId = %d, update(%s)", rowId, request.toString());
    }

    public void insert(Request request) {
        long rowId = getWritableDatabase().insert(DB_TABLE, Downloads.Impl.COLUMN_KEY, request.getContentValues());

        request.id = rowId;

        DLogger.v(TAG, "rowId = %s, insert(%s)", Long.toString(rowId), request.toString());
    }

}
