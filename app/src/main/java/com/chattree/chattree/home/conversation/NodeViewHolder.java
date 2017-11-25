package com.chattree.chattree.home.conversation;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import com.chattree.chattree.R;
import com.github.johnkil.print.PrintView;
import com.unnamed.b.atv.model.TreeNode;

public class NodeViewHolder extends TreeNode.BaseNodeViewHolder<NodeViewHolder.IconTreeItem> {
    private TextView  tvValue;
    private PrintView arrowView;

    public NodeViewHolder(Context context) {
        super(context);
    }

    @Override
    public View createNodeView(final TreeNode node, IconTreeItem value) {
        final LayoutInflater inflater = LayoutInflater.from(context);
        final View           view     = inflater.inflate(R.layout.layout_thread_node, null, false);
        tvValue = view.findViewById(R.id.node_value);
        tvValue.setText(value.text);

        // Tags
        ((TextView) view.findViewById(R.id.tag_labels)).setText("Tag1, Tag2");

        final PrintView iconView = view.findViewById(R.id.icon);
        iconView.setIconText(context.getResources().getString(value.icon));

        arrowView = view.findViewById(R.id.arrow_icon);
        if (node.getChildren().size() == 0) {
            arrowView.setVisibility(View.INVISIBLE);
        }


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
//
//        view.findViewById(R.id.btn_delete).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                getTreeView().removeNode(node);
//            }
//        });

        return view;
    }

    @Override
    public void toggle(boolean active) {
        arrowView.setIconText(context.getResources().getString(active ? R.string.ic_keyboard_arrow_down : R.string.ic_keyboard_arrow_right));
    }

    public static class IconTreeItem {
        public int    icon;
        public String text;

        public IconTreeItem(int icon, String text) {
            this.icon = icon;
            this.text = text;
        }
    }
}