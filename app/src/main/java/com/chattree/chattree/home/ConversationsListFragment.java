package com.chattree.chattree.home;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import com.chattree.chattree.R;
import com.chattree.chattree.db.Conversation;
import com.chattree.chattree.db.ConversationDao;
import com.chattree.chattree.db.User;
import com.chattree.chattree.home.conversation.ConversationActivity;
import com.chattree.chattree.home.conversation.ConversationItem;
import com.chattree.chattree.tools.Utils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ConversationsListFragment extends Fragment {

    private List<ConversationItem>  conversationsList;
    private ConversationListAdapter adapter;

    private ProgressBar mProgressBar;
    private ListView    conversationsListView;

    public static final String EXTRA_CONVERSATION_TITLE          = "com.chattree.chattree.CONVERSATION_TITLE";
    public static final String EXTRA_CONVERSATION_ID             = "com.chattree.chattree.CONVERSATION_ID";
    public static final String EXTRA_CONVERSATION_ROOT_THREAD_ID = "com.chattree.chattree.CONVERSATION_ROOT_THREAD_ID";

    private Intent startConvActivityLastIntent;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout resource that'll be returned
        View rootView = inflater.inflate(R.layout.fragment_conversations_list, container, false);

        mProgressBar = rootView.findViewById(R.id.list_convs_progress);

        startConvActivityLastIntent = null;

        // List of conversations
        conversationsList = new ArrayList<>();

        conversationsListView = rootView.findViewById(R.id.list_view);
        adapter = new ConversationListAdapter(getContext(), R.layout.row_conversation, conversationsList);
        conversationsListView.setAdapter(adapter);
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
                Intent intent = new Intent(getContext(), ContactListCheckActivity.class);
                startActivity(intent);
            }
        });

        return rootView;
    }

    void initConvsList(List<ConversationDao.CustomConversationUser> customConversationUsers) {
        int              lastConvId = 0;
        ConversationItem convItem   = null;
        User             member;

        SharedPreferences pref   = PreferenceManager.getDefaultSharedPreferences(getContext());
        int               userId = pref.getInt("user_id", 0);

        for (ConversationDao.CustomConversationUser convData : customConversationUsers) {
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

        adapter.notifyDataSetChanged();
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
        mProgressBar.animate().setDuration(shortAnimTime).alpha(0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressBar.setVisibility(View.GONE);
            }
        });
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
}
