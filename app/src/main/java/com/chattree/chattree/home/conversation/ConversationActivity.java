package com.chattree.chattree.home.conversation;

import android.accounts.Account;
import android.app.Activity;
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
import com.chattree.chattree.ChatTreeApplication;
import com.chattree.chattree.R;
import com.chattree.chattree.datasync.SyncAdapter;
import com.chattree.chattree.home.HomeActivity;
import com.chattree.chattree.profile.ProfileActivity;
import com.chattree.chattree.tools.Utils;
import com.chattree.chattree.tools.sliding_tab_basic.SlidingTabLayout;
import com.chattree.chattree.websocket.WebSocketService;

import java.util.Locale;

import static com.chattree.chattree.datasync.SyncAdapter.*;
import static com.chattree.chattree.home.ConversationsListFragment.EXTRA_CONVERSATION_ID;
import static com.chattree.chattree.home.ConversationsListFragment.EXTRA_CONVERSATION_ROOT_THREAD_ID;
import static com.chattree.chattree.home.ConversationsListFragment.EXTRA_CONVERSATION_TITLE;
import static com.chattree.chattree.home.conversation.ConversationTreeFragment.BUNDLE_CONV_ID;
import static com.chattree.chattree.home.conversation.ConversationTreeFragment.BUNDLE_ROOT_THREAD_ID;
import static com.chattree.chattree.home.conversation.ThreadDetailFragment.BUNDLE_THREAD_ID;
import static com.chattree.chattree.websocket.WebSocketService.*;

public class ConversationActivity extends AppCompatActivity {

    private final String TAG = "CONVERSATION ACTIVITY";
    private int    convId;
    private int    rootThreadId;
    private String convTitle;

    private ThreadDetailFragment     rootThreadDetailFragment;
    private ConversationTreeFragment conversationTreeFragment;

    private SyncReceiver      dataLoadedReceiver;
    private WebSocketReceiver objectReceivedFromWSReceiver;
    private boolean           convIsReady;
    private boolean           rootThreadIsReady;
    private boolean           currentTabIsRootThread;

    private FixedTabsPagerAdapter mFixedTabsPagerAdapter;
    private SlidingTabLayout      mSlidingTabLayout;
    private ViewPager             mViewPager;

