package com.chattree.chattree.home.conversation;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;

import com.chattree.chattree.R;


public class Thread_Detail extends Fragment {
    // TODO: Rename parameter arguments, choose names that match


    private ListView liste;
    private EditText my_Message;


    public Thread_Detail() {
        // Required empty public constructor
    }


    // TODO: Rename and change types and number of parameters


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        View view = inflater.inflate(R.layout.fragment_thread__detail, container, false);
        liste = (ListView) view.findViewById(R.id.liste);
        my_Message = (EditText) view.findViewById(R.id.messageEditText);
        ChatArrayAdapter chatArrayAdapter = new ChatArrayAdapter(   getContext(), R.layout.messagefrommyself);

        chatArrayAdapter.add(new ChatMessage("Allo!!\nWhat up!?\nça va?!", true));
        chatArrayAdapter.add(new ChatMessage("Tu viens ce soir à la chicha à Magog?!", true));
        chatArrayAdapter.add(new ChatMessage("Non ça va po!\nEt je viens pas",false,1, "Marie"));
        chatArrayAdapter.add(new ChatMessage("Tu saoules", true));
        chatArrayAdapter.add(new ChatMessage("tu fais trop la go qui connait pas charo!!\nTabernaque!", true));
        chatArrayAdapter.add(new ChatMessage("Vas-y je ne répondrai même pas tchiip", false, 1, "Marie"));
        liste.setAdapter(chatArrayAdapter);

        return view;

    }
}
