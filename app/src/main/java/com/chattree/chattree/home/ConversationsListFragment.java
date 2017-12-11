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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import com.chattree.chattree.R;
import com.chattree.chattree.db.AppDatabase;
import com.chattree.chattree.db.Conversation;
import com.chattree.chattree.db.ConversationDao;
import com.chattree.chattree.db.ConversationDao.CustomConversationUser;
import com.chattree.chattree.db.User;
import com.chattree.chattree.home.conversation.ConversationActivity;
import com.chattree.chattree.home.conversation.ConversationItem;
import com.chattree.chattree.tools.Utils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.chattree.chattree.home.ContactsListCheckFragment.EXTRA_CONTACTS_IDS_LIST;

public class ConversationsListFragment extends Fragment {
    public static final int REQUEST_CODE_CONTACTS_RETURNED = 1;

    public static final String EXTRA_CONVERSATION_TITLE          = "com.chattree.chattree.CONVERSATION_TITLE";
    public static final String EXTRA_CONVERSATION_ID             = "com.chattree.chattree.CONVERSATION_ID";
    public static final String EXTRA_CONVERSATION_ROOT_THREAD_ID = "com.chattree.chattree.CONVERSATION_ROOT_THREAD_ID";

    private List<ConversationItem>  conversationsList;
    private ConversationListAdapter conversationListAdapter;

    private ProgressBar mProgressBar;
    private View        emptyConversations;
    private ListView    conversationsListView;


    private Intent startConvActivityLastIntent;

    private int userId;

    private boolean isInit;
    /**
     * True if a sync process has finished and we need to refresh the view
     */
    private boolean pendingRefresh;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout resource that'll be returned
        View rootView = inflater.inflate(R.layout.fragment_conversations_list, container, false);

        mProgressBar = rootView.findViewById(R.id.list_convs_progress);
        emptyConversations = rootView.findViewById(R.id.emptyConversations);

        startConvActivityLastIntent = null;

