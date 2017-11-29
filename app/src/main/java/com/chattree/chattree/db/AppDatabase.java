package com.chattree.chattree.db;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;
import android.content.Context;

@Database(entities = {
        User.class,
        Conversation.class,
        ConversationUser.class,
        Thread.class,
        Message.class
}, version = 1)
@TypeConverters({ Converters.class })
public abstract class AppDatabase extends RoomDatabase {
    public abstract UserDao userDao();

    public abstract ConversationDao conversationDao();

    private static AppDatabase instance = null;

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).build();
//            instance = Room.databaseBuilder(context, AppDatabase.class, "chattree-database").build();
        }
        return instance;
    }
}
