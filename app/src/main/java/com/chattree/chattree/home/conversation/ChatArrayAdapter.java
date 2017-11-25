package com.chattree.chattree.home.conversation;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.chattree.chattree.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by steveamadodias on 19/11/2017.
 */


    class ChatArrayAdapter extends ArrayAdapter<ChatMessage> {

        private TextView chatText;
        private List<ChatMessage> chatMessageList = new ArrayList<ChatMessage>();
        private Context context;

        @Override
        public void add(ChatMessage object) {
            chatMessageList.add(object);
            super.add(object);
        }

        public ChatArrayAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
            this.context = context;
        }

        public int getCount() {
            return this.chatMessageList.size();
        }

        public ChatMessage getItem(int index) {
            return this.chatMessageList.get(index);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ChatMessage chatMessageObj = getItem(position);
            View row = convertView;
            LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            if (chatMessageObj.isFromMyself()) {
                row = inflater.inflate(R.layout.messagefrommyself, parent, false);
            }else{
                row = inflater.inflate(R.layout.messagefromother, parent, false);
                TextView pseudo = (TextView) row.findViewById(R.id.pseudo);
                pseudo.setText(chatMessageObj.getPseudo());
            }
            chatText = (TextView) row.findViewById(R.id.message);
            chatText.setText(chatMessageObj.getMessagetext());
            return row;
        }
    }

