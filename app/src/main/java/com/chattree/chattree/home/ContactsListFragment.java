package com.chattree.chattree.home;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import com.chattree.chattree.R;

import java.util.ArrayList;
import java.util.List;

public class ContactsListFragment extends Fragment {

    private List<String>       contactsList;
    private ContactListAdapter adapter;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout resource that'll be returned
        View rootView = inflater.inflate(R.layout.fragment_contacts_list, container, false);

        // List of conversations
        contactsList = new ArrayList<>();
        contactsList.add("CONTACT 1");
        contactsList.add("CONTACT 2");
        contactsList.add("CONTACT 3");
        contactsList.add("CONTACT 4");
        contactsList.add("CONTACT 5");
        contactsList.add("CONTACT 6");
        contactsList.add("CONTACT 7");
        contactsList.add("CONTACT 8");
        contactsList.add("CONTACT 9");

        ListView conversationsListView = rootView.findViewById(R.id.list_view);
        adapter = new ContactListAdapter(getContext(), R.layout.row_contact, contactsList);
        conversationsListView.setAdapter(adapter);
        conversationsListView.setEmptyView(rootView.findViewById(android.R.id.empty));

        conversationsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                // TODO: access the detail of the contact
                Toast.makeText(getContext(), "GO TO CONTACT " + ++position, Toast.LENGTH_SHORT).show();
            }
        });

        // New contact FAB
        FloatingActionButton newContactFAB = rootView.findViewById(R.id.new_contact_fab);
        newContactFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "NEW CONTACT", Toast.LENGTH_SHORT).show();
            }
        });

        return rootView;
    }


}
