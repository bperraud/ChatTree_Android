package com.chattree.chattree.home.conversation;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.chattree.chattree.R;
import com.chattree.chattree.db.Thread;
import com.github.johnkil.print.PrintView;
import com.unnamed.b.atv.model.TreeNode;

public class ThreadNodeViewHolder extends TreeNode.BaseNodeViewHolder<ThreadNodeViewHolder.ThreadTreeItem> {
    private static final String DEFAULT_THREAD_EMPTY_TITLE = "<Sans titre>";

    private ConversationActivity conversationActivity;

    private TextView  threadNodeTitleTextView;
    private PrintView arrowView;
    private PrintView createThreadView;

    public ThreadNodeViewHolder(Context context) {
        super(context);
        conversationActivity = (ConversationActivity) context;
    }

    @Override
    public View createNodeView(final TreeNode node, final ThreadTreeItem threadItem) {
        final Thread         thread   = threadItem.thread;
        final LayoutInflater inflater = LayoutInflater.from(context);
        final View           view     = inflater.inflate(R.layout.layout_thread_node, null, false);
        threadNodeTitleTextView = view.findViewById(R.id.thread_title);
        threadNodeTitleTextView.setText(thread.getTitle() == null ? DEFAULT_THREAD_EMPTY_TITLE : thread.getTitle());

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
                    // If we are in thread creation state, show the "+" buttons
                    if (conversationActivity.getConversationTreeFragment().isOnThreadCreationState()) {
                        for (TreeNode treeNode : node.getChildren()) {
                            ((ThreadNodeViewHolder) treeNode.getViewHolder()).toogleCreateThread(true);
                        }
                    }
                } else {
                    (node.getViewHolder().getTreeView()).collapseNode(node);
                    node.setExpanded(false);
                }
            }
        });

        createThreadView = view.findViewById(R.id.create_thread_icon);
        createThreadView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(conversationActivity, "onClick: thread to create from " + threadItem.thread.getId(), Toast.LENGTH_SHORT).show();
                Log.d("VIEW HOLDER", "onClick: thread to create from " + threadItem.thread.getId());
            }
        });

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

    public void toogleCreateThread(boolean show) {
        createThreadView.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}