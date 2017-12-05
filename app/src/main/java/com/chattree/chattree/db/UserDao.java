package com.chattree.chattree.db;

import android.arch.persistence.room.*;

import java.util.List;

@Dao
public interface UserDao {
    @Query("SELECT * FROM t_user")
    List<User> findAll();

    @Query("SELECT * FROM t_user WHERE id IN (:userIds)")
    List<User> loadAllByIds(int[] userIds);

    @Query("SELECT * FROM t_user WHERE firstname LIKE :first AND "
            + "lastname LIKE :last LIMIT 1")
    User findByName(String first, String last);

    @Query("SELECT * FROM t_user WHERE id = :id")
    User findById(int id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(User... users);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<User> users);

    @Delete
    void delete(User user);

    @Delete
    void deleteAll(List<User> users);
}
