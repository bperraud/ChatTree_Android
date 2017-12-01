package com.chattree.chattree.home;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.*;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import com.chattree.chattree.R;
import com.chattree.chattree.db.AppDatabase;
import com.chattree.chattree.db.ConversationDao;
import com.chattree.chattree.db.ConversationDao.CustomConversationUser;
import com.chattree.chattree.network.NetConnectCallback;
import com.chattree.chattree.network.NetworkFragment;
import com.chattree.chattree.websocket.WebSocketService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.chattree.chattree.profile.ProfileActivity;
import com.chattree.chattree.tools.sliding_tab_basic.SlidingTabLayout;


import java.util.*;

import static com.chattree.chattree.datasync.SyncAdapter.EXTRA_SYNC_STATUS;
import static com.chattree.chattree.datasync.SyncAdapter.EXTRA_SYNC_STATUS_DONE;
import static com.chattree.chattree.login.LoginActivity.EXTRA_LOGIN_DATA;
import static com.chattree.chattree.network.NetworkFragment.HTTP_METHOD_GET;

public class HomeActivity extends AppCompatActivity implements NetConnectCallback {

    // Content provider authority
    private static final String AUTHORITY    = "com.chattree.chattree.provider";
    // Account
    private static final String ACCOUNT      = "default_account";
    // An account type, in the form of a domain name
    private static final String ACCOUNT_TYPE = "chattree.com";


    private static final String TAG = "HOME ACTIVITY";

    public static final String SYNC_CALLBACK_INTENT_ACTION = "com.chattree.chattree.SYNC";

    private Account mAccount;

    private SyncReceiver dataLoadedReceiver;

    private FixedTabsPagerAdapter mFixedTabsPagerAdapter;
    private SlidingTabLayout      mSlidingTabLayout;
    private ViewPager             mViewPager;

    private ConversationsListFragment conversationsListFragment;
    private ContactsListFragment      contactsListFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setLogo(R.drawable.favicon_black);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayShowTitleEnabled(false);

        ImageButton profileImageBtn = findViewById(R.id.profileImageBtn);
        profileImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ProfileActivity.class);
                startActivity(intent);
            }
        });

        // Create the headless fragment which encapsulates the AsyncTask for the login op
        final NetworkFragment mNetworkFragment = NetworkFragment.getInstance(getSupportFragmentManager(), "api/get-conversations", HTTP_METHOD_GET);


        Toolbar toolbarBottom = findViewById(R.id.toolbar_bottom);
        toolbarBottom.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_home:
                        Toast.makeText(getApplicationContext(), "GO HOME", Toast.LENGTH_SHORT).show();
                        mNetworkFragment.startRequest(null);
                        return true;
                    case R.id.action_group_conversations:
                        Toast.makeText(getApplicationContext(), "GO TO GROUP CONVERSATIONS", Toast.LENGTH_SHORT).show();
                        return true;
                    case R.id.action_settings:
                        Toast.makeText(getApplicationContext(), "SETTINGS", Toast.LENGTH_SHORT).show();
                        return true;
                }
                return true;
            }
        });
        // Inflate a menu to be displayed in the toolbar
        toolbarBottom.inflateMenu(R.menu.home_toolbar_bottom_menu);


        mFixedTabsPagerAdapter = new FixedTabsPagerAdapter(getSupportFragmentManager());
        mViewPager = findViewById(R.id.viewpager);
        mViewPager.setAdapter(mFixedTabsPagerAdapter);


        // Give the SlidingTabLayout the ViewPager, this must be done AFTER the ViewPager has had
        // it's PagerAdapter set.
        mSlidingTabLayout = findViewById(R.id.sliding_tabs);
        mSlidingTabLayout.setDistributeEvenly(true);
        mSlidingTabLayout.setSelectedIndicatorColors(getResources().getColor(R.color.colorComplement));
        mSlidingTabLayout.setViewPager(mViewPager);

        // ------------------------------------------------------------------ //
        // ----------------------- Save the user data ----------------------- //
        // ------------------------------------------------------------------ //

        Intent     activityIntent = getIntent();
        String loginDataJson = activityIntent.getStringExtra(EXTRA_LOGIN_DATA);
        try {
            SharedPreferences        pref = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor edit = pref.edit();
            JSONObject               data = new JSONObject(loginDataJson).getJSONObject("data");
            JSONObject               user = data.getJSONObject("user");
            edit.putString("token", data.getString("token"));
            edit.putInt("user_id", user.getInt("id"));
            edit.putString("user_login", user.getString("login"));
            edit.putString("user_email", user.getString("email"));
            edit.putString("user_firstname", user.getString("firstname"));
            edit.putString("user_lastname", user.getString("lastname"));
            JSONArray   convArray = user.getJSONArray("conversations");
            Set<String> convIds   = new HashSet<>();
            for (int i = 0; i < convArray.length(); i++) {
                convIds.add(String.valueOf(convArray.getInt(i)));
            }
            edit.putStringSet("conversations_ids", convIds);
            edit.apply();

        } catch (JSONException e) {
            e.printStackTrace();
        }


        // ----------------------------------------------------------------- //
        // ----------------------- WebSocket Service ----------------------- //
        // ----------------------------------------------------------------- //

        // Start the updater service
        Intent wsServiceIntent = new Intent(this, WebSocketService.class);
        startService(wsServiceIntent);

        //-------------------------------------------------------------------------------------
        //-------------------------------------------------------------------------------------

        // Create the dummy account
        mAccount = CreateSyncAccount(this);
        // Enable syncing
        ContentResolver.setSyncAutomatically(mAccount, AUTHORITY, true);

        // Pass the settings flags by inserting them in a bundle
        Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean(
                ContentResolver.SYNC_EXTRAS_MANUAL, true);
        settingsBundle.putBoolean(
                ContentResolver.SYNC_EXTRAS_EXPEDITED, true);


        /*
         * Register a broadcast receiver to listen when the data are ready to be read from the local DB
         */
        dataLoadedReceiver = new SyncReceiver();
        registerReceiver(dataLoadedReceiver, new IntentFilter(SYNC_CALLBACK_INTENT_ACTION));

        /*
         * Signal the framework to run your sync adapter. Assume that
         * app initialization has already created the account.
         */
        ContentResolver.requestSync(mAccount, AUTHORITY, settingsBundle);
    }

