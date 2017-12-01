package com.chattree.chattree.db;

import android.arch.persistence.room.*;

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

    public ConversationUser(int fk_conversation, int fk_member) {
        this.fk_conversation = fk_conversation;
        this.fk_member = fk_member;
    }
    @Ignore
    public ConversationUser(int id, int fk_conversation, int fk_member) {
        this.id = id;
        this.fk_conversation = fk_conversation;
        this.fk_member = fk_member;
    }

    @Override
    public boolean equals(Object obj) {
        ConversationUser cu = (ConversationUser) obj;
        if(this.fk_conversation==cu.fk_conversation && this.fk_member==cu.fk_member)
            return true;
        else
            return false;
    }

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
