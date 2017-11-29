package com.chattree.chattree.websocket;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.*;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import io.socket.client.IO;
import io.socket.client.Socket;

import java.net.URISyntaxException;

import static com.chattree.chattree.network.NetworkFragment.BASE_URL;
import static io.socket.emitter.Emitter.*;

public class UpdaterService extends Service {
    private final IBinder binder = new MyBinder();

    private final String TAG = "UpdaterService";

    private Socket mSocket;

    private Listener onConnection = new Listener() {
        @Override
        public void call(Object... args) {
            for (Object arg : args) {
                Log.d(TAG, "onConnection: " + arg.toString());
            }
        }
    };

    private Listener onConnectError = new Listener() {
        @Override
        public void call(Object... args) {
            for (Object arg : args) {
                Log.d(TAG, "onConnectError: " + arg.toString());
            }
            mSocket.disconnect();
        }
    };

    private Listener onError = new Listener() {
        @Override
        public void call(Object... args) {
            for (Object arg : args) {
                Log.d(TAG, "onError: " + arg.toString());
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


    @Override
    public IBinder onBind(Intent arg0) {
        return binder;
    }

    public void doServiceStuff() {
        serviceTask.execute();
    }

    // create an inner Binder class
    public class MyBinder extends Binder {
        public UpdaterService getService() {
            return UpdaterService.this;
        }
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "SERVICE CREATED");

        SharedPreferences pref  = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String            token = pref.getString("token", null);

        try {

            IO.Options opts = new IO.Options();
            opts.forceNew = true;
            opts.query = "token=" + token;

//            mSocket = IO.socket(BASE_URL + "?token=" + token);
            mSocket = IO.socket(BASE_URL, opts);

            mSocket.on("connect", onConnection);
            mSocket.on("connect_error", onConnectError);
            mSocket.on("error", onError);

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
