package com.chattree.chattree.home.conversation;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.chattree.chattree.R;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import static com.chattree.chattree.home.conversation.MessageItem.*;

/**
 * Created by steveamadodias on 19/11/2017.
 */


public class MessagesListAdapter extends ArrayAdapter<MessageItem> {

    MessagesListAdapter(Context context, List<MessageItem> messages) {
        super(context, 0, messages);
    }

    // Return an integer representing the type by fetching the enum type ordinal
    @Override
    public int getItemViewType(int position) {
        return getItem(position).getOwner().ordinal();
    }

    // Total number of types is the number of enum values
    @Override
    public int getViewTypeCount() {
        return OwnerValues.values().length;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        MessageItem messageItem = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            // Get the data item type for this position
            int type = getItemViewType(position);
            // Inflate XML layout based on the type
            convertView = getInflatedLayoutForType(type);
        }

        // Lookup view for data population
        if (messageItem.getOwner() == OwnerValues.OTHER) {
            TextView pseudo = convertView.findViewById(R.id.pseudo);
            pseudo.setText(messageItem.getPseudo());
        }

        TextView content = convertView.findViewById(R.id.message);
        if (content != null) {
            content.setText(messageItem.getContent());
        }

        String dateFormatted = new SimpleDateFormat("HH:mm", Locale.CANADA_FRENCH).format(messageItem.getDate());
        TextView time = convertView.findViewById(R.id.time);
        if (time != null) {
            time.setText(dateFormatted);
        }

        // Return the completed view to render on screen
        return convertView;
    }

    // Given the item type, responsible for returning the correct inflated XML layout file
    private View getInflatedLayoutForType(int type) {
        if (type == OwnerValues.ME.ordinal()) {
            return LayoutInflater.from(getContext()).inflate(R.layout.row_message_me, null);
        } else if (type == OwnerValues.OTHER.ordinal()) {
            return LayoutInflater.from(getContext()).inflate(R.layout.row_message_other, null);
        } else {
            return null;
        }
    }
}

