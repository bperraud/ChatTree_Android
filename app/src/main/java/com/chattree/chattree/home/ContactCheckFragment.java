package com.chattree.chattree.home;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import android.content.Intent;

import com.chattree.chattree.R;


public class ContactCheckFragment extends Fragment {
    private List<String> contactsList;
    private ContactListAdapterCheck adapter;
    private ListView conversationsListView;



    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout resource that'll be returned
        final View rootView = inflater.inflate(R.layout.fragment_contact_check, container, false);

        // List of contacts
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
        contactsList.add("CONTACT 10");

        conversationsListView = rootView.findViewById(R.id.list_view);
        adapter = new ContactListAdapterCheck(getContext(), R.layout.row_contact_checkbox, contactsList);
        conversationsListView.setAdapter(adapter);


        conversationsListView.setEmptyView(rootView.findViewById(android.R.id.empty));



        conversationsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                TextView Contactname = (TextView) view.findViewById(R.id.nameTextView);
                CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkboxcontact);

                checkBox.setChecked(!checkBox.isChecked());

            }
        });



        FloatingActionButton newContactFAB = rootView.findViewById(R.id.new_contact_fab);
        newContactFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Adapter adapter2 = conversationsListView.getAdapter();
                String resultat = "Check:";
                for(int i =0; i<adapter2.getCount(); i++){

                    View viewrow = getViewByPosition(i,conversationsListView);

                    CheckBox checkBox = (CheckBox) viewrow.findViewById(R.id.checkboxcontact);
                    TextView name = (TextView) viewrow.findViewById(R.id.nameTextView);

                    if(checkBox.isChecked()) {
                        resultat += name.getText()+ "|";
                    }

                }
                Toast.makeText(getContext(), resultat, Toast.LENGTH_SHORT).show();
                //on envoi la liste vers la home activity
                 Intent intent = new Intent(getActivity(), HomeActivity.class);
                intent.putExtra("list", resultat);

            }
        });

        return rootView;
    }

    public View getViewByPosition(int pos, ListView listView) {
        final int firstListItemPosition = listView.getFirstVisiblePosition();
        final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;

        if (pos < firstListItemPosition || pos > lastListItemPosition ) {
            return listView.getAdapter().getView(pos, null, listView);
        } else {
            final int childIndex = pos - firstListItemPosition;
            return listView.getChildAt(childIndex);
        }
    }



}