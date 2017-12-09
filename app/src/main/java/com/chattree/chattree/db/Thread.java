package com.chattree.chattree.db;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.PrimaryKey;

import java.util.Date;
import java.util.Objects;

@Entity(tableName = "t_thread",
        foreignKeys = {
                @ForeignKey(entity = User.class,
                        parentColumns = "id",
                        childColumns = "fk_author",
                        deferred = true),
                @ForeignKey(entity = Conversation.class,
                        parentColumns = "id",
                        childColumns = "fk_conversation",
                        deferred = true),
                @ForeignKey(entity = Thread.class,
                        parentColumns = "id",
                        childColumns = "fk_thread_parent",
                        deferred = true),
                @ForeignKey(entity = Message.class,
                        parentColumns = "id",
                        childColumns = "fk_message_parent",
                        deferred = true)
        })
public class Thread implements Comparable<Thread> {
    @PrimaryKey
    private int id;

    @ColumnInfo(name = "creation_date")
    private Date creation_date;

    @ColumnInfo(name = "title")
    private String title;

    @ColumnInfo(name = "fk_author")
    private Integer fk_author;

    @ColumnInfo(name = "fk_conversation")
    private Integer fk_conversation;

    @ColumnInfo(name = "fk_thread_parent")
    private Integer fk_thread_parent;

    @ColumnInfo(name = "fk_message_parent")
    private Integer fk_message_parent;

    public Thread(int id, Date creation_date, String title, Integer fk_author, Integer fk_conversation, Integer fk_thread_parent, Integer fk_message_parent) {
        this.id = id;
        this.creation_date = creation_date;
        this.title = title;
        this.fk_author = fk_author;
        this.fk_conversation = fk_conversation;
        this.fk_thread_parent = fk_thread_parent;
        this.fk_message_parent = fk_message_parent;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        else if (!(obj instanceof Thread)) return false;

        Thread t = (Thread) obj;

        return this.id == t.id &&
               Objects.equals(this.fk_author, t.fk_author) &&
               Objects.equals(this.fk_conversation, t.fk_conversation) &&
               Objects.equals(this.fk_thread_parent, t.fk_thread_parent) &&
               Objects.equals(this.fk_message_parent, t.fk_message_parent) &&
               Objects.equals(this.creation_date, t.creation_date) &&
               Objects.equals(this.title, t.title);
    }

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

    public Integer getFk_author() {
        return fk_author;
    }

    public void setFk_author(Integer fk_author) {
        this.fk_author = fk_author;
    }

    public Integer getFk_conversation() {
        return fk_conversation;
    }

    public void setFk_conversation(Integer fk_conversation) {
        this.fk_conversation = fk_conversation;
    }

    public Integer getFk_thread_parent() {
        return fk_thread_parent;
    }

    public void setFk_thread_parent(Integer fk_thread_parent) {
        this.fk_thread_parent = fk_thread_parent;
    }

    public Integer getFk_message_parent() {
        return fk_message_parent;
    }

    public void setFk_message_parent(Integer fk_message_parent) {
        this.fk_message_parent = fk_message_parent;
    }

    @Override
    public int compareTo(Thread o) {
        return id - o.id;
    }
}
