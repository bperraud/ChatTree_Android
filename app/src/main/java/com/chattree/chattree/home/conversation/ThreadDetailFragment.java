package com.chattree.chattree.home.conversation;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.chattree.chattree.R;

public class ThreadDetailFragment extends Fragment implements View.OnClickListener {
    private ListView liste;
    private EditText my_Message;
    private ImageButton sendMessage_Button;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout resource that'll be returned
        View rootView = inflater.inflate(R.layout.fragment_thread_detail, container, false);
        liste = (ListView) rootView.findViewById(R.id.liste);
        my_Message = (EditText) rootView.findViewById(R.id.messageEditText);
        sendMessage_Button = (ImageButton) rootView.findViewById(R.id.sendMessageButton);
        sendMessage_Button.setOnClickListener(this);


        ChatArrayAdapter chatArrayAdapter = new ChatArrayAdapter(   getContext(), R.layout.messagefrommyself);

        chatArrayAdapter.add(new ChatMessage("Allo!!\nWhat up!?\nça va?!", true));
        chatArrayAdapter.add(new ChatMessage("Tu viens ce soir à la chicha à Magog?!", true));
        chatArrayAdapter.add(new ChatMessage("Non ça va po!\nEt je viens pas",false,1, "Marie"));
        chatArrayAdapter.add(new ChatMessage("Tu saoules", true));
        chatArrayAdapter.add(new ChatMessage("tu fais trop la go qui connait pas charo!!\nTabernaque!", true));
        chatArrayAdapter.add(new ChatMessage("Vas-y je ne répondrai même pas tchiip", false, 1, "Marie"));
        chatArrayAdapter.add(new ChatMessage("Tu viens ce soir à la chicha à Magog?!", true));
        chatArrayAdapter.add(new ChatMessage("Non ça va po!\nEt je viens pas",false,1, "Marie"));
        chatArrayAdapter.add(new ChatMessage("Tu saoules", true));
        chatArrayAdapter.add(new ChatMessage("tu fais trop la go qui connait pas charo!!\nTabernaque!", true));
        chatArrayAdapter.add(new ChatMessage("Vas-y je ne répondrai même pas tchiip", false, 1, "Marie"));
        liste.setAdapter(chatArrayAdapter);
        liste.setSelection(chatArrayAdapter.getCount()-1);
        return rootView;
    }


    @Override
    public void onClick(View view) {
        if(view.getId() == sendMessage_Button.getId()){

            Toast.makeText(this.getContext(),"Bouton envoyé cliqué",Toast.LENGTH_LONG).show();
        }
    }
}