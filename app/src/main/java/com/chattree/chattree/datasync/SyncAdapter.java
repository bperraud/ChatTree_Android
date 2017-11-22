package com.chattree.chattree.datasync;

import android.accounts.Account;
import android.content.*;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.util.Base64;
import android.util.Log;
import com.chattree.chattree.network.NetConnectCallback;
import com.chattree.chattree.network.NetworkFragment;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Handle the transfer of data between a server and an
 * app, using the Android sync adapter framework.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

    // Global variables
    // Define a variable to contain a content resolver instance
    ContentResolver mContentResolver;

    /**
     * Set up the sync adapter
     */
    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        /*
         * If your app uses a content resolver, get an instance of it
         * from the incoming Context
         */
        mContentResolver = context.getContentResolver();
    }

    /**
     * Set up the sync adapter. This form of the
     * constructor maintains compatibility with Android 3.0
     * and later platform versions
     */
    public SyncAdapter(
            Context context,
            boolean autoInitialize,
            boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        /*
         * If your app uses a content resolver, get an instance of it
         * from the incoming Context
         */
        mContentResolver = context.getContentResolver();

    }

    /*
     * Specify the code you want to run in the sync adapter. The entire
     * sync adapter runs in a background thread, so you don't have to set
     * up your own background processing.
     */
    @Override
    public void onPerformSync(Account account,Bundle extras,String authority,ContentProviderClient provider,SyncResult syncResult) {
        Log.i("SyncAdapter", "SYNCING DATA...");
        /*
         * Put the data transfer code here.
         */
        fullSync();

    }

    private void fullSync(){
        URL url = null;
        try {
            url = new URL(NetworkFragment.BASE_URL+"api/get-conversations");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        try {
            String result = requestUrl(url, NetworkFragment.HTTP_METHOD_GET, null);
            Log.i("SyncAdapter", "RESULT : " + result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String requestUrl(URL url, String httpMethod, String body) throws IOException {
        InputStream stream     = null;
        HttpsURLConnection connection = null;
        String             result     = null;
        try {
            connection = (HttpsURLConnection) url.openConnection();

            // Timeout for reading InputStream arbitrarily set to 3000ms.
            connection.setReadTimeout(3000);
            // Timeout for connection.connect() arbitrarily set to 3000ms.
            connection.setConnectTimeout(3000);
            // Set HTTP method.
            connection.setRequestMethod(httpMethod);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this.getContext());
            String token = pref.getString("token", null);
            connection.setRequestProperty("x-access-token", token);
            //String basicAuth = "Bearer " + new String(Base64.encode(token.getBytes(), android.util.Base64.NO_WRAP));
            //connection.setRequestProperty("Authorization", basicAuth);

            if(body == null)
                connection.setDoOutput(false);
            else
                connection.setDoOutput(true);
            // Already true by default but setting just in case; needs to be true since this request
            // is carrying an input (response) body.
            connection.setDoInput(true);

            if(body != null) {
                OutputStream os = connection.getOutputStream();
                os.write(body.getBytes("UTF-8"));
                os.close();
            }

            // Open communications link (network traffic occurs here).
            connection.connect();
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpsURLConnection.HTTP_OK) {
                throw new IOException("HTTP error code: " + responseCode);
            }
            // Retrieve the response body as an InputStream.
            stream = connection.getInputStream();
            if (stream != null) {
                // Converts Stream to String with max length.
                result = readStream(stream, 2000);
            }
        } finally {
            // Close Stream and disconnect HTTPS connection.
            if (stream != null) {
                stream.close();
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
        return result;
    }

    /**
     * Converts the contents of an InputStream to a String.
     */
    String readStream(InputStream stream, int maxReadSize) throws IOException {
        Reader reader    = new InputStreamReader(stream, "UTF-8");
        char[]        rawBuffer = new char[maxReadSize];
        int           readSize;
        StringBuilder buffer    = new StringBuilder();
        while (((readSize = reader.read(rawBuffer)) != -1) && maxReadSize > 0) {
            if (readSize > maxReadSize) {
                readSize = maxReadSize;
            }
            buffer.append(rawBuffer, 0, readSize);
            maxReadSize -= readSize;
        }
        return buffer.toString();
    }
}
