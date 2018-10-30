package com.atomtex.modbusapp;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {

    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("com.atomtex.modbusapp", appContext.getPackageName());

    }
    @Test
    public void dataBaseTest(){
       /* Log.i("myTag","Start");

        Context appContext = InstrumentationRegistry.getTargetContext();
        FeedReaderDbHelper mDbHelper = new FeedReaderDbHelper(appContext);


        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.execSQL("DELETE FROM " + "data");
        db.beginTransaction();
        long time = System.currentTimeMillis();
        Log.i("myTag","Start time " + (System.currentTimeMillis()-time));
        SQLiteStatement statement = db.compileStatement("INSERT INTO "+"data"+" VALUES(?,?);");
        for (int i = 0; i < 10000; i++) {
            statement.bindLong(1,i);
            statement.bindDouble(2,i);
            statement.executeInsert();
        }
        db.setTransactionSuccessful();
        db.endTransaction();
        Log.i("myTag","Finish write time " + (System.currentTimeMillis()-time));
        time = System.currentTimeMillis();
        db.beginTransaction();
        db = mDbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT value FROM data ",null);
        List<Double> itemIds = new ArrayList<>();
        while(cursor.moveToNext()) {
            Double itemId = cursor.getDouble(
                    cursor.getColumnIndex("value"));
            itemIds.add(itemId);
        }
        cursor.close();

        Log.i("myTag","list size " + itemIds.size());
        db.setTransactionSuccessful();
        db.endTransaction();
        Log.i("myTag","Finish read time " + (System.currentTimeMillis()-time));
    }
    public class FeedReaderDbHelper extends SQLiteOpenHelper {

        // If you change the database schema, you must increment the database version.
        public static final int DATABASE_VERSION = 1;
        public static final String DATABASE_NAME = "mydb.db";

        private static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + "data" + " (" +
                       "_id" + " INTEGER PRIMARY KEY," +
                        "value" + " REAL)";

        private static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + "data";


        public FeedReaderDbHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }
        public void onCreate(SQLiteDatabase db) {
            String db1 = "drop table IF EXISTS " + "data";
            db.execSQL(db1);
            db.execSQL(SQL_CREATE_ENTRIES);
        }
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // This database is only a cache for online data, so its upgrade policy is
            // to simply to discard the data and start over
            db.execSQL(SQL_DELETE_ENTRIES);
            onCreate(db);
        }
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            onUpgrade(db, oldVersion, newVersion);
        }*/
    }
}

