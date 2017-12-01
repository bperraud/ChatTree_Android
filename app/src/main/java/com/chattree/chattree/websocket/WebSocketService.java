package com.chattree.chattree.websocket;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.*;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import io.socket.client.IO;
import io.socket.client.Manager;
import io.socket.client.Socket;
import io.socket.engineio.client.Transport;

import java.net.URISyntaxException;
import java.util.*;

import static com.chattree.chattree.network.NetworkFragment.BASE_URL;
import static io.socket.emitter.Emitter.*;

public class WebSocketService extends Service {

    private final String TAG = "WEBSOCKET SERVICE";

    private Socket mSocket;

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
            mSocket.disconnect();
        }
    };

    private Listener onError = new Listener() {
        @Override
        public void call(Object... args) {
            Log.d(TAG, "WS onError");
            for (Object arg : args) {
                Log.d(TAG, "call: " + arg.toString());
            }
        }
    };

//    private Listener onNewMessage = new Listener() {
//        @Override
//        public void call(final Object... args) {
//            JSONObject data = (JSONObject) args[0];
//            String     username;
//            String     message;
//            try {
//                username = data.getString("username");
//                message = data.getString("message");
//            } catch (JSONException e) {
//                return;
//            }
//
//            Log.d(TAG, username);
//            Log.d(TAG, message);
//        }
//    };


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setParams(double d){
//        arg0=d;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");

        return START_NOT_STICKY;
    }

    public void doServiceStuff() {
        serviceTask.execute();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");

        SharedPreferences pref  = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String            token = pref.getString("token", null);

        try {
            IO.Options opts = new IO.Options();
            opts.forceNew = true;
            opts.query = "token=" + token;

            mSocket = IO.socket(BASE_URL, opts);

            mSocket.on(Socket.EVENT_CONNECT, onConnection);
            mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
            mSocket.on(Socket.EVENT_ERROR, onError);

            // Called upon transport creation.
            mSocket.io().on(Manager.EVENT_TRANSPORT, new Listener() {
                @Override
                public void call(Object... args) {
                    Transport transport = (Transport) args[0];

                    transport.on(Transport.EVENT_REQUEST_HEADERS, new Listener() {
                        @Override
                        public void call(Object... args) {
                            @SuppressWarnings("unchecked")
                            Map<String, List<String>> headers = (Map<String, List<String>>) args[0];
                            // modify request headers
//                            Log.d(TAG, "call: COOKIE: " + headers.get("Cookie"));
//                            headers.put("Cookie", "foo=1;");
//                            headers.put("Cookie", "io=TfYgzxX9Y3A12NWgAAAA");
                            headers.put("Origin", Collections.singletonList("http://localhost:4200"));
                        }
                    });

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

            mSocket.connect();

        } catch (URISyntaxException e) {
            Log.d(TAG, e.getMessage());
        }

    }

    private void attemptSend() {
        String message = "TOTO";
        if (TextUtils.isEmpty(message)) {
            return;
        }

        mSocket.emit("new message", message);
    }

    private AsyncTask<Void, Void, Void> serviceTask = new AsyncTask<Void, Void, Void>() {

        @Override
        protected Void doInBackground(Void... params) {
            Log.d(TAG, "long running service serviceTask");
            return null;
        }
    };
}
