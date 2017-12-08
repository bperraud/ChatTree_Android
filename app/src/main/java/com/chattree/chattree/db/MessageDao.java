package com.chattree.chattree.db;

import android.arch.persistence.room.*;

import java.util.Date;
import java.util.List;

@Dao
public interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Message> messages);

    @Query("SELECT * FROM t_message")
    List<Message> findAll();

    /**
     * @param threadId  The thread we want the messages of
     * @param msgOffset The message id from which we recover the next ones. Exclusive.
     * @return
     */
    @Query("SELECT " +
           "t_message.id m_id, " +
           "t_message.fk_author m_fk_author, " +
           "t_message.creation_date m_creation_date, " +
           "t_message.content m_content, " +
           "t_message.fk_thread_parent m_fk_thread_parent, " +
           "t_user.id u_id, " +
           "t_user.login u_login, " +
           "t_user.email u_email, " +
           "t_user.firstname u_firstname, " +
           "t_user.lastname u_lastname, " +
           "t_user.profile_picture u_pp " +
           "FROM t_message " +
           "INNER JOIN t_user ON t_user.id = t_message.fk_author " +
           "WHERE t_message.fk_thread_parent = :threadId " +
           "AND t_message.id > :msgOffset")
    List<CustomMessageWithUser> getMessageWithUserByThreadIdAndOffset(int threadId, int msgOffset);

    @Query("SELECT " +
           "t_message.id m_id, " +
           "t_message.fk_author m_fk_author, " +
           "t_message.creation_date m_creation_date, " +
           "t_message.content m_content, " +
           "t_message.fk_thread_parent m_fk_thread_parent, " +
           "t_user.id u_id, " +
           "t_user.login u_login, " +
           "t_user.email u_email, " +
           "t_user.firstname u_firstname, " +
           "t_user.lastname u_lastname, " +
           "t_user.profile_picture u_pp " +
           "FROM t_message " +
           "INNER JOIN t_user ON t_user.id = t_message.fk_author " +
           "WHERE t_message.fk_thread_parent = :threadId")
    List<CustomMessageWithUser> getMessageWithUserByThreadId(int threadId);

    @Query("SELECT " +
           "t_message.id m_id, " +
           "t_message.fk_author m_fk_author, " +
           "t_message.creation_date m_creation_date, " +
           "t_message.content m_content, " +
           "t_message.fk_thread_parent m_fk_thread_parent, " +
           "t_user.id u_id, " +
           "t_user.login u_login, " +
           "t_user.email u_email, " +
           "t_user.firstname u_firstname, " +
           "t_user.lastname u_lastname, " +
           "t_user.profile_picture u_pp " +
           "FROM t_message " +
           "INNER JOIN t_user ON t_user.id = t_message.fk_author " +
           "WHERE t_message.id = :msgId")
    CustomMessageWithUser findById(int msgId);

    class CustomMessageWithUser {
        public int    m_id;
        public int    m_fk_author;
        public Date   m_creation_date;
        public String m_content;
        public int    m_fk_thread_parent;
        public int    u_id;
        public String u_login;
        public String u_email;
        public String u_firstname;
        public String u_lastname;
        public String u_pp;
    }

    @Delete
    void deleteAll(List<Message> messages);
}
