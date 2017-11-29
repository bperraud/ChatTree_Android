package com.chattree.chattree.db;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "t_thread",
        foreignKeys = {
        @ForeignKey(entity = User.class,
                parentColumns = "id",
                childColumns = "fk_author"),
        @ForeignKey(entity = Conversation.class,
                parentColumns = "id",
                childColumns = "fk_conversation"),
        @ForeignKey(entity = Thread.class,
                parentColumns = "id",
                childColumns = "fk_thread_parent"),
        @ForeignKey(entity = Message.class,
                parentColumns = "id",
                childColumns = "fk_message_parent")
})
public class Thread {
    @PrimaryKey
    private int id;

    @ColumnInfo(name = "creation_date")
    private Date creation_date;

    @ColumnInfo(name = "title")
    private String title;

    @ColumnInfo(name = "fk_author")
    private int fk_author;

    @ColumnInfo(name = "fk_conversation")
    private int fk_conversation;

    @ColumnInfo(name = "fk_thread_parent")
    private int fk_thread_parent;

    @ColumnInfo(name = "fk_message_parent")
    private int fk_message_parent;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Date getCreation_date() {
        return creation_date;
    }

    public void setCreation_date(Date creation_date) {
        this.creation_date = creation_date;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getFk_author() {
        return fk_author;
    }

    public void setFk_author(int fk_author) {
        this.fk_author = fk_author;
    }

    public int getFk_conversation() {
        return fk_conversation;
    }

    public void setFk_conversation(int fk_conversation) {
        this.fk_conversation = fk_conversation;
    }

    public int getFk_thread_parent() {
        return fk_thread_parent;
    }

    public void setFk_thread_parent(int fk_thread_parent) {
        this.fk_thread_parent = fk_thread_parent;
    }

    public int getFk_message_parent() {
        return fk_message_parent;
    }

    public void setFk_message_parent(int fk_message_parent) {
        this.fk_message_parent = fk_message_parent;
    }
}
