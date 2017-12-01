package com.chattree.chattree.db;

import android.arch.persistence.room.*;

import java.util.List;

@Dao
public interface ThreadDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Thread> threads);

    @Query("SELECT * FROM t_thread")
    List<Thread> findAll();

    @Delete
    void deleteAll(List<Thread> threads);
}
