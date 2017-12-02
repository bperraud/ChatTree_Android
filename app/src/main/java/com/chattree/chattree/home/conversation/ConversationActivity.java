package com.chattree.chattree.home.conversation;

import android.content.*;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import com.chattree.chattree.R;
import com.chattree.chattree.datasync.SyncAdapter;
import com.chattree.chattree.profile.ProfileActivity;
import com.chattree.chattree.tools.sliding_tab_basic.SlidingTabLayout;
import com.chattree.chattree.websocket.WebSocketService;

import java.util.Locale;

import static com.chattree.chattree.datasync.SyncAdapter.EXTRA_SYNC_CONV_ID;
import static com.chattree.chattree.home.ConversationsListFragment.EXTRA_CONVERSATION_ID;
import static com.chattree.chattree.home.ConversationsListFragment.EXTRA_CONVERSATION_TITLE;

public class ConversationActivity extends AppCompatActivity {

    private final String TAG = "CONVERSATION ACTIVITY";
    private int convId;

    private SyncReceiver dataLoadedReceiver;
    private boolean      convIsReady;

    private FixedTabsPagerAdapter mFixedTabsPagerAdapter;
    private SlidingTabLayout      mSlidingTabLayout;
    private ViewPager             mViewPager;

    private WebSocketService wsService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        convIsReady = false;

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

        Intent   activityIntent    = getIntent();
        String   convTitle         = activityIntent.getStringExtra(EXTRA_CONVERSATION_TITLE);
        TextView convTitleTextView = findViewById(R.id.conversationTitleTextView);
        convTitleTextView.setText(convTitle);
        convId = activityIntent.getIntExtra(EXTRA_CONVERSATION_ID, 0);

        /*
         * Register a broadcast receiver to listen when the data are ready to be read from the local DB
         */
        dataLoadedReceiver = new SyncReceiver();
        registerReceiver(dataLoadedReceiver, new IntentFilter(SyncAdapter.SYNC_CALLBACK_CONV_LOADED_ACTION));

        // Prevent keyboard from auto-appearing
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    public class SyncReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getIntExtra(EXTRA_SYNC_CONV_ID, 0) == convId && !convIsReady) {
                // Connect to the conversation namespace because the conversation is ready
                wsService.connectToConvNsp(convId);
                convIsReady = true;
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // WS Service binding
        Intent wsServiceIntent = new Intent(this, WebSocketService.class);
        bindService(wsServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(serviceConnection);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(dataLoadedReceiver);
        super.onDestroy();
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected: SERVICE IS BOUND");
            wsService = ((WebSocketService.LocalBinder) service).getService();

            if (wsService.localConvIsReady(convId) && !convIsReady) {
                // Connect to the conversation namespace
                wsService.connectToConvNsp(convId);
                convIsReady = true;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            wsService = null;
        }
    };

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

    public WebSocketService getWsService() {
        return wsService;
    }
}