    private WebSocketService wsService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "ON CREATE");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        final Activity thisActivity = this;

        convIsReady = false;
        rootThreadIsReady = false;
        currentTabIsRootThread = true;

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
        mSlidingTabLayout.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            // This method will be invoked when a new page becomes selected.
            @Override
            public void onPageSelected(int position) {
                // If we return to the main thread, we need to join the corresponding ws room
                if (position == 0 && wsService != null) {
                    wsService.joinThreadRoom(rootThreadId);
                    rootThreadDetailFragment.initThread();
                }
                // Load the conv tree in the corresponding fragment
                else if (position == 1) {
                    Utils.hideKeyboard(thisActivity);

                    if (conversationTreeFragment != null)
                        conversationTreeFragment.initConvTree();
                }

                currentTabIsRootThread = position == 0;
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        Intent   activityIntent    = getIntent();
        String   convTitle         = activityIntent.getStringExtra(EXTRA_CONVERSATION_TITLE);
        TextView convTitleTextView = findViewById(R.id.conversationTitleTextView);
        convTitleTextView.setText(convTitle);
        convId = activityIntent.getIntExtra(EXTRA_CONVERSATION_ID, 0);
        rootThreadId = activityIntent.getIntExtra(EXTRA_CONVERSATION_ROOT_THREAD_ID, 0);
        if (rootThreadId == 0)
            throw new RuntimeException("rootThreadId not found, 0 given as default, convId: " + convId);

        /*
         * Register a broadcast receiver to listen when the data are ready to be read from the local DB
         */
        dataLoadedReceiver = new SyncReceiver();
        registerReceiver(dataLoadedReceiver, new IntentFilter(SyncAdapter.SYNC_CALLBACK_CONV_LOADED_ACTION));
        registerReceiver(dataLoadedReceiver, new IntentFilter(SyncAdapter.SYNC_CALLBACK_THREAD_LOADED_ACTION));

        /*
         * Register a receiver to listen when new entities have been created (WS)
         */
        objectReceivedFromWSReceiver = new WebSocketReceiver();
        registerReceiver(objectReceivedFromWSReceiver, new IntentFilter(WebSocketService.WS_NEW_MESSAGE_ACTION));
        registerReceiver(objectReceivedFromWSReceiver, new IntentFilter(WebSocketService.WS_NEW_THREAD_ACTION));

        // WS Service binding
        Intent wsServiceIntent = new Intent(this, WebSocketService.class);
        bindService(wsServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        // Prevent keyboard from auto-appearing
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    public class SyncReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getIntExtra(EXTRA_SYNC_CONV_ID, 0) != convId) return; // Skip if we are not concerned
            switch (intent.getAction()) {
                case SyncAdapter.SYNC_CALLBACK_CONV_LOADED_ACTION:
                    if (!convIsReady) {
                        // Connect to the conversation namespace because the conversation is ready
                        wsService.connectToConvNsp(convId);
                        convIsReady = true;
                        // Load the conv tree in the corresponding fragment
                        conversationTreeFragment.initConvTree();
                    }
                    break;
                case SyncAdapter.SYNC_CALLBACK_THREAD_LOADED_ACTION:
                    if (intent.getIntExtra(EXTRA_SYNC_THREAD_ID, 0) == rootThreadId) {
                        rootThreadIsReady = true;
                    }
                    if (currentTabIsRootThread && rootThreadIsReady && wsService != null) {
                        wsService.joinThreadRoom(rootThreadId);
                        rootThreadDetailFragment.initThread();
                    }
                    break;
            }
        }
    }

    public class WebSocketReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case WS_NEW_MESSAGE_ACTION:
                    if (intent.getIntExtra(EXTRA_THREAD_ID, 0) != rootThreadId) return; // Skip if we are not concerned
                    rootThreadDetailFragment.addMessageToView(intent.getIntExtra(EXTRA_MESSAGE_ID, 0));
                    break;
                case WS_NEW_THREAD_ACTION:
                    if (intent.getIntExtra(EXTRA_CONV_ID, 0) != convId) return; // Skip if we are not concerned
                    conversationTreeFragment.addThread(intent.getIntExtra(EXTRA_THREAD_ID, 0));
                    break;
            }
        }
    }

    @Override
    protected void onResume() {
        //Resync the conv
        // Pass the settings flags by inserting them in a bundle
        Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean(
                ContentResolver.SYNC_EXTRAS_MANUAL, true);
        settingsBundle.putBoolean(
                ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        settingsBundle.putInt(SyncAdapter.EXTRA_SYNC_CONV_ID, this.convId);

        /*
         * Signal the framework to run your sync adapter. Assume that
         * app initialization has already created the account.
         */
        ContentResolver.requestSync(ChatTreeApplication.getSyncAccount(this), ChatTreeApplication.AUTHORITY, settingsBundle);

        // Always try to join the thread room
        if (currentTabIsRootThread && rootThreadIsReady && wsService != null) {
            wsService.joinThreadRoom(rootThreadId);
            rootThreadDetailFragment.initThread();
        }
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        unbindService(serviceConnection);
        unregisterReceiver(objectReceivedFromWSReceiver);
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
                // Load the conv tree in the corresponding fragment
                if (conversationTreeFragment != null) {
                    conversationTreeFragment.initConvTree();
                }
            }

            if (wsService.localThreadIsReady(rootThreadId) && !rootThreadIsReady && currentTabIsRootThread && rootThreadDetailFragment != null) {
                // Join the root thread room
                wsService.joinThreadRoom(rootThreadId);
                rootThreadDetailFragment.initThread();
                rootThreadIsReady = true;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            wsService = null;
        }
    };

    public void attemptJoinThreadRoom() {
        if (wsService != null) {
            wsService.joinThreadRoom(rootThreadId);
            rootThreadIsReady = true;
        }
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
                    rootThreadDetailFragment = new ThreadDetailFragment();
                    Bundle args1 = new Bundle();
                    args1.putInt(BUNDLE_THREAD_ID, rootThreadId);
                    rootThreadDetailFragment.setArguments(args1);
                    return rootThreadDetailFragment;
                case 1:
                    conversationTreeFragment = new ConversationTreeFragment();
                    Bundle args2 = new Bundle();
                    args2.putInt(BUNDLE_CONV_ID, convId);
                    args2.putInt(BUNDLE_ROOT_THREAD_ID, rootThreadId);
                    // TODO: check the line below (useful ?)
                    args2.putString("CONV_TITLE", convTitle);
                    conversationTreeFragment.setArguments(args2);
                    return conversationTreeFragment;
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

    @Override
    public void onBackPressed() {
        // Cancel the creation of a new thread
        if (conversationTreeFragment.isOnThreadSelectedState()) {
            conversationTreeFragment.clearThreadSelection(true);
        } else {
            super.onBackPressed();
        }
    }

    ConversationTreeFragment getConversationTreeFragment() {
        return conversationTreeFragment;
    }
}