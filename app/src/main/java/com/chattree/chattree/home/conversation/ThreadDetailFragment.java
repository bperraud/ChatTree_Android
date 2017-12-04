package com.chattree.chattree.home.conversation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.chattree.chattree.R;
import com.chattree.chattree.db.AppDatabase;
import com.chattree.chattree.db.Message;
import com.chattree.chattree.db.MessageDao;
import com.chattree.chattree.db.User;
import com.chattree.chattree.home.conversation.MessageItem.OwnerValues;
import com.chattree.chattree.tools.Utils;
import com.chattree.chattree.websocket.WebSocketService;
import com.github.johnkil.print.PrintView;

import java.util.ArrayList;
import java.util.List;

import static com.chattree.chattree.db.MessageDao.*;
import static com.chattree.chattree.home.conversation.MessageItem.OwnerValues.*;

public class ThreadDetailFragment extends Fragment implements View.OnClickListener {

    static final String BUNDLE_THREAD_ID = "com.chattree.chattree.BUNDLE_THREAD_ID";

    private static final String TAG = "THREAD DETAIL FRAGMENT";

    private View      progressBar;
    private ListView  messagesListView;
    private EditText  mMessage;
    private PrintView sendMessageBtn;

    private List<MessageItem>   messagesList;
    private MessagesListAdapter messagesListAdapter;

    private int     threadId;
    private boolean isInit;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "ON CREATE VIEW");

        // Inflate the layout resource that'll be returned
        View rootView = inflater.inflate(R.layout.fragment_thread_detail, container, false);
        messagesListView = rootView.findViewById(R.id.messagesListView);
        mMessage = rootView.findViewById(R.id.messageEditText);
        sendMessageBtn = rootView.findViewById(R.id.sendMessageButton);
        progressBar = rootView.findViewById(R.id.thread_detail_progress);

        sendMessageBtn.setOnClickListener(this);

        threadId = getArguments().getInt(BUNDLE_THREAD_ID);
        isInit = false;

        // List of messages
        messagesList = new ArrayList<>();

        messagesListAdapter = new MessagesListAdapter(getContext(), messagesList);
        messagesListView.setAdapter(messagesListAdapter);

        if (getActivity().getClass() == ConversationActivity.class) {
            ((ConversationActivity) getActivity()).attemptJoinThreadRoom();
            initThread();
        }

        return rootView;
    }


    // TODO: replace the printView with a drawable and pressed state
    @Override
    public void onClick(View view) {
        if (view.getId() == sendMessageBtn.getId()) {

            if (mMessage.getText().toString().isEmpty())
                return;

            Toast.makeText(this.getContext(), "To send:\n" + mMessage.getText(), Toast.LENGTH_LONG).show();

            WebSocketService wsService = ((ConversationActivity) getActivity()).getWsService();

            wsService.sendMessage(mMessage.getText().toString());
        }
    }

    public void initThread() {
        if (isInit) return;
        isInit = true;

        new AsyncTask<Void, Void, List<CustomMessageWithUser>>() {
            @Override
            protected List<CustomMessageWithUser> doInBackground(Void... params) {
                MessageDao messageDao = AppDatabase.getInstance(getContext()).messageDao();
                return messageDao.getMessageWithUserByThreadId(threadId);
            }

            @Override
            protected void onPostExecute(List<CustomMessageWithUser> messages) {
                refreshListOfMessages(messages);
            }
        }.execute();
    }

    private void refreshListOfMessages(List<CustomMessageWithUser> messages) {
        SharedPreferences pref   = PreferenceManager.getDefaultSharedPreferences(getContext());
        int               userId = pref.getInt("user_id", 0);
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
        messagesListView.setEmptyView(getView().findViewById(R.id.emptyMessages));
    }
}