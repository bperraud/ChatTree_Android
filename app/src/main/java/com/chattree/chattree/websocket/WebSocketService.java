package com.chattree.chattree.websocket;

import android.app.Service;
import android.content.*;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;
import com.chattree.chattree.ChatTreeApplication;
import com.chattree.chattree.datasync.SyncAdapter;
import com.chattree.chattree.db.*;
import com.chattree.chattree.db.Thread;
import com.chattree.chattree.network.NetworkFragment;
import io.socket.client.IO;
import io.socket.client.Manager;
import io.socket.client.Socket;
import io.socket.engineio.client.Transport;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.chattree.chattree.datasync.SyncAdapter.EXTRA_SYNC_CONV_ID;
import static com.chattree.chattree.datasync.SyncAdapter.EXTRA_SYNC_THREAD_ID;
import static com.chattree.chattree.network.NetworkFragment.BASE_URL;
import static com.chattree.chattree.network.NetworkFragment.HTTP_METHOD_GET;
import static io.socket.emitter.Emitter.Listener;
import static org.json.JSONObject.NULL;

public class WebSocketService extends Service {

    private final String TAG = "WEBSOCKET SERVICE";

    @SuppressWarnings("FieldCanBeLocal")
    private static final String WS_EVENT_CREATE_MESSAGE      = "create-message";
    private static final String WS_EVENT_CREATE_THREAD       = "create-thread";
    private static final String WS_EVENT_CREATE_CONVERSATION = "create-conversation";
    private static final String WS_EVENT_NEW_CONVERSATION    = "new-conversation";
    private static final String WS_EVENT_EDIT_THREAD         = "edit-thread";
    private static final String WS_EVENT_JOIN_THREAD_ROOM    = "join-thread-room";

    public static final String WS_NEW_MESSAGE_ACTION      = "com.chattree.chattree.WS_NEW_MESSAGE_ACTION";
    public static final String WS_NEW_THREAD_ACTION       = "com.chattree.chattree.WS_NEW_THREAD_ACTION";
    public static final String WS_NEW_CONVERSATION_ACTION = "com.chattree.chattree.WS_NEW_CONVERSATION_ACTION";
    public static final String WS_THREAD_EDITED_ACTION    = "com.chattree.chattree.WS_THREAD_EDITED_ACTION";

    public static final String EXTRA_CONV_ID    = "com.chattree.chattree.EXTRA_CONV_ID";
    public static final String EXTRA_THREAD_ID  = "com.chattree.chattree.EXTRA_THREAD_ID";
    public static final String EXTRA_MESSAGE_ID = "com.chattree.chattree.EXTRA_MESSAGE_ID";
    public static final String EXTRA_FROM_SELF  = "com.chattree.chattree.EXTRA_FROM_SELF";

    private int    userId;
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

    class onCreateMessageListener implements Listener {

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
    }

    class onCreateThreadListener implements Listener {

        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];
            Thread     newThread;

            try {
                JSONObject thread     = data.getJSONObject("thread");
                int        threadId   = thread.getInt("id");
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.CANADA_FRENCH);
                newThread = new Thread(
                        threadId,
                        dateFormat.parse(thread.getString("date").replaceAll("Z$", "+0000")),
                        null,
                        thread.getInt("author"),
                        thread.getInt("conversation"),
                        thread.getInt("thread_parent"),
                        thread.isNull("message_parent") ? null : thread.getInt("message_parent")
                );

                // Update the database
                ThreadDao threadDao = AppDatabase.getInstance(getApplicationContext()).threadDao();
                threadDao.insertAll(newThread);

