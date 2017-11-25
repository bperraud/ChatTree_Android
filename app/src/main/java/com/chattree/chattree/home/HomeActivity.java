package com.chattree.chattree.home;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.content.SharedPreferences;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.chattree.chattree.profile.ProfileActivity;
import com.chattree.chattree.tools.sliding_tab_basic.SlidingTabLayout;

import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    // Content provider authority
    public static final String AUTHORITY = "com.chattree.chattree.provider";
    // Account
    public static final String ACCOUNT = "default_account";
    // An account type, in the form of a domain name
    public static final String ACCOUNT_TYPE = "chattree.com";

    Account mAccount;

    private FixedTabsPagerAdapter mFixedTabsPagerAdapter;
    private SlidingTabLayout      mSlidingTabLayout;
    private ViewPager             mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setLogo(R.drawable.favicon_black);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayShowTitleEnabled(false);

        ImageButton profileImageBtn = (ImageButton) findViewById(R.id.profileImageBtn);
        profileImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ProfileActivity.class);
                startActivity(intent);
            }
        });


        Toolbar toolbarBottom = (Toolbar) findViewById(R.id.toolbar_bottom);
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
        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        mViewPager.setAdapter(mFixedTabsPagerAdapter);


        // Give the SlidingTabLayout the ViewPager, this must be done AFTER the ViewPager has had
        // it's PagerAdapter set.
        mSlidingTabLayout = (SlidingTabLayout) findViewById(R.id.sliding_tabs);
        mSlidingTabLayout.setDistributeEvenly(true);
        mSlidingTabLayout.setSelectedIndicatorColors(getResources().getColor(R.color.colorComplement));
        mSlidingTabLayout.setViewPager(mViewPager);


//        Intent intent        = getIntent();
//        String loginDataJson = intent.getStringExtra(EXTRA_LOGIN_DATA);
//
//        textView = (TextView) findViewById(R.id.textView);
//        textView.setText(loginDataJson);

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
//                    loginFragment = new LoginFragment();
//                    return loginFragment;
                    return new ConversationsListFragment();
                case 1:
//                    signupFragment = new SignupFragment();
//                    return signupFragment;
                    return new ContactsListFragment();
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
