package com.chattree.chattree.db;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.PrimaryKey;

@Entity(tableName = "t_conversation_user",
    foreignKeys = {
        @ForeignKey(entity = Conversation.class,
                parentColumns = "id",
                childColumns = "fk_conversation"),
        @ForeignKey(entity = User.class,
                parentColumns = "id",
                childColumns = "fk_member")
    }
)
public class ConversationUser {
    @PrimaryKey
    private int id;

    @ColumnInfo(name = "fk_conversation")
    private int fk_conversation;

    @ColumnInfo(name = "fk_member")
    private int fk_member;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getFk_conversation() {
        return fk_conversation;
    }

    public void setFk_conversation(int fk_conversation) {
        this.fk_conversation = fk_conversation;
    }

    public int getFk_member() {
        return fk_member;
    }

    public void setFk_member(int fk_member) {
        this.fk_member = fk_member;
    }
}
