package com.chattree.chattree.home.conversation;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.chattree.chattree.R;
import com.chattree.chattree.websocket.WebSocketService;
import com.github.johnkil.print.PrintView;

public class ThreadDetailFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "THREAD DETAIL FRAGMENT";

    private ListView  listView;
    private EditText  mMessage;
    private PrintView sendMessageBtn;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout resource that'll be returned
        View rootView = inflater.inflate(R.layout.fragment_thread_detail, container, false);
        listView = rootView.findViewById(R.id.messagesListView);
        mMessage = rootView.findViewById(R.id.messageEditText);
        sendMessageBtn = rootView.findViewById(R.id.sendMessageButton);

        sendMessageBtn.setOnClickListener(this);


        ChatArrayAdapter chatArrayAdapter = new ChatArrayAdapter(getContext(), R.layout.messagefrommyself);

        chatArrayAdapter.add(new ChatMessage("Allo!!\nWhat up!?\nça va?!", true));
        chatArrayAdapter.add(new ChatMessage("Tu viens ce soir à la chicha à Magog?!", true));
        chatArrayAdapter.add(new ChatMessage("Non ça va po!\nEt je viens pas", false, 1, "Marie"));
        chatArrayAdapter.add(new ChatMessage("Tu saoules", true));
        chatArrayAdapter.add(new ChatMessage("tu fais trop la go qui connait pas charo!!\nTabernaque!", true));
        chatArrayAdapter.add(new ChatMessage("Vas-y je ne répondrai même pas tchiip", false, 1, "Marie"));
        chatArrayAdapter.add(new ChatMessage("Tu viens ce soir à la chicha à Magog?!", true));
        chatArrayAdapter.add(new ChatMessage("Non ça va po!\nEt je viens pas", false, 1, "Marie"));
        chatArrayAdapter.add(new ChatMessage("Tu saoules", true));
        chatArrayAdapter.add(new ChatMessage("tu fais trop la go qui connait pas charo!!\nTabernaque!", true));
        chatArrayAdapter.add(new ChatMessage("Vas-y je ne répondrai même pas tchiip", false, 1, "Marie"));
        listView.setAdapter(chatArrayAdapter);
        listView.setSelection(chatArrayAdapter.getCount() - 1);

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
}