package com.chattree.chattree.home;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import com.chattree.chattree.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static com.chattree.chattree.login.LoginActivity.EXTRA_LOGIN_DATA;

public class HomeActivity extends AppCompatActivity {

    // Content provider authority
    public static final String AUTHORITY = "com.chattree.chattree.provider";
    // Account
    public static final String ACCOUNT = "default_account";
    // An account type, in the form of a domain name
    public static final String ACCOUNT_TYPE = "chattree.com";

    Account mAccount;

    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Intent intent        = getIntent();
        String loginDataJson = intent.getStringExtra(EXTRA_LOGIN_DATA);

        textView = (TextView) findViewById(R.id.textView);
        textView.setText(loginDataJson);

        //save the user data
        try {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor edit = pref.edit();
            JSONObject data = new JSONObject(loginDataJson).getJSONObject("data");
            JSONObject user = data.getJSONObject("user");
            edit.putString("token", data.getString("token"));
            edit.putInt("user_id",user.getInt("id"));
            edit.putString("user_login", user.getString("login"));
            edit.putString("user_email", user.getString("email"));
            edit.putString("user_firstname", user.getString("firstname"));
            edit.putString("user_lastname", user.getString("lastname"));
            edit.commit();

            JSONArray convArray = user.getJSONArray("conversations");
            int[] convIds = new int[convArray.length()];
            for(int i = 0; i<convArray.length(); i++){
                convIds[i] = convArray.getInt(i);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Create the dummy account
        mAccount = CreateSyncAccount(this);
        //enable syncing
        ContentResolver.setSyncAutomatically(mAccount, AUTHORITY, true);

        // Pass the settings flags by inserting them in a bundle
        Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean(
                ContentResolver.SYNC_EXTRAS_MANUAL, true);
        settingsBundle.putBoolean(
                ContentResolver.SYNC_EXTRAS_EXPEDITED, true);

        /*
         * Signal the framework to run your sync adapter. Assume that
         * app initialization has already created the account.
         */
        ContentResolver.requestSync(mAccount, AUTHORITY, settingsBundle);

    }

    /**
     * Create a new dummy account for the sync adapter
     *
     * @param context The application context
     */
    public static Account CreateSyncAccount(Context context) {
        // Create the account type and default account
        Account newAccount = new Account(
                ACCOUNT, ACCOUNT_TYPE);
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(
                        ACCOUNT_SERVICE);
        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
        if (accountManager.addAccountExplicitly(newAccount, null, null)) {
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call context.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */
            return newAccount;
        } else {
            /*
             * The account exists or some other error occurred. Log this, report it,
             * or handle it internally.
             */
            return accountManager.getAccountsByType("chattree.com")[0];
            //Log.e("LoginActivity", "Error creating the sync account.");
            //return null;
        }
    }
}
