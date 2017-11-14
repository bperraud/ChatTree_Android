package com.chattree.chattree.home;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.chattree.chattree.R;

public class ConversationsListFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout resource that'll be returned
        View rootView = inflater.inflate(R.layout.fragment_conversations_list, container, false);

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
