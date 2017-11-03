package db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

public class DbTest {

    public class InsertTask extends AsyncTask<DbHelper,Void,Void> {

        @Override
        protected Void doInBackground(DbHelper... mDbHelpers) {
            DbHelper mDbHelper = mDbHelpers[0];
            // Gets the data repository in write mode
            SQLiteDatabase db = mDbHelper.getWritableDatabase();

            // Create a new map of values, where column names are the keys
            ContentValues values = new ContentValues();
            values.put(DbContract.tUser.COLUMN_NAME_EMAIL, "test@test.com");

            // Insert the new row, returning the primary key value of the new row
            long newRowId = db.insert(DbContract.tUser.TABLE_NAME, null, values);
            System.out.println("Inserted row id "+newRowId);
            return null;
        }
    }

    public class RetrieveTask extends AsyncTask<DbHelper,Void,Void>{

        private Cursor cursor;

        @Override
        protected Void doInBackground(DbHelper... mDbHelpers) {
            DbHelper mDbHelper = mDbHelpers[0];
            SQLiteDatabase db = mDbHelper.getReadableDatabase();

            // Define a projection that specifies which columns from the database
            // you will actually use after this query.
            String[] projection = {
                    DbContract.tUser._ID,
                    DbContract.tUser.COLUMN_NAME_EMAIL
            };

            // Filter results WHERE "title" = 'My Title'
            //String selection = FeedEntry.COLUMN_NAME_TITLE + " = ?";
            //String[] selectionArgs = { "My Title" };

            // How you want the results sorted in the resulting Cursor
            //String sortOrder = FeedEntry.COLUMN_NAME_SUBTITLE + " DESC";

            cursor = db.query(
                    DbContract.tUser.TABLE_NAME,                     // The table to query
                    projection,                               // The columns to return
                    null,                                // The columns for the WHERE clause
                    null,                            // The values for the WHERE clause
                    null,                                     // don't group the rows
                    null,                                     // don't filter by row groups
                    null                                 // The sort order
            );
            return null;
        }

        @Override
        protected void onPostExecute(Void v){
            while(cursor.moveToNext()) {
                long itemId = cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.tUser._ID));
                String email = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.tUser.COLUMN_NAME_EMAIL));
                System.out.println("Retrieved from local db : " + itemId + " " + email);
            }
            cursor.close();
        }
    }
}
