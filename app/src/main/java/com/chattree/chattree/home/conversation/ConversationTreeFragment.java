package com.chattree.chattree.home.conversation;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.chattree.chattree.R;
import com.chattree.chattree.home.conversation.NodeViewHolder.IconTreeItem;
import com.unnamed.b.atv.model.TreeNode;
import com.unnamed.b.atv.view.AndroidTreeView;

public class ConversationTreeFragment extends Fragment {
    private AndroidTreeView tView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View      rootView      = inflater.inflate(R.layout.fragment_conversation_tree, null, false);
        ViewGroup containerView = rootView.findViewById(R.id.container);

        TreeNode root = TreeNode.root();

        TreeNode thread1    = new TreeNode(new IconTreeItem(R.string.ic_messenger, "Fil 1"));
        TreeNode subThread1 = new TreeNode(new IconTreeItem(R.string.ic_messenger, "Fil 1.1"));
        TreeNode subThread2 = new TreeNode(new IconTreeItem(R.string.ic_messenger, "Fil 1.2"));
        TreeNode subThread3 = new TreeNode(new IconTreeItem(R.string.ic_messenger, "Fil 1.3"));
        TreeNode subThread4 = new TreeNode(new IconTreeItem(R.string.ic_messenger, "Fil 1.4"));

        thread1.addChildren(subThread1, subThread2, subThread3, subThread4);

        TreeNode thread2 = new TreeNode(new IconTreeItem(R.string.ic_messenger, "Fil 2"));

        root.addChildren(thread1, thread2);

        tView = new AndroidTreeView(getActivity(), root);
        tView.setDefaultAnimation(true);
        tView.setDefaultContainerStyle(R.style.TreeNodeStyleCustom);
        tView.setDefaultViewHolder(NodeViewHolder.class);

        containerView.addView(tView.getView());

        if (savedInstanceState != null) {
            String state = savedInstanceState.getString("tState");
            if (!TextUtils.isEmpty(state)) {
                tView.restoreState(state);
            }
        }

        return rootView;
    }

}