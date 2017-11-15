package com.chattree.chattree.db;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "t_message",
        foreignKeys = {
        @ForeignKey(entity = User.class,
                parentColumns = "id",
                childColumns = "fk_author"),
        @ForeignKey(entity = Thread.class,
                parentColumns = "id",
                childColumns = "fk_thread_parent")
        })
public class Message {
    @PrimaryKey
    private int id;

    @ColumnInfo(name = "fk_author")
    private int fk_author;

    @ColumnInfo(name = "creation_date")
    private Date creation_date;

    @ColumnInfo(name = "content")
    private String content;

    @ColumnInfo(name = "fk_thread_parent")
    private int fk_thread_parent;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getFk_author() {
        return fk_author;
    }

    public void setFk_author(int fk_author) {
        this.fk_author = fk_author;
    }

    public Date getCreation_date() {
        return creation_date;
    }

    public void setCreation_date(Date creation_date) {
        this.creation_date = creation_date;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getFk_thread_parent() {
        return fk_thread_parent;
    }

    public void setFk_thread_parent(int fk_thread_parent) {
        this.fk_thread_parent = fk_thread_parent;
    }
}