//    private boolean isWsServiceRunning() {
//        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
//        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
//            if (WebSocketService.class.getName().equals(service.service.getClassName())) {
//                return true;
//            }
//        }
//        return false;
//    }
//


    public class SyncReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getStringExtra(EXTRA_SYNC_STATUS).equals(EXTRA_SYNC_STATUS_DONE)) {

                new AsyncTask<Void, Void, List<CustomConversationUser>>() {
                    @Override
                    protected List<CustomConversationUser> doInBackground(Void... params) {
                        ConversationDao conversationDao = AppDatabase.getInstance(getApplicationContext()).conversationDao();
                        return conversationDao.getCustomConversationUsers();
                    }

                    @Override
                    protected void onPostExecute(List<CustomConversationUser> customConversationUsers) {
                        conversationsListFragment.refreshListOfConv(customConversationUsers);
                    }
                }.execute();

            }
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(dataLoadedReceiver);
        super.onDestroy();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home_toolbar_menu, menu);

        MenuItem   searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                callSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
//                callSearch(newText);
//                return true;
                return false;
            }

            void callSearch(String query) {
                // Do searching
                Log.d("SEARCHING", "query: " + query);
            }

        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings1:
                return true;

            case R.id.action_settings2:
                return true;

            case R.id.action_notifications:
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    public void updateFromRequest(Object result) {
        Log.d(TAG, "updateFromRequest: " + result);
    }

    @Override
    public NetworkInfo getActiveNetworkInfo() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectivityManager.getActiveNetworkInfo();
    }

    @Override
    public void onProgressUpdate(int progressCode, int percentComplete) {

    }

    @Override
    public void finishRequesting() {

    }

    class FixedTabsPagerAdapter extends FragmentPagerAdapter {

        FixedTabsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    conversationsListFragment = new ConversationsListFragment();
                    return conversationsListFragment;
                case 1:
                    contactsListFragment = new ContactsListFragment();
                    return contactsListFragment;
                default:
                    return null;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.conversations_title).toUpperCase(Locale.CANADA_FRENCH);
                case 1:
                    return getString(R.string.contacts_title).toUpperCase(Locale.CANADA_FRENCH);
                default:
                    return null;
            }
        }
    }
}
