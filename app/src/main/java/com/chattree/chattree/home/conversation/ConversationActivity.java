package com.chattree.chattree.home.conversation;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import com.chattree.chattree.R;
import com.chattree.chattree.profile.ProfileActivity;
import com.chattree.chattree.tools.sliding_tab_basic.SlidingTabLayout;

import java.util.Locale;

import static com.chattree.chattree.home.ConversationsListFragment.EXTRA_CONVERSATION_TITLE;

public class ConversationActivity extends AppCompatActivity {

    private FixedTabsPagerAdapter mFixedTabsPagerAdapter;
    private SlidingTabLayout      mSlidingTabLayout;
    private ViewPager             mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
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

        mFixedTabsPagerAdapter = new FixedTabsPagerAdapter(getSupportFragmentManager());
        mViewPager = findViewById(R.id.viewpager);
        mViewPager.setAdapter(mFixedTabsPagerAdapter);

        // Give the SlidingTabLayout the ViewPager, this must be done AFTER the ViewPager has had
        // it's PagerAdapter set.
        mSlidingTabLayout = findViewById(R.id.sliding_tabs);
        mSlidingTabLayout.setDistributeEvenly(true);
        mSlidingTabLayout.setSelectedIndicatorColors(getResources().getColor(R.color.colorComplement));
        mSlidingTabLayout.setViewPager(mViewPager);


        String convTitle = getIntent().getStringExtra(EXTRA_CONVERSATION_TITLE);
        TextView convTitleTextView = findViewById(R.id.conversationTitleTextView);
        convTitleTextView.setText(convTitle);

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.conversation_toolbar_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings1:
                return true;

            case R.id.action_settings2:
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
                    return new ThreadDetailFragment();
                case 1:
                    return new ConversationTreeFragment();
                default:
                    return null;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.root_thread_title).toUpperCase(Locale.CANADA_FRENCH);
                case 1:
                    return getString(R.string.threads_title).toUpperCase(Locale.CANADA_FRENCH);
                default:
                    return null;
            }
        }
    }

}