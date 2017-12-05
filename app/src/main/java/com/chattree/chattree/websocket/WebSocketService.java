package com.chattree.chattree.websocket;

import android.app.Service;
import android.content.*;
import android.os.*;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;
import com.chattree.chattree.ChatTreeApplication;
import com.chattree.chattree.datasync.SyncAdapter;
import com.chattree.chattree.db.AppDatabase;
import com.chattree.chattree.db.Message;
import com.chattree.chattree.db.MessageDao;
import io.socket.client.IO;
import io.socket.client.Manager;
import io.socket.client.Socket;
import io.socket.engineio.client.Transport;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpCookie;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.chattree.chattree.datasync.SyncAdapter.EXTRA_SYNC_CONV_ID;
import static com.chattree.chattree.datasync.SyncAdapter.EXTRA_SYNC_THREAD_ID;
import static com.chattree.chattree.network.NetworkFragment.BASE_URL;
import static io.socket.emitter.Emitter.*;

public class WebSocketService extends Service {

    private final String TAG = "WEBSOCKET SERVICE";

    @SuppressWarnings("FieldCanBeLocal")
    private static final String WS_EVENT_CREATE_MESSAGE   = "create-message";
    private static final String WS_EVENT_JOIN_THREAD_ROOM = "join-thread-room";

    public static final String WS_NEW_MESSAGE_ACTION = "com.chattree.chattree.WS_NEW_MESSAGE_ACTION";
    public static final String EXTRA_THREAD_ID       = "com.chattree.chattree.EXTRA_THREAD_ID";
    public static final String EXTRA_MESSAGE_ID      = "com.chattree.chattree.EXTRA_MESSAGE_ID";

    private SyncReceiver dataLoadedReceiver;
    private Set<Integer> localConvIdsLoaded;
    private Set<Integer> localThreadIdsLoaded;

    private String token;
    private Socket mainSocket;
    private Socket activeConvSocket;

    public class LocalBinder extends Binder {
        public WebSocketService getService() {
            return WebSocketService.this;
        }
    }

    private final IBinder binder = new LocalBinder();


    // -------------------------------------------------------------- //
    // ------------------------ WS Listeners ------------------------ //
    // -------------------------------------------------------------- //

    private Listener onConnection = new Listener() {
        @Override
        public void call(Object... args) {
            Log.d(TAG, "WS onConnection");
            for (Object arg : args) {
                Log.d(TAG, "call: " + arg.toString());
            }
        }
    };

    private Listener onConnectError = new Listener() {
        @Override
        public void call(Object... args) {
            Log.d(TAG, "WS onConnectError");
            for (Object arg : args) {
                Log.d(TAG, "call: " + arg.toString());
            }
            mainSocket.disconnect();
        }
    };

    private Listener onError = new Listener() {
        @Override
        public void call(Object... args) {
            Log.d(TAG, "WS onError");
            for (Object arg : args) {
                Log.d(TAG, "call: " + arg.toString());
            }
            mainSocket.disconnect();
        }
    };

    private Listener onConnectionForConv = new Listener() {
        @Override
        public void call(Object... args) {
            Log.d(TAG, "WS/conv onConnection");
            for (Object arg : args) {
                Log.d(TAG, "call: " + arg.toString());
            }
        }
    };

    private Listener onConnectErrorForConv = new Listener() {
        @Override
        public void call(Object... args) {
            Log.d(TAG, "WS/conv onConnectError");
            for (Object arg : args) {
                Log.d(TAG, "call: " + arg.toString());
            }
            activeConvSocket.disconnect();
        }
    };

    private Listener onErrorForConv = new Listener() {
        @Override
        public void call(Object... args) {
            Log.d(TAG, "WS/conv onError");
            for (Object arg : args) {
                Log.d(TAG, "call: " + arg.toString());
            }
            activeConvSocket.disconnect();
        }
    };

