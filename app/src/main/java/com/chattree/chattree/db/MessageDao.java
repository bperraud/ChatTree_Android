package com.chattree.chattree.db;

import android.arch.persistence.room.*;

import java.util.List;

@Dao
public interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Message> messages);

    @Query("SELECT * FROM t_message")
    List<Message> findAll();

    @Delete
    void deleteAll(List<Message> messages);
}
