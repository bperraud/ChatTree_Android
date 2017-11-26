package com.chattree.chattree.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.chattree.chattree.R;
import com.chattree.chattree.home.conversation.ConversationItem;

import java.util.List;

public class ConversationListAdapter extends ArrayAdapter<ConversationItem> {

    ConversationListAdapter(Context context, int resource, List<ConversationItem> conversations) {
        super(context, resource, conversations);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v = convertView;

        if (v == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            v = vi.inflate(R.layout.row_conversation, parent, false);
        }

        ConversationItem conv             = getItem(position);
        TextView         convName         = v.findViewById(R.id.nameTextView);
        TextView         convMembersLabel = v.findViewById(R.id.overviewTextView);
        ImageView        convPicture      = v.findViewById(R.id.picture);
        if (conv.getTitle() != null) {
            convName.setText(conv.getTitle());
            convMembersLabel.setText(conv.getMemberLabelsFormated());
        } else {
            convName.setText(conv.getMemberLabelsFormated());
            convMembersLabel.setText("");
        }

        // Set default picture
        if (conv.getPicture() == null) {
            convPicture.setImageResource(R.drawable.cat_avatar);
        }

        return v;
    }
}