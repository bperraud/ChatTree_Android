package com.chattree.chattree.home.conversation;


import android.content.*;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.chattree.chattree.ChatTreeApplication;
import com.chattree.chattree.R;
import com.chattree.chattree.datasync.SyncAdapter;
import com.chattree.chattree.profile.ProfileActivity;
import com.chattree.chattree.websocket.WebSocketService;

import static com.chattree.chattree.home.conversation.ThreadDetailFragment.BUNDLE_THREAD_ID;
import static com.chattree.chattree.websocket.WebSocketService.EXTRA_MESSAGE_ID;

public class ThreadActivity extends AppCompatActivity {
    private static final String TAG = "Thread Activity";

    static final String EXTRA_THREAD_ID   = "com.chattree.chattree.EXTRA_THREAD_ID";
    static final String EXTRA_THREAD_NAME = "com.chattree.chattree.EXTRA_THREAD_NAME";
    static final String EXTRA_CONV_ID   = "com.chattree.chattree.EXTRA_CONV_ID";

    private WebSocketReceiver newMsgInThreadReceiver;

    private int                  threadId;
    private int                  convId;
    private ThreadDetailFragment threadDetailFragment;
    private boolean              threadIsReady;

    private WebSocketService wsService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thread);

        threadIsReady = false;

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayShowTitleEnabled(false);

        TextView profileTextView = findViewById(R.id.profileTextView);
        profileTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ProfileActivity.class);
                startActivity(intent);
            }
        });

        Intent intentThread = getIntent();
        String threadName   = intentThread.getStringExtra(EXTRA_THREAD_NAME);
        threadId = intentThread.getIntExtra(EXTRA_THREAD_ID, 0);
        convId = intentThread.getIntExtra(EXTRA_CONV_ID, 0);

        FragmentManager     fragmentManager     = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        threadDetailFragment = new ThreadDetailFragment();
        Bundle args = new Bundle();
        args.putInt(BUNDLE_THREAD_ID, threadId);
        threadDetailFragment.setArguments(args);
        fragmentTransaction.add(R.id.fragment_container, threadDetailFragment);
        fragmentTransaction.commit();

        profileTextView.setText(threadName == null ? "<Sans titre>" : threadName);

        newMsgInThreadReceiver = new WebSocketReceiver();
        registerReceiver(newMsgInThreadReceiver, new IntentFilter(WebSocketService.WS_NEW_MESSAGE_ACTION));

        // Prevent keyboard from auto-appearing
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public class WebSocketReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getIntExtra(EXTRA_THREAD_ID, 0) != threadId) return; // Skip if we are not concerned

            threadDetailFragment.addMessageToView(intent.getIntExtra(EXTRA_MESSAGE_ID, 0));
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        //Resync the conv
        // Pass the settings flags by inserting them in a bundle
        Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean(
                ContentResolver.SYNC_EXTRAS_MANUAL, true);
        settingsBundle.putBoolean(
                ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        settingsBundle.putInt(SyncAdapter.EXTRA_SYNC_CONV_ID, this.convId);
        settingsBundle.putInt(SyncAdapter.EXTRA_SYNC_THREAD_ID, this.threadId);

        /*
         * Signal the framework to run your sync adapter. Assume that
         * app initialization has already created the account.
         */
        ContentResolver.requestSync(ChatTreeApplication.getSyncAccount(this), ChatTreeApplication.AUTHORITY, settingsBundle);
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
        unbindService(serviceConnection);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(newMsgInThreadReceiver);
        super.onDestroy();
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected: SERVICE IS BOUND");
            wsService = ((WebSocketService.LocalBinder) service).getService();

            if (wsService.localThreadIsReady(threadId) && !threadIsReady && threadDetailFragment != null) {
                // Join the root thread room
                wsService.joinThreadRoom(threadId);
                threadDetailFragment.initThread();
                threadIsReady = true;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            wsService = null;
        }
    };

    public WebSocketService getWsService() {
        return wsService;
    }
}
