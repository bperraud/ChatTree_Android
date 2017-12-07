package com.chattree.chattree.home.conversation;


import android.content.Context;
import android.support.v7.widget.AppCompatEditText;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.InputMethodManager;


public class CustomEditText extends AppCompatEditText {

    private Context context;

    public CustomEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // User has pressed Back key. So hide the keyboard
            InputMethodManager mgr = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            mgr.hideSoftInputFromWindow(this.getWindowToken(), 0);

            if (context.getClass() == ConversationActivity.class) {
                ((ThreadNodeViewHolder)
                         ((ConversationActivity) context).getConversationTreeFragment()
                                 .getLastSelectedNode().getViewHolder())
                        .cancelTitleEdition();
                return true;
            }

            super.onKeyPreIme(keyCode, event);
        }
        return false;
    }
}
