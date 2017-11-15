package com.chattree.chattree.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbHelper extends SQLiteOpenHelper{
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "ChatTree.db";

    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    public void onCreate(SQLiteDatabase db) {
        executeBatchSql(db, DbContract.SQL_CREATE_ENTRIES);
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        executeBatchSql(db, DbContract.SQL_DELETE_ENTRIES);
        onCreate(db);
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public void executeBatchSql(SQLiteDatabase db, String sql){
        // use something like StringTokenizer to separate sql statements
        String[] statements = sql.split(";");
        for(String statement : statements){
            db.execSQL(statement);
        }
    }

    public Cursor getConversations(){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT " +
                    "t_conversation._id c_id, " +
                    "t_conversation.title c_title, " +
                    "t_conversation.picture c_picture, " +
                    "t_user._id u_id, " +
                    "t_user.login u_login, " +
                    "t_user.email u_email, " +
                    "t_user.firstname u_firstname, " +
                    "t_user.lastname u_lastname, " +
                    "t_user.profile_picture u_pp " +
                    "FROM t_conversation " +
                    "INNER JOIN t_conversation_user ON t_conversation._id = t_conversation_user.fk_conversation " +
                    "INNER JOIN t_user ON t_user._id = t_conversation_user.fk_member",
                null
        );
        c.moveToFirst();
        return c;
    }

    public void insertConversation(String title){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues val = new ContentValues();
        val.put(DbContract.tConversation.COLUMN_NAME_TITLE, title);
        long convId = db.insertOrThrow(DbContract.tConversation.TABLE_NAME,  null, val);

        val = new ContentValues();
        val.put(DbContract.tThread.COLUMN_NAME_FK_CONVERSATION, convId);
        long threadId = db.insertOrThrow(DbContract.tThread.TABLE_NAME, null, val);

    }

}
