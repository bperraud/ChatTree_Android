package com.chattree.chattree.db;

import android.arch.persistence.room.*;

import java.util.List;

@Dao
public interface ThreadDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(Thread... threads);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Thread> threads);

    @Query("UPDATE t_thread SET title = :title WHERE id = :threadId")
    void updateTitleById(String title, int threadId);

    @Query("SELECT * FROM t_thread")
    List<Thread> findAll();

    @Query("SELECT * FROM t_thread WHERE id = :threadId")
    Thread findById(int threadId);

    @Query("SELECT * FROM t_thread WHERE fk_conversation = :convId")
    List<Thread> findByConvId(int convId);

    /**
     * @param convId       The conversation we want the threads of
     * @param threadOffset The thread id from which we recover the next ones. Exclusive.
     * @return The list of the threads
     */
    @Query("SELECT * FROM t_thread WHERE fk_conversation = :convId AND id > :threadOffset")
    List<Thread> findByConvIdAndOffset(int convId, int threadOffset);

    @Delete
    void deleteAll(List<Thread> threads);
}
