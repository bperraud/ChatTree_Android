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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConversationListAdapter extends ArrayAdapter<ConversationItem> {

    Set<Integer> conversationsIds;

    ConversationListAdapter(Context context, int resource, List<ConversationItem> conversations) {
        super(context, resource, conversations);
        conversationsIds = new HashSet<>();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v = convertView;

        if (v == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            v = vi.inflate(R.layout.row_conversation, parent, false);
        }

        ConversationItem convItem         = getItem(position);
        TextView         convName         = v.findViewById(R.id.contactPseudoTextView);
        TextView         convMembersLabel = v.findViewById(R.id.overviewTextView);
        ImageView        convPicture      = v.findViewById(R.id.picture);
        if (convItem.getTitle() != null) {
            convName.setText(convItem.getTitle());
            convMembersLabel.setText(convItem.getMemberLabelsFormated());
        } else {
            convName.setText("Conversation avec " + convItem.getMemberLabelsFormated());
            convMembersLabel.setText("");
        }

        // Set default picture
        if (convItem.getPicture() == null) {
            convPicture.setImageResource(R.drawable.cat_avatar);
        }

        // Add the msg id to the set
        conversationsIds.add(convItem.getId());

        return v;
    }
}