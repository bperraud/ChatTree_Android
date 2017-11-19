package com.chattree.chattree.login;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.arch.persistence.room.Room;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import com.chattree.chattree.R;
import com.chattree.chattree.db.AppDatabase;
import com.chattree.chattree.db.DbTest;
import com.chattree.chattree.db.User;
import com.chattree.chattree.db.UserDao;

import java.util.Locale;

public class LoginActivity extends AppCompatActivity {
    public static final String EXTRA_LOGIN_DATA = "com.chattree.chattree.LOGIN_DATA";


    private FixedTabsPagerAdapter mFixedTabsPagerAdapter;

    /**
     * The {@link ViewPager} that will display the three primary sections of the app, one at a time.
     */
    private ViewPager mViewPager;

    private Fragment loginFragment;
    private Fragment signupFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mFixedTabsPagerAdapter = new FixedTabsPagerAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        mViewPager.setAdapter(mFixedTabsPagerAdapter);


        /*AppDatabase mDb = AppDatabase.getInstance(this.getApplicationContext());
        DbTest.RoomTestTask task = new DbTest().new RoomTestTask();
        task.execute(mDb);*/

    }

    public Fragment getLoginFragment() {
        return loginFragment;
    }

    public Fragment getSignupFragment() {
        return signupFragment;
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
//            // Create fragment object
//            Fragment fragment = new DemoFragment();
//
//            // Attach some data to the fragment
//            // that we'll use to populate our fragment layouts
//            Bundle args = new Bundle();
//            args.putInt("page_position", position + 1);
//
//            // Set the arguments on the fragment
//            // that will be fetched in the
//            // DemoFragment@onCreateView
//            fragment.setArguments(args);

            switch (position) {
                case 0:
                    loginFragment = new LoginFragment();
                    return loginFragment;
                case 1:
                    signupFragment = new SignupFragment();
                    return signupFragment;
                default:
                    return null;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.login_page_title).toUpperCase(Locale.CANADA_FRENCH);
                case 1:
                    return getString(R.string.signup_page_title).toUpperCase(Locale.CANADA_FRENCH);
                default:
                    return null;
            }
        }
    }
}

