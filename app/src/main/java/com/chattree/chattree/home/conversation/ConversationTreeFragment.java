package com.chattree.chattree.home.conversation;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.chattree.chattree.R;
import com.chattree.chattree.home.conversation.NodeViewHolder.IconTreeItem;
import com.unnamed.b.atv.model.TreeNode;
import com.unnamed.b.atv.view.AndroidTreeView;

public class ConversationTreeFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout resource that'll be returned
        LinearLayout rootView = (LinearLayout) inflater.inflate(R.layout.fragment_conversation_tree, container, false);

        TreeNode     root       = TreeNode.root();
        IconTreeItem parentItem = new IconTreeItem("PARENT");
        TreeNode     parent     = new TreeNode(parentItem);
        IconTreeItem nodeItem1  = new IconTreeItem("FIL 1");
        TreeNode     child1     = new TreeNode(nodeItem1);
        IconTreeItem nodeItem2  = new IconTreeItem("FIL 2");
        TreeNode     child2     = new TreeNode(nodeItem2);
        IconTreeItem nodeItem3  = new IconTreeItem("FIL 3");
        TreeNode     child3     = new TreeNode(nodeItem3);
        parent.addChildren(child1, child2, child3);
        root.addChild(parent);

        AndroidTreeView tView = new AndroidTreeView(getActivity(), root);
        tView.setDefaultNodeClickListener(new TreeNode.TreeNodeClickListener() {
            @Override
            public void onClick(TreeNode node, Object value) {
                Log.d("TREE", value.toString());
            }
        });
        tView.setDefaultAnimation(true);
        tView.setDefaultViewHolder(NodeViewHolder.class);
        tView.expandLevel(1);
        rootView.addView(tView.getView());

        return rootView;
    }


}