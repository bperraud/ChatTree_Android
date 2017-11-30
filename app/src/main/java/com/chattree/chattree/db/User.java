package com.chattree.chattree.db;


import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity(tableName = "t_user")
public class User {
    @PrimaryKey
    private int id;

    @ColumnInfo(name = "login")
    private String login;

    @ColumnInfo(name = "email")
    private String email;

    @ColumnInfo(name = "firstname")
    private String firstname;

    @ColumnInfo(name = "lastname")
    private String lastname;

    @ColumnInfo(name = "profile_picture")
    private String profile_picture;

    public User(int id, String login, String email, String firstname, String lastname, String profile_picture) {
        this.id = id;
        this.login = login;
        this.email = email;
        this.firstname = firstname;
        this.lastname = lastname;
        this.profile_picture = profile_picture;
    }

    @Override
    public boolean equals(Object obj) {
        User u = (User) obj;
        boolean ret = true;
        ret = ret && this.id==u.id;
        if(this.login==null)
            ret = ret && u.login==null;
        else
            ret = ret && this.login.equals(u.login);
        if(this.email==null)
            ret = ret && u.email==null;
        else
            ret = ret && this.email.equals(u.email);
        if(this.firstname==null)
            ret = ret && u.firstname==null;
        else
            ret = ret && this.firstname.equals(u.firstname);
        if(this.lastname==null)
            ret = ret && u.lastname==null;
        else
            ret = ret && this.lastname.equals(u.lastname);
        if(this.profile_picture==null)
            ret = ret && u.profile_picture==null;
        else
            ret = ret && this.profile_picture.equals(u.profile_picture);
        return ret;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getProfile_picture() {
        return profile_picture;
    }

    public void setProfile_picture(String profile_picture) {
        this.profile_picture = profile_picture;
    }
}
