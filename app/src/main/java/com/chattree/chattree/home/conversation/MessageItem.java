package com.chattree.chattree.home.conversation;

import java.util.Date;

/**
 * Created by steveamadodias on 19/11/2017.
 */

public class MessageItem {
    public enum OwnerValues {ME, OTHER}

    private int         id;
    private int         thread;
    private int         author;
    private String      content;
    private Date        date;
    private String      pseudo;
    private OwnerValues owner;

    MessageItem(int id, int thread, int author, String content, Date date, String pseudo, OwnerValues owner) {
        this.id = id;
        this.thread = thread;
        this.author = author;
        this.content = content;
        this.date = date;
        this.pseudo = pseudo;
        this.owner = owner;
    }

    public int getId() {
        return id;
    }

    public int getThread() {
        return thread;
    }

    public int getAuthor() {
        return author;
    }

    public Date getDate() {
        return date;
    }

    public String getContent() {
        return content;
    }

    public String getPseudo() {
        return pseudo;
    }

    public OwnerValues getOwner() {
        return owner;
    }

}
