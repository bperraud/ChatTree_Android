package com.chattree.chattree.home;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.chattree.chattree.R;
import com.chattree.chattree.db.ConversationDao;
import com.chattree.chattree.home.conversation.ConversationActivity;

import java.util.ArrayList;
import java.util.List;

public class ConversationsListFragment extends Fragment {

    private List<String>            conversationsList;
    private ConversationListAdapter adapter;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout resource that'll be returned
        View rootView = inflater.inflate(R.layout.fragment_conversations_list, container, false);

        // List of conversations
        conversationsList = new ArrayList<>();
        conversationsList.add("CONV 1");
        conversationsList.add("CONV 2");
        conversationsList.add("CONV 3");
        conversationsList.add("CONV 4");
        conversationsList.add("CONV 5");
        conversationsList.add("CONV 6");
        conversationsList.add("CONV 7");
        conversationsList.add("CONV 8");
        conversationsList.add("CONV 9");

        ListView conversationsListView = rootView.findViewById(R.id.list_view);
        adapter = new ConversationListAdapter(getContext(), R.layout.row_conversation, conversationsList);
        conversationsListView.setAdapter(adapter);
        conversationsListView.setEmptyView(rootView.findViewById(android.R.id.empty));

        conversationsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                Intent intent = new Intent(getContext(), ConversationActivity.class);
                startActivity(intent);
            }
        });

        // New conversation FAB
        FloatingActionButton newConvFAB = rootView.findViewById(R.id.new_conv_fab);
        newConvFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "NEW CONVERSATION", Toast.LENGTH_SHORT).show();
            }
        });

        return rootView;
    }

    void refreshListOfConv(List<ConversationDao.CustomConversationUser> customConversationUsers) {
        Log.d("TEST", "refreshListOfConv: !!!");


        int lastConvId = 0;

        for (ConversationDao.CustomConversationUser conversationUser : customConversationUsers) {
            Log.d("TEST", "doInBackground: " + conversationUser.c_id);
            Log.d("TEST", "doInBackground: " + conversationUser.u_id);

            conversationsList.add(String.valueOf(conversationUser.c_id));

            // New conversation found
            if (conversationUser.c_id != lastConvId) {
//                conversation = new Conversation(row);
//                member       = new User(row);
//                conversation.members.push(member);
                lastConvId = conversationUser.c_id;
//                conversations.push(conversation);
            }
            // Conversation member found
            else {
//                member = new User(row);
//                conversation.members.push(member);
            }

            adapter.notifyDataSetChanged();
        }
    }
}
