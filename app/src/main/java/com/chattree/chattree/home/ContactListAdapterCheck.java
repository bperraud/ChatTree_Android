package com.chattree.chattree.home;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import android.widget.CompoundButton.OnCheckedChangeListener;
import com.chattree.chattree.R;
import com.chattree.chattree.db.User;
import com.chattree.chattree.tools.Utils;

import java.util.ArrayList;
import java.util.List;

public class ContactListAdapterCheck extends ArrayAdapter<User> {

    private List<User> contactsList;
    ArrayList<Boolean> positionArray;
    private Context ctx;

    ContactListAdapterCheck(Context context, int resource, List<User> contacts) {
        super(context, resource, contacts);
        contactsList = contacts;
        ctx = context;

        positionArray = new ArrayList<>(contacts.size());
        for (int i = 0; i < contacts.size(); i++) {
            positionArray.add(false);
        }
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        View   row = convertView;
        Holder holder;

        if (row == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            row = vi.inflate(R.layout.row_contact_checkbox, parent, false);
            holder = new Holder();
            holder.checkBox = row.findViewById(R.id.checkbox_contact_fragment);
        } else {
            holder = new Holder();
            holder.checkBox = convertView.findViewById(R.id.checkbox_contact_fragment);
            holder.checkBox.setOnCheckedChangeListener(null);
        }

        User contact = getItem(position);
        ((TextView) row.findViewById(R.id.contactPseudoTextView)).setText(Utils.getLabelFromUser(contact));
        TextView emailTextView = row.findViewById(R.id.contactEmailTextView);
        if (contact.getLogin() != null) {
            emailTextView.setText(contact.getEmail());
        } else {
            emailTextView.setText("");
        }

        ImageView contactAvatar = row.findViewById(R.id.contactAvatar);
        if (contact.getProfile_picture() != null) {
            byte[] data   = Base64.decode(contact.getProfile_picture(), Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            contactAvatar.setImageBitmap(bitmap);
        } else {
            contactAvatar.setImageResource(R.drawable.ic_account_circle_black_24dp);
        }

        holder.checkBox.setFocusable(false);
        holder.checkBox.setChecked(positionArray.get(position));
        holder.checkBox.setTag(contact);
        holder.checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                positionArray.set(position, isChecked);
            }
        });

        return row;
    }

    static class Holder {
        CheckBox checkBox;
    }
}