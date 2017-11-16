package com.chattree.chattree.home.conversation;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.chattree.chattree.R;
import com.unnamed.b.atv.model.TreeNode;

public class NodeViewHolder extends TreeNode.BaseNodeViewHolder<NodeViewHolder.IconTreeItem> {

    public NodeViewHolder(Context context) {
        super(context);
    }

    @Override
    public View createNodeView(TreeNode node, IconTreeItem value) {
        final LayoutInflater inflater = LayoutInflater.from(context);
        final View           view     = inflater.inflate(R.layout.layout_thread_node, null, false);
        TextView             tvValue  = view.findViewById(R.id.node_value);
        tvValue.setText(value.text);

        return view;
    }

    static class IconTreeItem {
        public int    icon;
        private String text;

        public IconTreeItem(String text) {
            this.icon = icon;
            this.text = text;
        }
    }
}