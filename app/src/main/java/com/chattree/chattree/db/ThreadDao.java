package com.chattree.chattree.db;

import android.arch.persistence.room.*;

import java.util.List;

@Dao
public interface ThreadDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Thread> threads);

    @Query("SELECT * FROM t_thread")
    List<Thread> findAll();

    @Query("SELECT * FROM t_thread WHERE id = :threadId")
    Thread findById(int threadId);

    @Query("SELECT * FROM t_thread WHERE fk_conversation = :convId")
    List<Thread> findByConvId(int convId);

    @Delete
    void deleteAll(List<Thread> threads);
}
