package com.chattree.chattree.tools;

import android.app.Activity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.chattree.chattree.R;

public class Toaster {

    // TODO: implements success and error toast
    static public void showCustomToast(Activity activity, String text, Integer duration) {

        LayoutInflater inflater = activity.getLayoutInflater();
        View           layout   = inflater.inflate(R.layout.custom_toast, (ViewGroup) activity.findViewById(R.id.custom_toast_container));

        TextView textView = layout.findViewById(R.id.text);
        textView.setText(text);

        Toast toast = new Toast(activity.getApplicationContext());
        toast.setGravity(Gravity.TOP | Gravity.CENTER, 0, 200);
        toast.setDuration(duration == null ? Toast.LENGTH_SHORT : duration);
        toast.setView(layout);
        toast.show();
    }

}
