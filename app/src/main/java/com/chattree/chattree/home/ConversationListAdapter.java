package com.chattree.chattree.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.chattree.chattree.R;

import java.util.List;

public class ConversationListAdapter extends ArrayAdapter<String> {

    ConversationListAdapter(Context context, int resource, List<String> conversations) {
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

        String conv = getItem(position);
        ((TextView) v.findViewById(R.id.nameTextView)).setText(conv);

        return v;
    }
}