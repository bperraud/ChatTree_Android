package com.chattree.chattree.home.conversation;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.ViewSwitcher;
import com.chattree.chattree.R;
import com.chattree.chattree.db.Thread;
import com.github.johnkil.print.PrintView;
import com.unnamed.b.atv.model.TreeNode;

public class ThreadNodeViewHolder extends TreeNode.BaseNodeViewHolder<ThreadNodeViewHolder.ThreadTreeItem> {
    private static final String DEFAULT_THREAD_EMPTY_TITLE = "<Sans titre>";

    private TreeNode node;

    private ViewSwitcher   titleSwitcher;
    private TextView       titleTextView;
    private CustomEditText editTitleView;
    private PrintView      arrowView;
    /**
     * We need to keep a reference to the nodeView because getView returns a copy of it, calling
     * createNodeView even if the view already exists
     *
     * @see TreeNode.BaseNodeViewHolder#getNodeView()
     */
    private View           nodeView;

    private ConversationActivity conversationActivity;

    public ThreadNodeViewHolder(Context context) {
        super(context);
        conversationActivity = (ConversationActivity) context;
    }

    @Override
    public View createNodeView(final TreeNode node, final ThreadTreeItem threadItem) {
        final Thread         thread   = threadItem.thread;
        final LayoutInflater inflater = LayoutInflater.from(context);
        final View           view     = inflater.inflate(R.layout.layout_thread_node, null, false);

        this.node = node;

        titleSwitcher = view.findViewById(R.id.thread_title_switcher);

        titleTextView = view.findViewById(R.id.thread_title);
        titleTextView.setText(thread.getTitle() == null ? DEFAULT_THREAD_EMPTY_TITLE : thread.getTitle());

        editTitleView = view.findViewById(R.id.thread_title_edit);

        // On title validation
        editTitleView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.thread_edit_validate || id == EditorInfo.IME_NULL || id == EditorInfo.IME_ACTION_DONE) {
                    Log.d("ViewHolder", "Title edited");
                    validateTitleEdition();
                    return true;
                }
                return false;
            }
        });

        // Tags
//        ((TextView) view.findViewById(R.id.tag_labels)).setText("Tag1, Tag2");
//        final TextView tagLabelsView = view.findViewById(R.id.tag_labels);
        view.findViewById(R.id.tag_container).setVisibility(View.GONE);

        final PrintView iconView = view.findViewById(R.id.icon);
        iconView.setIconText(context.getResources().getString(threadItem.icon));

        arrowView = view.findViewById(R.id.arrow_icon);
        if (node.getChildren().size() == 0) {
            arrowView.setVisibility(View.INVISIBLE);
        }

        // Expand/collapse behaviour
        view.findViewById(R.id.arrow_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!node.isExpanded()) {
                    (node.getViewHolder().getTreeView()).expandNode(node);
                    node.setExpanded(true);
                } else {
                    (node.getViewHolder().getTreeView()).collapseNode(node);
                    node.setExpanded(false);
                }
            }
        });

        nodeView = view;
        return view;
    }

    @Override
    public void toggle(boolean active) {
        arrowView.setIconText(context.getResources().getString(active ? R.string.ic_keyboard_arrow_down : R.string.ic_keyboard_arrow_right));
    }

    public static class ThreadTreeItem {
        public int    icon;
        public Thread thread;

        ThreadTreeItem(int icon, Thread thread) {
            this.icon = icon;
            this.thread = thread;
        }
    }

    void toggleItemSelectedBackground(boolean selected) {
        // Create an array of the attributes we want to resolve
        // using values from a theme
        int[] attrs = new int[]{ R.attr.selectableItemBackground /* index 0 */ };

        // Obtain the styled attributes. 'themedContext' is a context with a
        // theme, typically the current Activity (i.e. 'this')
        TypedArray ta = conversationActivity.obtainStyledAttributes(attrs);

        // To get the value of the 'listItemBackground' attribute that was
        // set in the theme used in 'themedContext'. The parameter is the index
        // of the attribute in the 'attrs' array. The returned Drawable
        // is what you are after
        Drawable drawableFromTheme = ta.getDrawable(0 /* index */);

        // Finally, free the resources used by TypedArray
        ta.recycle();

        if (selected)
            nodeView.setBackgroundResource(R.color.extremeLightGrey);
        else nodeView.setBackground(drawableFromTheme);
    }

    void enableTitleEdition(boolean retrieveLastTitle) {
        titleSwitcher.showNext();
        editTitleView.setText(
                !retrieveLastTitle || titleTextView.getText().toString().equals(DEFAULT_THREAD_EMPTY_TITLE) ?
                        "" :
                        titleTextView.getText()
        );
        editTitleView.requestFocus();
        InputMethodManager imm = (InputMethodManager) conversationActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(editTitleView, InputMethodManager.SHOW_IMPLICIT);
    }

    void cancelTitleEdition() {
        editTitleView.setText(titleTextView.getText());
        titleSwitcher.showNext();
        editTitleView.clearFocus();

        conversationActivity.getConversationTreeFragment().clearThreadTitleBeingEdited();
    }

    void validateTitleEdition() {
        // Close the keyboard
        InputMethodManager imm = (InputMethodManager) conversationActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editTitleView.getWindowToken(), 0);

        CharSequence newTitle = editTitleView.getText();
        CharSequence oldTitle = titleTextView.getText();
        Thread       thread   = ((ThreadTreeItem) node.getValue()).thread;

        // If title isn't empty and it is different from the old one
        if (!newTitle.toString().isEmpty() && !newTitle.equals(oldTitle))
            conversationActivity.getWebSocketService().editThreadTitle(
                    thread.getId(), thread.getFk_conversation(), newTitle.toString()
            );

        titleTextView.setText(newTitle.toString().isEmpty() ? DEFAULT_THREAD_EMPTY_TITLE : newTitle);
        titleSwitcher.showNext();
        editTitleView.clearFocus();

        conversationActivity.getConversationTreeFragment().clearThreadTitleBeingEdited();
    }

    void refreshTitle(String newTitle) {
        titleTextView.setText(newTitle);
    }

    public void displayArrow() {
        if (node.getChildren().size() > 0) {
            arrowView.setVisibility(View.VISIBLE);
        }
    }
}