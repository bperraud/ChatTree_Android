package com.chattree.chattree.home;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.chattree.chattree.R;
import com.chattree.chattree.db.User;
import com.chattree.chattree.network.NetworkFragment;
import com.chattree.chattree.tools.JSONMessageParser;
import com.chattree.chattree.tools.Utils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static com.chattree.chattree.network.NetworkFragment.HTTP_METHOD_GET;


public class ContactsListCheckFragment extends Fragment {
    private static final String GET_USERS_URL_PATH      = "api/get-users";
    static final         String EXTRA_CONTACTS_IDS_LIST = "com.chattree.chattree.EXTRA_CONTACTS_IDS_LIST";

    private FloatingActionButton validateContactsFAB;

    private List<User>               contactsList;
    private ContactsListCheckAdapter adapter;
    private ListView                 contactsListView;

    private ProgressBar mProgressBar;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout resource that'll be returned
        final View rootView = inflater.inflate(R.layout.fragment_contact_check, container, false);

        mProgressBar = rootView.findViewById(R.id.list_contacts_progress);

        contactsListView = rootView.findViewById(R.id.list_view);

        contactsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                CheckBox checkBox = view.findViewById(R.id.checkbox_contact_fragment);
                checkBox.setChecked(!checkBox.isChecked());
//                updateValidateContactsFAB();
            }
        });

        validateContactsFAB = rootView.findViewById(R.id.contacts_fab);
        validateContactsFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (adapter == null || adapter.positionArray.indexOf(true) == -1) {
                    getActivity().setResult(Activity.RESULT_CANCELED, new Intent());
                    getActivity().finish();
                    return;
                }

                ArrayList<Integer> contactsIds = new ArrayList<>();

                for (int i = 0; i < adapter.positionArray.size(); i++) {
                    // Skip if user isn't checked
                    if (!adapter.positionArray.get(i)) continue;

                    View     viewRow  = getViewByPosition(i, contactsListView);
                    CheckBox checkBox = viewRow.findViewById(R.id.checkbox_contact_fragment);

                    User user = (User) checkBox.getTag();
                    contactsIds.add(user.getId());
                }

                // Return the contacts ids list
                Intent returnIntent = new Intent();
                returnIntent.putExtra(EXTRA_CONTACTS_IDS_LIST, contactsIds);
                getActivity().setResult(Activity.RESULT_OK, returnIntent);
                getActivity().finish();
            }
        });

        // Request all users
        requestUsersList();

        return rootView;
    }

    void updateValidateContactsFAB() {
        validateContactsFAB.setImageDrawable(
                getResources().getDrawable(
                        (adapter.positionArray.indexOf(true) != -1 ?
                                 R.drawable.ic_check_white_24dp :
                                 R.drawable.ic_arrow_back_white_24dp),
                        getContext().getTheme())
        );
    }

    private View getViewByPosition(int pos, ListView listView) {
        final int firstListItemPosition = listView.getFirstVisiblePosition();
        final int lastListItemPosition  = firstListItemPosition + listView.getChildCount() - 1;

        if (pos < firstListItemPosition || pos > lastListItemPosition) {
            return listView.getAdapter().getView(pos, null, listView);
        } else {
            final int childIndex = pos - firstListItemPosition;
            return listView.getChildAt(childIndex);
        }
    }

    private void requestUsersList() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                InputStream        stream     = null;
                HttpsURLConnection connection = null;
                String             result     = null;
                try {
                    connection = (HttpsURLConnection) new URL(NetworkFragment.BASE_URL + GET_USERS_URL_PATH).openConnection();

                    // Timeout for reading InputStream arbitrarily set to 3000ms.
                    connection.setReadTimeout(3000);
                    // Timeout for connection.connect() arbitrarily set to 3000ms.
                    connection.setConnectTimeout(3000);
                    // Set HTTP method.
                    connection.setRequestMethod(HTTP_METHOD_GET);
                    connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    SharedPreferences pref  = PreferenceManager.getDefaultSharedPreferences(getContext());
                    String            token = pref.getString("token", null);
                    connection.setRequestProperty("x-access-token", token);
                    connection.setDoOutput(false);
                    connection.setDoInput(true);

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
                        result = Utils.readStream(stream, 9999999);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    // Close Stream and disconnect HTTPS connection.
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
                return result;
            }

            @Override
            protected void onPostExecute(String result) {
                try {
                    JSONMessageParser jsonParser = new JSONMessageParser(result);
                    if (jsonParser.getSuccess()) {
                        // List of contacts
                        contactsList = new ArrayList<>();

                        JSONArray usersJsonArray = jsonParser.getData().getJSONArray("users");

                        for (int i = 0, size = usersJsonArray.length(); i < size; ++i) {
                            JSONObject userJsonObj = usersJsonArray.getJSONObject(i);
                            contactsList.add(new User(
                                    userJsonObj.getInt("id"),
                                    userJsonObj.isNull("login") ? null : userJsonObj.getString("login"),
                                    userJsonObj.isNull("email") ? null : userJsonObj.getString("email"),
                                    null,
                                    null,
                                    userJsonObj.isNull("profile_picture") ? null : userJsonObj.getString("profile_picture")
                            ));
                        }

                        contactsListView = getView().findViewById(R.id.list_view);
                        adapter = new ContactsListCheckAdapter(getContext(), R.id.checkbox_contact_fragment, contactsList);
                        contactsListView.setAdapter(adapter);
                        contactsListView.setEmptyView(getView().findViewById(android.R.id.empty));

                        adapter.notifyDataSetChanged();
                        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
                        mProgressBar.animate().setDuration(shortAnimTime).alpha(0).setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                mProgressBar.setVisibility(View.GONE);
                            }
                        });
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }.execute();
    }

}