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
import com.chattree.chattree.websocket.WebSocketCaller;
import com.chattree.chattree.websocket.WebSocketService;

import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.Queue;

import static com.chattree.chattree.datasync.SyncAdapter.EXTRA_SYNC_THREAD_ID;
import static com.chattree.chattree.home.conversation.ThreadDetailFragment.BUNDLE_THREAD_ID;
import static com.chattree.chattree.websocket.WebSocketService.EXTRA_MESSAGE_ID;

public class ThreadActivity extends AppCompatActivity implements WebSocketCaller {
    private static final String TAG = "Thread Activity";

    static final String EXTRA_THREAD_ID   = "com.chattree.chattree.EXTRA_THREAD_ID";
    static final String EXTRA_THREAD_NAME = "com.chattree.chattree.EXTRA_THREAD_NAME";
    static final String EXTRA_CONV_ID     = "com.chattree.chattree.EXTRA_CONV_ID";

    private SyncReceiver      dataLoadedReceiver;
    private WebSocketReceiver newMsgInThreadReceiver;

    private int                  threadId;
    private int                  convId;
    private ThreadDetailFragment threadDetailFragment;

    private Queue<String> pendingNewMessages;

    private WebSocketService wsService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thread);

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

        pendingNewMessages = new LinkedList<>();

        FragmentManager     fragmentManager     = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        threadDetailFragment = new ThreadDetailFragment();
        Bundle args = new Bundle();
        args.putInt(BUNDLE_THREAD_ID, threadId);
        threadDetailFragment.setArguments(args);
        fragmentTransaction.add(R.id.fragment_container, threadDetailFragment);
        fragmentTransaction.commit();

        profileTextView.setText(threadName == null ? "<Sans titre>" : threadName);

        /*
         * Register a broadcast receiver to listen when the data are ready to be read from the local DB
         */
        dataLoadedReceiver = new SyncReceiver();
        registerReceiver(dataLoadedReceiver, new IntentFilter(SyncAdapter.SYNC_CALLBACK_THREAD_LOADED_ACTION));

        /*
         * Register a receiver to listen when new entities have been created (WS)
         */
        newMsgInThreadReceiver = new WebSocketReceiver();
        registerReceiver(newMsgInThreadReceiver, new IntentFilter(WebSocketService.WS_NEW_MESSAGE_ACTION));

        // Prevent keyboard from auto-appearing
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    public class SyncReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getIntExtra(EXTRA_SYNC_THREAD_ID, 0) != threadId) return; // Skip if we are not concerned
            threadDetailFragment.refreshThread();
        }
    }

    public class WebSocketReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getIntExtra(EXTRA_THREAD_ID, 0) != threadId) return; // Skip if we are not concerned

            threadDetailFragment.addMessageToView(intent.getIntExtra(EXTRA_MESSAGE_ID, 0));
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Resync the thread
        // Pass the settings flags by inserting them in a bundle
        Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
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
        unregisterReceiver(dataLoadedReceiver);
        super.onDestroy();
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected: SERVICE IS BOUND");
            wsService = ((WebSocketService.LocalBinder) service).getService();

            // TODO: send a joining room confirmation from server so we can better manage if it has been done
            // Currently, we assume that if wsService is bound, then we have joined the room (considering the short delay)
            // Join the thread room
            wsService.joinThreadRoom(threadId);

            String msgToSend;
            while ((msgToSend = pendingNewMessages.peek()) != null) {
                wsService.sendMessage(msgToSend);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            wsService = null;
        }
    };

    @Override
    public WebSocketService getWebSocketService() {
        return wsService;
    }

    @Override
    public void attemptToSendMessage(String messageContent) {
        if (wsService != null)
            wsService.sendMessage(messageContent);
        else
            pendingNewMessages.add(messageContent);
    }

    @Override
    public void attemptToJoinThreadRoom(int threadId) {
        if (wsService != null) {
            wsService.joinThreadRoom(threadId);
        }
    }
}
