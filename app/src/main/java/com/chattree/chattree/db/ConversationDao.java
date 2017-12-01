package com.chattree.chattree.db;

import android.arch.persistence.room.*;

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
    List<CustomConversationUser> getCustomConversationUsers();

    class CustomConversationUser {
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(Conversation... conversations);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Conversation> conversations);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertConversationUsers(ConversationUser... conversationUsers);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertConversationUsers(List<ConversationUser> conversationUsers);

    @Query("SELECT * FROM t_conversation WHERE id = :convId")
    Conversation findById(int convId);

    @Query("SELECT * FROM t_conversation")
    List<Conversation> findAll();

    @Query("SELECT * FROM t_conversation_user")
    List<ConversationUser> findAllConversationUser();

    @Query("SELECT MAX(id) FROM t_conversation_user")
    int getConversationUserMaxId();

    @Query("SELECT * FROM t_conversation_user WHERE fk_conversation = :convId and fk_member = :userId")
    ConversationUser findConversionUser(int convId, int userId);

    @Query("UPDATE t_conversation SET fk_root_thread = :fkrt WHERE id = :convId")
    void updateFKRootThreadById(int fkrt, int convId);

    @Delete
    void delete(Conversation conversation);

    @Delete
    void deleteAll(List<Conversation> conversations);

    @Delete
    void deleteConversationUsers(List<ConversationUser> convUsers);

}