    private Listener requestHeadersListener = new Listener() {
        @Override
        public void call(Object... args) {
            @SuppressWarnings("unchecked")
            Map<String, List<String>> headers = (Map<String, List<String>>) args[0];
            // modify request headers
            headers.put("Origin", Collections.singletonList("http://localhost:4200"));
            List<String> cookies = new ArrayList<>();
            for (HttpCookie cookie : ChatTreeApplication.getCookieManager().getCookieStore().getCookies()) {
                cookies.add(cookie.toString());
            }
            headers.put("Cookie", cookies);
        }
    };


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        activeConvSocket.disconnect();
        Log.d(TAG, "onUnbind: WS DISCONNECTED");
        return true;
    }

    public void setParams(double d) {
//        arg0=d;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");

        /*
         * Register a broadcast receiver to listen when the data are ready to be read from the local DB
         */
        localConvIdsLoaded = new HashSet<>();
        localThreadIdsLoaded = new HashSet<>();
        dataLoadedReceiver = new SyncReceiver();
        registerReceiver(dataLoadedReceiver, new IntentFilter(SyncAdapter.SYNC_CALLBACK_CONV_LOADED_ACTION));
        registerReceiver(dataLoadedReceiver, new IntentFilter(SyncAdapter.SYNC_CALLBACK_THREAD_LOADED_ACTION));

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        token = pref.getString("token", null);

        try {
            IO.Options opts = new IO.Options();
            opts.query = "token=" + token;

            mainSocket = IO.socket(BASE_URL, opts);

            mainSocket.on(Socket.EVENT_CONNECT, onConnection);
            mainSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
            mainSocket.on(Socket.EVENT_ERROR, onError);

            // Called upon transport creation.
            mainSocket.io().on(Manager.EVENT_TRANSPORT, new Listener() {
                @Override
                public void call(Object... args) {
                    Transport transport = (Transport) args[0];

                    transport.on(Transport.EVENT_REQUEST_HEADERS, requestHeadersListener);

                    transport.on(Transport.EVENT_RESPONSE_HEADERS, new Listener() {
                        @Override
                        public void call(Object... args) {
                            @SuppressWarnings("unchecked")
                            Map<String, List<String>> headers = (Map<String, List<String>>) args[0];
                            // access response headers
//                            Log.d(TAG, "call: headers: " + headers.toString());
                            List<String> setCookieHeader = headers.get("Set-Cookie");
                            if (setCookieHeader != null) {
                                String cookie = setCookieHeader.get(0);
                                Log.d(TAG, "call: COOKIE: " + cookie);
                            }
                        }
                    });
                }
            });

            mainSocket.connect();

        } catch (URISyntaxException e) {
            Log.d(TAG, e.getMessage());
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");

        return START_NOT_STICKY;
    }

    public boolean localConvIsReady(int convId) {
        return localConvIdsLoaded.contains(convId);
    }

    public boolean localThreadIsReady(int threadId) {
        return localThreadIdsLoaded.contains(threadId);
    }

    private Listener onCreateMessage = new Listener() {
        @Override
        public void call(final Object... args) {
            JSONObject data = (JSONObject) args[0];
            Message    newMessage;
            try {
                JSONObject message    = data.getJSONObject("message");
                int        messageId  = message.getInt("id");
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.CANADA_FRENCH);
                newMessage = new Message(
                        messageId,
                        message.getInt("author"),
                        dateFormat.parse(message.getString("date").replaceAll("Z$", "+0000")),
                        message.getString("content"),
                        message.getInt("thread")
                );

                // Update the database
                MessageDao messageDao = AppDatabase.getInstance(getApplicationContext()).messageDao();
                messageDao.insertAll(Collections.singletonList(newMessage));

                // Send the event
                Intent newMessageIntent = new Intent();
                newMessageIntent.setAction(WS_NEW_MESSAGE_ACTION);
                newMessageIntent.putExtra(EXTRA_THREAD_ID, message.getInt("thread"));
                newMessageIntent.putExtra(EXTRA_MESSAGE_ID, messageId);
                getApplicationContext().sendBroadcast(newMessageIntent);

            } catch (JSONException | ParseException e) {
                e.printStackTrace();
            }
        }
    };

    public void connectToConvNsp(int convId) {
        try {
            IO.Options opts = new IO.Options();
            opts.query = "token=" + token;

            activeConvSocket = IO.socket(BASE_URL + "conv-" + convId, opts);

            activeConvSocket.on(Socket.EVENT_CONNECT, onConnectionForConv);
            activeConvSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectErrorForConv);
            activeConvSocket.on(Socket.EVENT_ERROR, onErrorForConv);
            activeConvSocket.on(WS_EVENT_CREATE_MESSAGE, onCreateMessage);

            // Called upon transport creation.
            activeConvSocket.io().on(Manager.EVENT_TRANSPORT, new Listener() {
                @Override
                public void call(Object... args) {
                    Transport transport = (Transport) args[0];

                    transport.on(Transport.EVENT_REQUEST_HEADERS, requestHeadersListener);
                }
            });

            activeConvSocket.connect();

        } catch (URISyntaxException e) {
            Log.d(TAG, e.getMessage());
        }
    }

    public void joinThreadRoom(int threadId) {
        activeConvSocket.emit(WS_EVENT_JOIN_THREAD_ROOM, threadId);
    }

    public class SyncReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case SyncAdapter.SYNC_CALLBACK_CONV_LOADED_ACTION:
                    localConvIdsLoaded.add(intent.getIntExtra(EXTRA_SYNC_CONV_ID, 0));
                    break;
                case SyncAdapter.SYNC_CALLBACK_THREAD_LOADED_ACTION:
                    localThreadIdsLoaded.add(intent.getIntExtra(EXTRA_SYNC_THREAD_ID, 0));
                    break;
            }
        }
    }

    // ------------------------------------------------------------ //
    // ---------------------- WS USER EVENTS ---------------------- //
    // ------------------------------------------------------------ //

    public void sendMessage(String content) {
        JSONObject newMessage = new JSONObject();
        try {
            newMessage.put("message", new JSONObject().put("content", content));
            activeConvSocket.emit(WS_EVENT_CREATE_MESSAGE, newMessage);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

}