        isInit = false;
        pendingRefresh = false;

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getContext());
        userId = pref.getInt("user_id", 0);

        // List of conversations
        conversationsList = new ArrayList<>();

        conversationsListView = rootView.findViewById(R.id.list_view);
        conversationListAdapter = new ConversationListAdapter(getContext(), R.layout.row_conversation, conversationsList);
        conversationsListView.setAdapter(conversationListAdapter);
        conversationsListView.setEmptyView(rootView.findViewById(android.R.id.empty));

        conversationsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                Intent           intent   = new Intent(getContext(), ConversationActivity.class);
                ConversationItem convItem = conversationsList.get(position);
                intent.putExtra(EXTRA_CONVERSATION_ID, convItem.getId());
                intent.putExtra(EXTRA_CONVERSATION_TITLE, convItem.getTitle());
                intent.putExtra(EXTRA_CONVERSATION_ROOT_THREAD_ID, convItem.getRootThreadId());
                if (convItem.getRootThreadId() == 0) {
                    startConvActivityLastIntent = intent;
                    int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
                    mProgressBar.animate().setDuration(shortAnimTime).alpha(1).setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mProgressBar.setVisibility(View.VISIBLE);
                        }
                    });
                    conversationsListView.animate().setDuration(shortAnimTime).alpha(0.5f);
                } else {
                    startActivity(intent);
                }
            }
        });

        // New conversation FAB
        FloatingActionButton newConvFAB = rootView.findViewById(R.id.new_conv_fab);
        newConvFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), ContactsListCheckActivity.class);
                startActivityForResult(intent, REQUEST_CODE_CONTACTS_RETURNED);
            }
        });

        initConvs();

        return rootView;
    }

    private void initConvs() {
        new AsyncTask<Void, Void, List<CustomConversationUser>>() {
            @Override
            protected List<CustomConversationUser> doInBackground(Void... params) {
                ConversationDao conversationDao = AppDatabase.getInstance(getContext()).conversationDao();
                return conversationDao.getCustomConversationUsers();
            }

            @Override
            protected void onPostExecute(List<CustomConversationUser> messages) {
                initConvsList(messages);
            }
        }.execute();
    }

    private void initConvsList(List<CustomConversationUser> customConversationUsers) {
        parseCustomConversationUserAndAddToConvList(customConversationUsers);

        conversationListAdapter.notifyDataSetChanged();
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
        mProgressBar.animate().setDuration(shortAnimTime).alpha(0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressBar.setVisibility(View.GONE);
            }
        });

        conversationsListView.setEmptyView(emptyConversations);

        isInit = true;
        if (pendingRefresh) {
            refreshConvs();
        }
    }

    public void refreshConvs() {
        if (!isInit) {
            pendingRefresh = true;
            return;
        }

        new AsyncTask<Void, Void, List<CustomConversationUser>>() {
            @Override
            protected List<CustomConversationUser> doInBackground(Void... params) {
                int maxConvId = conversationListAdapter.conversationsIds.size() > 0 ?
                        Collections.max(conversationListAdapter.conversationsIds) : -1;
                ConversationDao conversationDao = AppDatabase.getInstance(getContext()).conversationDao();
                return conversationDao.getCustomConversationUsersByOffset(maxConvId);
            }

            @Override
            protected void onPostExecute(List<CustomConversationUser> conversations) {
                refreshConvsList(conversations);
            }
        }.execute();
    }

    private void refreshConvsList(List<CustomConversationUser> conversations) {
        parseCustomConversationUserAndAddToConvList(conversations);
        conversationListAdapter.notifyDataSetChanged();
        pendingRefresh = false;
    }

    private void parseCustomConversationUserAndAddToConvList(List<CustomConversationUser> conversations) {
        int              lastConvId = 0;
        ConversationItem convItem   = null;
        User             member;

        for (CustomConversationUser convData : conversations) {
            // New conversation found
            if (convData.c_id != lastConvId) {

                convItem = new ConversationItem(convData.c_id, convData.c_title, convData.c_picture, convData.c_fk_root_thread);

                if (userId != convData.u_id) {
                    member = new User(
                            convData.u_id,
                            convData.u_login,
                            convData.u_email,
                            convData.u_firstname,
                            convData.u_lastname,
                            null
                    );
                    convItem.addMemberLabel(Utils.getLabelFromUser(member));
                }

                conversationsList.add(convItem);
                lastConvId = convData.c_id;
            }
            // Conversation member found
            else if (userId != convData.u_id) {
                member = new User(
                        convData.u_id,
                        convData.u_login,
                        convData.u_email,
                        convData.u_firstname,
                        convData.u_lastname,
                        null
                );
                assert convItem != null;
                convItem.addMemberLabel(Utils.getLabelFromUser(member));
            }
        }
    }

    void updateRootThreadOfConv(final Conversation conversation) {
        Collection result = CollectionUtils.select(conversationsList, new Predicate() {
            @Override
            public boolean evaluate(Object object) {
                ConversationItem conversationItem = (ConversationItem) object;
                return conversationItem.getId() == conversation.getId();
            }
        });

        ((ConversationItem) result.toArray()[0]).setRootThreadId(conversation.getFk_root_thread());

        if (startConvActivityLastIntent != null &&
            startConvActivityLastIntent.getIntExtra(EXTRA_CONVERSATION_ID, 0) == conversation.getId()) {

            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
            mProgressBar.animate().setDuration(shortAnimTime).alpha(0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressBar.setVisibility(View.GONE);
                }
            });
            conversationsListView.animate().setDuration(shortAnimTime).alpha(1f);

            startConvActivityLastIntent.putExtra(EXTRA_CONVERSATION_ROOT_THREAD_ID, conversation.getFk_root_thread());
            startActivity(startConvActivityLastIntent);
            startConvActivityLastIntent = null;
        }
    }

    // ------------------------------------------------------------------- //
    // ----------------------- CREATE CONVERSATION ----------------------- //
    // ------------------------------------------------------------------- //

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_CONTACTS_RETURNED) {
            if (resultCode == Activity.RESULT_OK) {
                ArrayList<Integer> contactIds = data.getIntegerArrayListExtra(EXTRA_CONTACTS_IDS_LIST);
                Log.d("CONV LIST FRAG", "onActivityResult: ok, " + contactIds);

                
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Log.d("CONV LIST FRAG", "onActivityResult: canceled");
            }
        }
    }
}
