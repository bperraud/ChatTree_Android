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
    private Integer fk_root_thread;

    @ColumnInfo(name = "title")
    private String title;

    @ColumnInfo(name = "picture")
    private String picture;

    public Conversation(int id, Integer fk_root_thread, String title, String picture) {
        this.id = id;
        this.fk_root_thread = fk_root_thread;
        this.title = title;
        this.picture = picture;
    }

    @Override
    public boolean equals(Object obj) {
        Conversation c = (Conversation) obj;
        boolean ret = true;
        ret = ret && this.id==c.id && this.fk_root_thread==c.fk_root_thread;
        if(this.title==null)
            ret = ret && c.title==null;
        else
            ret = ret && this.title.equals(c.title);
        if(this.picture==null)
            ret = ret && c.picture==null;
        else
            ret = ret && this.picture.equals(c.picture);
        return ret;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getFk_root_thread() {
        return fk_root_thread;
    }

    public void setFk_root_thread(Integer fk_root_thread) {
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
