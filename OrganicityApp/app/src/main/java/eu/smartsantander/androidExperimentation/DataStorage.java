package eu.smartsantander.androidExperimentation;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.util.Pair;

import java.util.HashSet;
import java.util.Set;

public class DataStorage extends SQLiteOpenHelper {

    private static final String TAG = "DataStorage";
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "experimentalMessagesDB.db";
    private static final String TABLE_MESSAGES = "messages";

    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_MESSAGE = "message";
    private static DataStorage sInstance = null;


    public static DataStorage getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new DataStorage(context);
        }
        return sInstance;
    }


    public DataStorage(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION, new DatabaseErrorHandler() {
            @Override
            public void onCorruption(SQLiteDatabase dbObj) {
                Log.e(TAG, "onCorruption");
            }
        });
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_MESSAGES_TABLE = "CREATE TABLE " + TABLE_MESSAGES + "(" + COLUMN_ID + " INTEGER PRIMARY KEY," + COLUMN_MESSAGE + " TEXT" + ")";
        db.execSQL(CREATE_MESSAGES_TABLE);

    }

    @Override
    public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
        // TODO Auto-generated method stub
    }

    public synchronized void addMessage(String message) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_MESSAGE, message);
        SQLiteDatabase db = sInstance.getWritableDatabase();
        long id = db.insert(TABLE_MESSAGES, null, values);
        Log.d(TAG, "Stored message with id " + id);
    }

    public synchronized void deleteMessage(long id) {
        Log.d(TAG, "Delete Message with id " + id);

        SQLiteDatabase db = sInstance.getWritableDatabase();
        db.delete(TABLE_MESSAGES, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public synchronized Pair<Long, String> getMessage() {
        String query = "Select * FROM " + TABLE_MESSAGES + " WHERE rowid= (SELECT MIN(rowid) FROM " + TABLE_MESSAGES + ")";
        SQLiteDatabase db = sInstance.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        Pair<Long, String> idMessage = new Pair<>(0L, "");
        if (cursor.moveToFirst()) {
            cursor.moveToFirst();
            idMessage = new Pair<>(Long.parseLong(cursor.getString(0)), cursor.getString(1));
            cursor.close();
        } else {
            idMessage = new Pair<>(0L, "");
        }
        return idMessage;
    }

    public synchronized Set<Pair<Long, String>> getMessages() {
        String query = "Select * FROM " + TABLE_MESSAGES;
        SQLiteDatabase db = sInstance.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        Set<Pair<Long, String>> elems = new HashSet<>();
        do {
            Pair<Long, String> idMessage = new Pair<>(0L, "");
            if (cursor.moveToFirst()) {
                cursor.moveToFirst();
                elems.add(new Pair<>(Long.parseLong(cursor.getString(0)), cursor.getString(1)));

            } else {
                idMessage = new Pair<>(0L, "");
            }
        } while (cursor.moveToNext());
        cursor.close();
        return elems;
    }

    public synchronized Long size() {
        Long size = 0L;
        final String query = String.format("Select COUNT(*) FROM %s", TABLE_MESSAGES);
        final SQLiteDatabase db = sInstance.getReadableDatabase();
        final Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            cursor.moveToFirst();
            size = Long.parseLong(cursor.getString(0));
            cursor.close();
        }
        return size;
    }

}