package com.chattree.chattree.db;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.PrimaryKey;

@Entity(tableName = "t_conversation",
    foreignKeys = @ForeignKey(entity = Thread.class,
                        parentColumns = "id",
                        childColumns = "fk_root_thread")
)
public class Conversation {
    @PrimaryKey
    private int id;

    @ColumnInfo(name = "fk_root_thread")
    private int fk_root_thread;

    @ColumnInfo(name = "title")
    private String title;

    @ColumnInfo(name = "picture")
    private String picture;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getFk_root_thread() {
        return fk_root_thread;
    }

    public void setFk_root_thread(int fk_root_thread) {
        this.fk_root_thread = fk_root_thread;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }
}
