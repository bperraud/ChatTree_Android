package com.chattree.chattree.datasync;

import android.accounts.Account;
import android.arch.persistence.room.Dao;
import android.content.*;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import com.chattree.chattree.db.*;
import com.chattree.chattree.network.NetworkFragment;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.chattree.chattree.home.HomeActivity.SYNC_CALLBACK_INTENT_ACTION;

/**
 * Handle the transfer of data between a server and an
 * app, using the Android sync adapter framework.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

    public static final String EXTRA_SYNC_STATUS       = "SyncStatus";
    public static final String EXTRA_SYNC_STATUS_START = "START";
    public static final String EXTRA_SYNC_STATUS_DONE  = "DONE";

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
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        Log.i("SyncAdapter", "SYNCING DATA...");

        /*
         * Put the data transfer code here.
         */
        fullSync();

    }

    private void fullSync() {

        Intent intent = new Intent();
        intent.setAction(SYNC_CALLBACK_INTENT_ACTION);
        intent.putExtra(EXTRA_SYNC_STATUS, EXTRA_SYNC_STATUS_START);
        getContext().sendBroadcast(intent);

        URL url = null;
        try {
            url = new URL(NetworkFragment.BASE_URL + "api/get-conversations");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        try {
            String result = requestUrl(url, NetworkFragment.HTTP_METHOD_GET, null);
            Log.i("SyncAdapter", "RESULT : " + result);

            syncConversationsWithLocalDB(result);

            intent.putExtra(EXTRA_SYNC_STATUS, EXTRA_SYNC_STATUS_DONE);
            getContext().sendBroadcast(intent);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String requestUrl(URL url, String httpMethod, String body) throws IOException {
        InputStream        stream     = null;
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
            SharedPreferences pref  = PreferenceManager.getDefaultSharedPreferences(this.getContext());
            String            token = pref.getString("token", null);
            connection.setRequestProperty("x-access-token", token);

            if (body == null)
                connection.setDoOutput(false);
            else
                connection.setDoOutput(true);
            // Already true by default but setting just in case; needs to be true since this request
            // is carrying an input (response) body.
            connection.setDoInput(true);

            if (body != null) {
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
                result = readStream(stream, 50000);
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
        Reader        reader    = new InputStreamReader(stream, "UTF-8");
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

    private void syncConversationsWithLocalDB(String jsonData) {

        try {
            JSONObject   json                  = new JSONObject(jsonData);
            JSONArray    convJsonArray         = json.getJSONObject("data").getJSONArray("conversations");
            Set<Integer> userIdSet             = new HashSet<>();
            Integer      conversationUserIndex = 0;

            ConversationDao conversationDao = AppDatabase.getInstance(getContext()).conversationDao();
            UserDao         userDao         = AppDatabase.getInstance(getContext()).userDao();

            ArrayList<Conversation> convsToInsertOrUpdate = new ArrayList<>();
            ArrayList<User> usersToInsertOrUpdate = new ArrayList<>();
            ArrayList<ConversationUser> convUsersToInsert = new ArrayList<>();

            List<Conversation> localConvs = conversationDao.findAll();
            List<User> localUsers = userDao.getAll();
            List<ConversationUser> localConvUsers = conversationDao.findAllConversationUser();

            for (int i = 0, size = convJsonArray.length(); i < size; ++i) {
                JSONObject convJsonObj = convJsonArray.getJSONObject(i);
                int        convId      = convJsonObj.getInt("id");

                int localConvIndex = getConvById(convId,localConvs);
                Conversation receivedConv = new Conversation(
                        convId,
                        convJsonObj.isNull("fk_root_thread") ? null : convJsonObj.getInt("fk_root_thread"),
                        convJsonObj.isNull("title") ? null : convJsonObj.getString("title"),
                        convJsonObj.isNull("picture") ? null : convJsonObj.getString("picture")
                );
                if((localConvIndex==-1) || (!localConvs.get(localConvIndex).equals(receivedConv))){
                    //conv does not exist locally or is different
                    convsToInsertOrUpdate.add(receivedConv);
                }
                localConvs.remove(receivedConv);

                JSONArray membersJsonArray = convJsonObj.getJSONArray("members");

                for (int j = 0, membersSize = membersJsonArray.length(); j < membersSize; ++j) {
                    JSONObject memberJsonObj = membersJsonArray.getJSONObject(j);
                    int        memberId      = memberJsonObj.getInt("id");

                    int localMemberIndex = getUserById(memberId,localUsers);
                    User receivedMember = new User(
                            memberId,
                            memberJsonObj.isNull("login") ? null : memberJsonObj.getString("login"),
                            memberJsonObj.isNull("email") ? null : memberJsonObj.getString("email"),
                            memberJsonObj.isNull("firstname") ? null : memberJsonObj.getString("firstname"),
                            memberJsonObj.isNull("lastname") ? null : memberJsonObj.getString("lastname"),
                            memberJsonObj.isNull("profile_picture") ? null : memberJsonObj.getString("profile_picture")
                    );

                    if((localMemberIndex==-1) || (!localUsers.get(localMemberIndex).equals(receivedMember))){
                        //member does not exist locally or is different
                        // check member is not already in the list
                        if (!usersToInsertOrUpdate.contains(receivedMember)) {
                            usersToInsertOrUpdate.add(receivedMember);
                        }
                    }
                    localUsers.remove(receivedMember);


                    ConversationUser receivedConvUser = new ConversationUser(convId,memberId);
                    int localConvUserIndex = localConvUsers.indexOf(receivedConvUser);
                    if(localConvUserIndex == -1){
                        convUsersToInsert.add(receivedConvUser);
                    }
                    localConvUsers.remove(receivedConvUser);
                }
            }

            //DELETES
            conversationDao.deleteConversationUsers(localConvUsers);
            System.out.println(localConvUsers.size()+" conv users deleted");
            conversationDao.deleteAll(localConvs);
            System.out.println(localConvs.size() + " convs deleted");
            userDao.deleteAll(localUsers);
            System.out.println(localUsers.size() + " users deleted");

            //INSERTS OR UPDATES
            conversationDao.insertAll(convsToInsertOrUpdate);
            System.out.println(convsToInsertOrUpdate.size() + " convs inserted");
            userDao.insertAll(usersToInsertOrUpdate);
            System.out.println(usersToInsertOrUpdate.size() + " users inserted");
            conversationDao.insertConversationUsers(convUsersToInsert);
            System.out.println(convUsersToInsert.size()+" conv users inserted");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    private int getConvById(int id, List<Conversation> l){
        for(int i=0; i<l.size(); i++){
            if(l.get(i).getId() == id)
                return i;
        }
        return -1;
    }

    private int getUserById(int id, List<User> l){
        for(int i=0; i<l.size(); i++){
            if(l.get(i).getId() == id)
                return i;
        }
        return -1;
    }

    private void insertConversationsIntoLocalDB(String jsonData) {

        try {
            JSONObject   json                  = new JSONObject(jsonData);
            JSONArray    convJsonArray         = json.getJSONObject("data").getJSONArray("conversations");
            Set<Integer> userIdSet             = new HashSet<>();
            Integer      conversationUserIndex = 0;

            ConversationDao conversationDao = AppDatabase.getInstance(getContext()).conversationDao();
            UserDao         userDao         = AppDatabase.getInstance(getContext()).userDao();

            for (int i = 0, size = convJsonArray.length(); i < size; ++i) {
                JSONObject convJsonObj = convJsonArray.getJSONObject(i);
                int        convId      = convJsonObj.getInt("id");

                Conversation conv = new Conversation(
                        convId,
                        convJsonObj.isNull("fk_root_thread") ? null : convJsonObj.getInt("fk_root_thread"),
                        convJsonObj.isNull("title") ? null : convJsonObj.getString("title"),
                        convJsonObj.isNull("picture") ? null : convJsonObj.getString("picture")
                );

                conversationDao.insertAll(conv);

                JSONArray membersJsonArray = convJsonObj.getJSONArray("members");

                for (int j = 0, membersSize = membersJsonArray.length(); j < membersSize; ++j) {
                    JSONObject memberJsonObj = membersJsonArray.getJSONObject(j);
                    int        memberId      = memberJsonObj.getInt("id");

                    // Skip the insertion of the element to prevent duplicata
                    if (userIdSet.add(memberId)) {
                        User member = new User(
                                memberId,
                                memberJsonObj.isNull("login") ? null : memberJsonObj.getString("login"),
                                memberJsonObj.isNull("email") ? null : memberJsonObj.getString("email"),
                                memberJsonObj.isNull("firstname") ? null : memberJsonObj.getString("firstname"),
                                memberJsonObj.isNull("lastname") ? null : memberJsonObj.getString("lastname"),
                                memberJsonObj.isNull("profile_picture") ? null : memberJsonObj.getString("profile_picture")
                        );

                        userDao.insertAll(member);
                    }

                    ConversationUser conversationUser = new ConversationUser(
                            conversationUserIndex++,
                            convId,
                            memberId
                    );
                    conversationDao.insertConversationUsers(conversationUser);
                }
            }


        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
