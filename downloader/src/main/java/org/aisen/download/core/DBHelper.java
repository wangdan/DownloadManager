package org.aisen.download.core;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.aisen.download.utils.Constants;

/**
 * Created by wangdan on 16/7/30.
 */
public class DBHelper extends SQLiteOpenHelper {

    private static final int DB_VERSION = 1;

    private static final String DB_NAME = "aisen_download.db";

    private static final String DB_TABLE = "aisen_downloads";

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
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
                    ");");
        } catch (SQLException ex) {
            Log.e(Constants.TAG, "couldn't create table in downloads database");
            throw ex;
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public Cursor query(String key) {
        String selection = String.format(" %s = ? ", Downloads.Impl.COLUMN_KEY);
        String[] selectionArgs = new String[]{ key };

        return getReadableDatabase().query(DB_TABLE, columns(), selection, selectionArgs, null, null, null);
    }

    public long insert(final ContentValues values) {
        return getWritableDatabase().insert(DB_TABLE, null, values);
    }

    public final long update(long id, ContentValues values, String where, String[] selectionArgs) {
        return -1;
    }

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
        };
    }

}
