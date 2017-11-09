package com.chattree.chattree.datasync;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import com.chattree.chattree.db.DbHelper;

/*
 * Define an implementation of ContentProvider that stubs out
 * all methods
 */
public class AppContentProvider extends ContentProvider {

    private static final String AUTHORITY = "com.chattree.chattree.provider";
    private static final int CONVERSATIONS = 1;
    private static final int MENU_ID = 2;
    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sURIMatcher.addURI(AUTHORITY, "conversations", CONVERSATIONS);
        sURIMatcher.addURI(AUTHORITY, "menu/#", MENU_ID);
    }

    public static final Uri CONVERSATIONS_URI = Uri.parse("content://" + AUTHORITY + "/" + "conversations");
    //public static final Uri MENU_URI = Uri.parse("content://" + AUTHORITY + "/" + "menu");

    private DbHelper mDbHelper;

    /*
     * Always return true, indicating that the
     * provider loaded correctly.
     */
    @Override
    public boolean onCreate() {
        mDbHelper = new DbHelper(this.getContext());
        return true;
    }
    /*
     * Return no type for MIME type
     */
    @Override
    public String getType(Uri uri) {
        switch (sURIMatcher.match(uri)) {
            case CONVERSATIONS:
                return "vnd.Android.cursor.dir/vnd.com.chattree.chattree.provider.conversations";
            case MENU_ID:
                return "vnd.Android.cursor.dir/vnd.com.chattree.chattree.provider.menu";
            default:
                throw new RuntimeException("getType No URI Match: " + uri);
        }
    }
    /*
     * query() always returns no results
     *
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        switch (sURIMatcher.match(uri)) {
            case CONVERSATIONS:
                return mDbHelper.getConversations();
            default:
                throw new UnsupportedOperationException("Not yet implemented");
        }

    }
    /*
     * insert() always returns null (no URI)
     */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }
    /*
     * delete() always returns "no rows affected" (0)
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }
    /*
     * update() always returns "no rows affected" (0)
     */
    public int update(
            Uri uri,
            ContentValues values,
            String selection,
            String[] selectionArgs) {
        return 0;
    }
}


