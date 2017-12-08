package com.chattree.chattree.home.conversation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import com.chattree.chattree.R;
import com.chattree.chattree.db.AppDatabase;
import com.chattree.chattree.db.MessageDao;
import com.chattree.chattree.db.User;
import com.chattree.chattree.tools.Utils;
import com.chattree.chattree.websocket.WebSocketCaller;
import com.chattree.chattree.websocket.WebSocketService;
import com.github.johnkil.print.PrintView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.chattree.chattree.db.MessageDao.CustomMessageWithUser;
import static com.chattree.chattree.home.conversation.MessageItem.OwnerValues.ME;
import static com.chattree.chattree.home.conversation.MessageItem.OwnerValues.OTHER;

public class ThreadDetailFragment extends Fragment implements View.OnClickListener {

    static final String BUNDLE_THREAD_ID = "com.chattree.chattree.BUNDLE_THREAD_ID";

    private static final String TAG = "THREAD DETAIL FRAGMENT";

    private View      progressBar;
    private View      emptyMessages;
    private ListView  messagesListView;
    private EditText  mMessage;
    private PrintView sendMessageBtn;

    private List<MessageItem>   messagesList;
    private MessagesListAdapter messagesListAdapter;

    private int     userId;
    private int     threadId;
    private boolean isInit;
    /**
     * True if a sync process has finished and we need to refresh the view
     */
    private boolean pendingRefresh;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout resource that'll be returned
        View rootView = inflater.inflate(R.layout.fragment_thread_detail, container, false);
        messagesListView = rootView.findViewById(R.id.messagesListView);
        mMessage = rootView.findViewById(R.id.messageEditText);
        sendMessageBtn = rootView.findViewById(R.id.sendMessageButton);
        progressBar = rootView.findViewById(R.id.thread_detail_progress);
        emptyMessages = rootView.findViewById(R.id.emptyMessages);

        sendMessageBtn.setOnClickListener(this);

        threadId = getArguments().getInt(BUNDLE_THREAD_ID);
        isInit = false;
        pendingRefresh = false;

        // Retrieve the user id
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getContext());
        userId = pref.getInt("user_id", 0);

        // List of messages
        messagesList = new ArrayList<>();
        messagesListAdapter = new MessagesListAdapter(getContext(), messagesList);
        messagesListView.setAdapter(messagesListAdapter);

//        if (getActivity().getClass() == ConversationActivity.class) {
//            ConversationActivity activity = (ConversationActivity) getActivity();
//            activity.attemptJoinThreadRoom();
//
//            // Attempt to init the thread if possible
//            if (activity.getWebSocketService() != null && activity.getWebSocketService().localThreadIsReady(threadId)) {
//                refreshThread();
//            }
//        } else if (getActivity().getClass() == ThreadActivity.class) {
//            initThread();
//        }
        initThread();

        return rootView;
    }


    // TODO: replace the printView with a drawable and pressed state
    @Override
    public void onClick(View view) {
        if (view.getId() == sendMessageBtn.getId()) {
            String msgContent = mMessage.getText().toString();

            // Don't send an empty message
            if (msgContent.isEmpty()) return;

//            WebSocketService wsService = ((WebSocketCaller) getActivity()).getWebSocketService();
            ((WebSocketCaller) getActivity()).attemptToSendMessage(msgContent);

//            assert wsService != null;
//            wsService.sendMessage(msgContent);
//            mMessage.setText("");
        }
    }

    private void initThread() {
        new AsyncTask<Void, Void, List<CustomMessageWithUser>>() {
            @Override
            protected List<CustomMessageWithUser> doInBackground(Void... params) {
                MessageDao messageDao = AppDatabase.getInstance(getContext()).messageDao();
                return messageDao.getMessageWithUserByThreadId(threadId);
            }

            @Override
            protected void onPostExecute(List<CustomMessageWithUser> messages) {
                initMessagesList(messages);
            }
        }.execute();
    }

    private void initMessagesList(List<CustomMessageWithUser> messages) {
        for (CustomMessageWithUser m : messages) {
            messagesList.add(new MessageItem(
                    m.m_id,
                    m.m_fk_thread_parent,
                    m.m_fk_author,
                    m.m_content,
                    m.m_creation_date,
                    Utils.getLabelFromUser(new User(m.u_id, m.u_login, m.u_email, m.u_firstname, m.u_lastname, m.u_pp)),
                    m.m_fk_author == userId ? ME : OTHER
            ));
        }

        messagesListAdapter.notifyDataSetChanged();
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
        progressBar.animate().setDuration(shortAnimTime).alpha(0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                progressBar.setVisibility(View.GONE);
            }
        });

        messagesListView.setSelection(messagesListAdapter.getCount() - 1);
        messagesListView.setEmptyView(emptyMessages);

        isInit = true;
        if (pendingRefresh) {
            refreshThread();
        }
    }

    public void refreshThread() {
        if (!isInit) {
            pendingRefresh = true;
            return;
        }

        new AsyncTask<Void, Void, List<CustomMessageWithUser>>() {
            @Override
            protected List<CustomMessageWithUser> doInBackground(Void... params) {
                int        maxMsgId   = Collections.max(messagesListAdapter.messageIds);
                MessageDao messageDao = AppDatabase.getInstance(getContext()).messageDao();
                return messageDao.getMessageWithUserByThreadIdAndOffset(threadId, maxMsgId);
            }

            @Override
            protected void onPostExecute(List<CustomMessageWithUser> messages) {
                refreshMessagesList(messages);
            }
        }.execute();
    }

    private void refreshMessagesList(List<CustomMessageWithUser> messages) {
        for (CustomMessageWithUser m : messages) {
            if (messagesListAdapter.messageIds.contains(m.m_id)) continue;

            messagesList.add(new MessageItem(
                    m.m_id,
                    m.m_fk_thread_parent,
                    m.m_fk_author,
                    m.m_content,
                    m.m_creation_date,
                    Utils.getLabelFromUser(new User(m.u_id, m.u_login, m.u_email, m.u_firstname, m.u_lastname, m.u_pp)),
                    m.m_fk_author == userId ? ME : OTHER
            ));
        }
        messagesListAdapter.notifyDataSetChanged();
        messagesListView.setSelection(messagesListAdapter.getCount() - 1);

        pendingRefresh = false;
    }

    public void addMessageToView(final int msgId) {
        // Check first if we already have displayed the msg
        if (messagesListAdapter.messageIds.contains(msgId)) return;

        // Retrieve the message from db and add it to the view
        new AsyncTask<Void, Void, CustomMessageWithUser>() {
            @Override
            protected CustomMessageWithUser doInBackground(Void... params) {
                MessageDao messageDao = AppDatabase.getInstance(getContext()).messageDao();
                return messageDao.findById(msgId);
            }

            @Override
            protected void onPostExecute(CustomMessageWithUser m) {
                messagesList.add(new MessageItem(
                        m.m_id,
                        m.m_fk_thread_parent,
                        m.m_fk_author,
                        m.m_content,
                        m.m_creation_date,
                        Utils.getLabelFromUser(new User(m.u_id, m.u_login, m.u_email, m.u_firstname, m.u_lastname, m.u_pp)),
                        m.m_fk_author == userId ? ME : OTHER
                ));
                messagesListAdapter.notifyDataSetChanged();

                // If we are the author of the message, scroll down in the list view
                if (m.m_fk_author == userId) {
                    messagesListView.smoothScrollToPosition(messagesListAdapter.getCount());
                }
            }
        }.execute();
    }
}