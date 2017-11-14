package com.chattree.chattree.home;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.chattree.chattree.R;

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
                // TODO: access the detail of the conversation
                Toast.makeText(getContext(), "GO TO CONV " + ++position, Toast.LENGTH_SHORT).show();
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


}
