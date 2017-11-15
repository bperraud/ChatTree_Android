package com.chattree.chattree.db;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface ConversationDao {
    @Query("SELECT " +
            "t_conversation.id c_id, " +
            "t_conversation.title c_title, " +
            "t_conversation.picture c_picture, " +
            "t_user.id u_id, " +
            "t_user.login u_login, " +
            "t_user.email u_email, " +
            "t_user.firstname u_firstname, " +
            "t_user.lastname u_lastname, " +
            "t_user.profile_picture u_pp " +
            "FROM t_conversation " +
            "INNER JOIN t_conversation_user ON t_conversation.id = t_conversation_user.fk_conversation " +
            "INNER JOIN t_user ON t_user.id = t_conversation_user.fk_member")
    List<ConversationUser> getConversations();

    static class ConversationUser {
        public int c_id;
        public String c_title;
        public String c_picture;
        public int u_id;
        public String u_login;
        public String u_email;
        public String u_firstname;
        public String u_lastname;
        public String u_pp;
    }

    @Insert
    void insertAll(Conversation... conversations);

    @Delete
    void delete(Conversation conversation);

}
