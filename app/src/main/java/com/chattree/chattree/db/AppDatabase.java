package com.chattree.chattree.db;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;
import android.content.Context;

@Database(entities = {
        User.class
}, version = 1)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    public abstract UserDao userDao();

    private static AppDatabase instance = null;

    public static AppDatabase getInstance(Context context){
        if(instance == null){
            instance = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).build();
        }
        return instance;
    }
}