                // Send the event
                Intent newThreadIntent = new Intent();
                newThreadIntent.setAction(WS_NEW_THREAD_ACTION);
                newThreadIntent.putExtra(EXTRA_CONV_ID, thread.getInt("conversation"));
                newThreadIntent.putExtra(EXTRA_THREAD_ID, threadId);
                getApplicationContext().sendBroadcast(newThreadIntent);

            } catch (JSONException | ParseException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Only for thread title edition
     */
    class onEditThreadListener implements Listener {

        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];

            try {
                JSONObject thread   = data.getJSONObject("thread");
                int        threadId = thread.getInt("id");
                String     title    = thread.getString("title");

                // Update the database
                ThreadDao threadDao = AppDatabase.getInstance(getApplicationContext()).threadDao();
                threadDao.updateTitleById(title, threadId);

                // Send the event
                Intent threadEditedIntent = new Intent();
                threadEditedIntent.setAction(WS_THREAD_EDITED_ACTION);
                threadEditedIntent.putExtra(EXTRA_CONV_ID, thread.getInt("conversation"));
                threadEditedIntent.putExtra(EXTRA_THREAD_ID, threadId);
                getApplicationContext().sendBroadcast(threadEditedIntent);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    class onCreateConversationListener implements Listener {

        boolean fromSelf;

        onCreateConversationListener(boolean fromSelf) {
            this.fromSelf = fromSelf;
        }

        @Override
        public void call(final Object... args) {
            JSONObject   data = (JSONObject) args[0];
            Conversation newConversation;
            try {
                JSONObject conversation = data.getJSONObject("conversation");
                int        convId       = conversation.getInt("id");
                int        rootThreadId = conversation.getInt("root");

                // TODO: fix the server (it should auto. create the conv nsp without a get request from the client,
                // for now, it can't because of a circularly dependency between io-server and conversation.io)
                // Sync the conversation
//                Bundle settingsBundle = new Bundle();
//                settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
//                settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
//                settingsBundle.putInt(SyncAdapter.EXTRA_SYNC_CONV_ID, convId);
//                settingsBundle.putInt("IGNORE_RESULT", 1);
//                ContentResolver.requestSync(ChatTreeApplication.getSyncAccount(getApplicationContext()), ChatTreeApplication.AUTHORITY, settingsBundle);


                try {
                    URL                url        = new URL(NetworkFragment.BASE_URL + "api/get-conv/" + convId);
                    HttpsURLConnection connection = null;
                    try {
                        connection = (HttpsURLConnection) url.openConnection();
                        connection.setReadTimeout(10000);
                        connection.setConnectTimeout(10000);
                        connection.setRequestMethod(HTTP_METHOD_GET);
                        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                        SharedPreferences pref  = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        String            token = pref.getString("token", null);
                        connection.setRequestProperty("x-access-token", token);
                        connection.setDoOutput(false);
                        connection.setDoInput(true);
                        connection.connect();
                        int responseCode = connection.getResponseCode();
                        if (responseCode != HttpsURLConnection.HTTP_OK) {
                            throw new IOException("HTTP error code: " + responseCode);
                        }
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    } finally {
                        if (connection != null) {
                            connection.disconnect();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }


                // Save the new conv id in SharedPreferences
                SharedPreferences        pref    = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor edit    = pref.edit();
                Set<String>              convIds = pref.getStringSet("conversations_ids", new HashSet<String>());
                convIds.add(String.valueOf(convId));
                edit.putStringSet("conversations_ids", convIds);
                edit.apply();

                newConversation = new Conversation(
                        convId,
                        null,
                        conversation.isNull("title") ? null : conversation.getString("title"),
                        null
                );
                JSONArray members = conversation.getJSONArray("members");

                // Update the database
                ConversationDao conversationDao = AppDatabase.getInstance(getApplicationContext()).conversationDao();
                ThreadDao       threadDao       = AppDatabase.getInstance(getApplicationContext()).threadDao();
                UserDao         userDao         = AppDatabase.getInstance(getApplicationContext()).userDao();

                // Insert the conversation
                conversationDao.insertAll(newConversation);

                // Insert the root thread
                Thread rootThread = new Thread(
                        rootThreadId,
                        null,
                        null,
                        null,
                        convId,
                        null,
                        null
                );
                threadDao.insertAll(rootThread);

                // Update the fk_root_thread of the conversation
                conversationDao.updateFKRootThreadById(rootThreadId, convId);

                // Insert the members
                int convUserIndex = conversationDao.getConversationUserMaxId();

                for (int i = 0, membersSize = members.length(); i < membersSize; ++i) {
                    JSONObject memberJsonObj = members.getJSONObject(i);
                    final int  memberId      = memberJsonObj.getInt("id");

                    User receivedMember = new User(
                            memberId,
                            memberJsonObj.isNull("login") ? null : memberJsonObj.getString("login"),
                            memberJsonObj.isNull("email") ? null : memberJsonObj.getString("email"),
                            memberJsonObj.isNull("firstname") ? null : memberJsonObj.getString("firstname"),
                            memberJsonObj.isNull("lastname") ? null : memberJsonObj.getString("lastname"),
                            memberJsonObj.isNull("profile_picture") ? null : memberJsonObj.getString("profile_picture")
                    );

                    User existingUser = userDao.findById(memberId);
                    // If member doesn't exist or is different from the received received one, insert
                    if (existingUser == null || !existingUser.equals(receivedMember)) {
                        userDao.insertAll(receivedMember);
                    }

                    ConversationUser conversationUser = new ConversationUser(++convUserIndex, convId, memberId);
                    conversationDao.insertConversationUsers(conversationUser);
                }

                // Send the event
                Intent newConversationIntent = new Intent();
                newConversationIntent.setAction(WS_NEW_CONVERSATION_ACTION);
                newConversationIntent.putExtra(EXTRA_CONV_ID, convId);
                newConversationIntent.putExtra(EXTRA_FROM_SELF, fromSelf);
                getApplicationContext().sendBroadcast(newConversationIntent);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (activeConvSocket != null) {
            activeConvSocket.disconnect();
        }
        Log.d(TAG, "onUnbind: WS DISCONNECTED");
        return true;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        userId = pref.getInt("user_id", -1);
        token = pref.getString("token", null);

        try {
            IO.Options opts = new IO.Options();
            opts.query = "token=" + token;

            mainSocket = IO.socket(BASE_URL, opts);

            mainSocket.on(Socket.EVENT_CONNECT, onConnection);
            mainSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
            mainSocket.on(Socket.EVENT_ERROR, onError);
            // TODO: uniform the calls from server for [create/new] conversation
            // (we need to add a conversation author to do that)
            mainSocket.on(WS_EVENT_CREATE_CONVERSATION, new onCreateConversationListener(true));
            mainSocket.on(WS_EVENT_NEW_CONVERSATION, new onCreateConversationListener(false));

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
//                                Log.d(TAG, "call: COOKIE: " + cookie);
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

    public void connectToConvNsp(int convId) {
        try {
            IO.Options opts = new IO.Options();
            opts.query = "token=" + token;

            activeConvSocket = IO.socket(BASE_URL + "conv-" + convId, opts);

            activeConvSocket.on(Socket.EVENT_CONNECT, onConnectionForConv);
            activeConvSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectErrorForConv);
            activeConvSocket.on(Socket.EVENT_ERROR, onErrorForConv);
            activeConvSocket.on(WS_EVENT_CREATE_MESSAGE, new onCreateMessageListener());
            activeConvSocket.on(WS_EVENT_CREATE_THREAD, new onCreateThreadListener());
            activeConvSocket.on(WS_EVENT_EDIT_THREAD, new onEditThreadListener());

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

    // ------------------------------------------------------------ //
    // ---------------------- WS USER EVENTS ---------------------- //
    // ------------------------------------------------------------ //

    public void createConversation(List<Integer> members, String title) {
        JSONObject newConv = new JSONObject();
        try {
            JSONArray membersJSON = new JSONArray();
            for (Integer id : members) {
                membersJSON.put(new JSONObject().put("id", id));
            }
            // Add user itself
            membersJSON.put(new JSONObject().put("id", userId));

            JSONObject convData = new JSONObject();
            convData.put("title", title == null ? NULL : title)
                    .put("members", membersJSON);

            newConv.put("conv", convData);
            mainSocket.emit(WS_EVENT_CREATE_CONVERSATION, newConv);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void createThread(int parentThreadId, int convId) {
        JSONObject newThread = new JSONObject();
        try {
            JSONObject threadData = new JSONObject();
            threadData.put("title", NULL)
                    .put("messages", new JSONArray())
                    .put("message_parent", NULL)
                    .put("thread_parent", parentThreadId)
                    .put("conversation", convId);

            newThread.put("thread", threadData);
            activeConvSocket.emit(WS_EVENT_CREATE_THREAD, newThread);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void editThreadTitle(int threadId, int convId, String newTitle) {
        JSONObject thread = new JSONObject();
        try {
            JSONObject threadData = new JSONObject();
            threadData.put("id", threadId)
                    .put("conversation", convId)
                    .put("title", newTitle);

            thread.put("thread", threadData);
            activeConvSocket.emit(WS_EVENT_EDIT_THREAD, thread);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

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
