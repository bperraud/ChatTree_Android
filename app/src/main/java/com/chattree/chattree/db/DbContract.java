package com.chattree.chattree.db;

import android.provider.BaseColumns;

//Defines the database schema
public final class DbContract {
    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private DbContract() {}

    /* Inner class that defines the table contents */
    public static class tUser implements BaseColumns {
        public static final String TABLE_NAME = "t_user";
        public static final String COLUMN_NAME_LOGIN = "login";
        public static final String COLUMN_NAME_EMAIL = "email";
        public static final String COLUMN_NAME_FIRSTNAME = "firstname";
        public static final String COLUMN_NAME_LASTNAME = "lastname";
        public static final String COLUMN_NAME_PROFILE_PICTURE = "profile_picture";
    }

    /* Inner class that defines the table contents */
    public static class tMessage implements BaseColumns {
        public static final String TABLE_NAME = "t_message";
        public static final String COLUMN_NAME_FK_AUTHOR = "fk_author";
        public static final String COLUMN_NAME_CREATION_DATE = "creation_date";
        public static final String COLUMN_NAME_CONTENT = "content";
        public static final String COLUMN_NAME_FK_THREAD_PARENT = "fk_thread_parent";
    }

    /* Inner class that defines the table contents */
    public static class tThread implements BaseColumns {
        public static final String TABLE_NAME = "t_thread";
        public static final String COLUMN_NAME_CREATION_DATE = "creation_date";
        public static final String COLUMN_NAME_TITLE = "title";
        public static final String COLUMN_NAME_FK_AUTHOR = "fk_author";
        public static final String COLUMN_NAME_FK_CONVERSATION = "fk_conversation";
        public static final String COLUMN_NAME_FK_THREAD_PARENT = "fk_thread_parent";
        public static final String COLUMN_NAME_FK_MESSAGE_PARENT = "fk_message_parent";
    }

    /* Inner class that defines the table contents */
    public static class tConversation implements BaseColumns {
        public static final String TABLE_NAME = "t_conversation";
        public static final String COLUMN_NAME_FK_ROOT_THREAD = "fk_root_thread";
        public static final String COLUMN_NAME_TITLE = "title";
        public static final String COLUMN_NAME_PICTURE = "picture";
    }

    /* Inner class that defines the table contents */
    public static class tConversationUser implements BaseColumns {
        public static final String TABLE_NAME = "t_conversation_user";
        public static final String COLUMN_NAME_FK_CONVERSATION = "fk_conversation";
        public static final String COLUMN_NAME_FK_MEMBER = "fk_member";
    }

    public static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + tUser.TABLE_NAME + " (" +
                    tUser._ID + " INTEGER PRIMARY KEY," +
                    tUser.COLUMN_NAME_LOGIN + " VARCHAR(200)," +
                    tUser.COLUMN_NAME_EMAIL + " VARCHAR(200) UNIQUE NOT NULL," +
                    tUser.COLUMN_NAME_FIRSTNAME + " VARCHAR(200)," +
                    tUser.COLUMN_NAME_LASTNAME + " VARCHAR(200)," +
                    tUser.COLUMN_NAME_PROFILE_PICTURE + " VARCHAR(200)); " +
            "CREATE TABLE " + tMessage.TABLE_NAME + " (" +
                    tMessage._ID + " INTEGER PRIMARY KEY," +
                    tMessage.COLUMN_NAME_FK_AUTHOR + " INTEGER REFERENCES "+tUser.TABLE_NAME+" NOT NULL," +
                    tMessage.COLUMN_NAME_CREATION_DATE + " TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                    tMessage.COLUMN_NAME_CONTENT + " TEXT NOT NULL);" +
            "CREATE TABLE " + tThread.TABLE_NAME + " (" +
                    tThread._ID + " INTEGER PRIMARY KEY," +
                    tThread.COLUMN_NAME_CREATION_DATE + " TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                    tThread.COLUMN_NAME_TITLE + " VARCHAR(200)," +
                    tThread.COLUMN_NAME_FK_AUTHOR + " INTEGER REFERENCES "+tUser.TABLE_NAME+"," +
                    tThread.COLUMN_NAME_FK_THREAD_PARENT + " INTEGER REFERENCES "+tThread.TABLE_NAME+"," +
                    tThread.COLUMN_NAME_FK_MESSAGE_PARENT + " INTEGER REFERENCES "+tMessage.TABLE_NAME+"); " +
            "ALTER TABLE " + tMessage.TABLE_NAME +
                    " ADD COLUMN " + tMessage.COLUMN_NAME_FK_THREAD_PARENT + " INTEGER " +
                    " REFERENCES " + tThread.TABLE_NAME + "; " +
            "CREATE TABLE " + tConversation.TABLE_NAME + " (" +
                    tConversation._ID + " INTEGER PRIMARY KEY," +
                    tConversation.COLUMN_NAME_FK_ROOT_THREAD + " INTEGER REFERENCES "+tThread.TABLE_NAME+"," +
                    tConversation.COLUMN_NAME_TITLE + " VARCHAR(200)," +
                    tConversation.COLUMN_NAME_PICTURE + " VARCHAR(200)); " +
            "ALTER TABLE " + tThread.TABLE_NAME +
                    " ADD COLUMN " + tThread.COLUMN_NAME_FK_CONVERSATION +
                    " REFERENCES " + tConversation.TABLE_NAME + "; " +
            "CREATE TABLE " + tConversationUser.TABLE_NAME + " (" +
                    tConversationUser._ID + " INTEGER PRIMARY KEY," +
                    tConversationUser.COLUMN_NAME_FK_CONVERSATION + " INTEGER NOT NULL REFERENCES "+tConversation.TABLE_NAME+"," +
                    tConversationUser.COLUMN_NAME_FK_MEMBER + " INTEGER NOT NULL REFERENCES "+tUser.TABLE_NAME+");";


    public static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + tUser.TABLE_NAME + "; " +
            "DROP TABLE IF EXISTS " + tMessage.TABLE_NAME + "; " +
            "DROP TABLE IF EXISTS " + tThread.TABLE_NAME + "; " +
            "DROP TABLE IF EXISTS " + tConversation.TABLE_NAME + "; " +
            "DROP TABLE IF EXISTS " + tConversationUser.TABLE_NAME + "; ";
}


