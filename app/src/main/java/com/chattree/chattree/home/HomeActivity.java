package com.chattree.chattree.home;

import android.content.*;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import com.chattree.chattree.ChatTreeApplication;
import com.chattree.chattree.R;
import com.chattree.chattree.db.AppDatabase;
import com.chattree.chattree.db.Conversation;
import com.chattree.chattree.db.ConversationDao;
import com.chattree.chattree.db.ConversationDao.CustomConversationUser;
import com.chattree.chattree.profile.ProfileActivity;
import com.chattree.chattree.tools.Toaster;
import com.chattree.chattree.tools.sliding_tab_basic.SlidingTabLayout;
import com.chattree.chattree.websocket.WebSocketService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.chattree.chattree.datasync.SyncAdapter.*;
import static com.chattree.chattree.login.LoginActivity.EXTRA_LOGIN_DATA;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HOME ACTIVITY";

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

        Toolbar toolbarBottom = findViewById(R.id.toolbar_bottom);
        toolbarBottom.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_home:
                        Toast.makeText(getApplicationContext(), "GO HOME", Toast.LENGTH_SHORT).show();
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

        Intent activityIntent = getIntent();
        String loginDataJson  = activityIntent.getStringExtra(EXTRA_LOGIN_DATA);
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
        registerReceiver(dataLoadedReceiver, new IntentFilter(SYNC_CALLBACK_ALL_CONV_LOADED_ACTION));
        registerReceiver(dataLoadedReceiver, new IntentFilter(SYNC_CALLBACK_CONV_LOADED_ACTION));

        /*
         * Signal the framework to run your sync adapter. Assume that
         * app initialization has already created the account.
         */
        ContentResolver.requestSync(ChatTreeApplication.getSyncAccount(this), ChatTreeApplication.AUTHORITY, settingsBundle);

        Toaster.showCustomToast(this, getString(R.string.login_successful_toast), null);
    }

    public class SyncReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, final Intent intent) {
            switch (intent.getAction()) {
                case SYNC_CALLBACK_ALL_CONV_LOADED_ACTION:
                    conversationsListFragment.refreshConvs();
                    break;
                case SYNC_CALLBACK_CONV_LOADED_ACTION:
                    new AsyncTask<Void, Void, Conversation>() {
                        @Override
                        protected Conversation doInBackground(Void... params) {
                            ConversationDao conversationDao = AppDatabase.getInstance(getApplicationContext()).conversationDao();
                            return conversationDao.findById(intent.getIntExtra(EXTRA_SYNC_CONV_ID, 0));
                        }

                        @Override
                        protected void onPostExecute(Conversation conversation) {
                            conversationsListFragment.updateRootThreadOfConv(conversation);
                        }
                    }.execute();
                    break;
            }


        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(dataLoadedReceiver);
        super.onDestroy();
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
