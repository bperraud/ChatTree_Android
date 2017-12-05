package com.chattree.chattree.home.conversation;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.chattree.chattree.R;
import com.chattree.chattree.db.AppDatabase;
import com.chattree.chattree.db.Thread;
import com.chattree.chattree.db.ThreadDao;
import com.chattree.chattree.home.conversation.ThreadNodeViewHolder.ThreadTreeItem;
import com.unnamed.b.atv.model.TreeNode;
import com.unnamed.b.atv.view.AndroidTreeView;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;

import java.util.Collection;
import java.util.List;

import static com.chattree.chattree.home.conversation.ThreadActivity.EXTRA_THREAD_ID;
import static com.chattree.chattree.home.conversation.ThreadActivity.EXTRA_THREAD_NAME;

public class ConversationTreeFragment extends Fragment {

    static final String BUNDLE_CONV_ID        = "com.chattree.chattree.BUNDLE_CONV_ID";
    static final String BUNDLE_ROOT_THREAD_ID = "com.chattree.chattree.BUNDLE_ROOT_THREAD_ID";

    private static final String TAG = "CONVERSATION TREE";

    private AndroidTreeView treeView;
    private TreeNode        root;

    private int     convId;
    private int     rootThreadId;
    private boolean isInit;

    private List<Thread> threads;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "ON CREATE VIEW");
        View      rootView      = inflater.inflate(R.layout.fragment_conversation_tree, null, false);
        ViewGroup containerView = rootView.findViewById(R.id.container);

        convId = getArguments().getInt(BUNDLE_CONV_ID);
        rootThreadId = getArguments().getInt(BUNDLE_ROOT_THREAD_ID);
        if (rootThreadId == 0)
            throw new RuntimeException("rootThreadId not found, 0 given as default, convId: " + convId);
        isInit = false;

        root = TreeNode.root();

        treeView = new AndroidTreeView(getActivity(), root);
        treeView.setDefaultAnimation(true);
        treeView.setUse2dScroll(true);
        treeView.setDefaultContainerStyle(R.style.TreeNodeStyleCustom);
        treeView.setDefaultViewHolder(ThreadNodeViewHolder.class);
        treeView.setUseAutoToggle(false);
        treeView.setDefaultNodeClickListener(new TreeNode.TreeNodeClickListener() {
            @Override
            public void onClick(TreeNode node, Object value) {
                Intent intent = new Intent(getContext(), ThreadActivity.class);

                ThreadTreeItem item = (ThreadTreeItem) value;
                intent.putExtra(EXTRA_THREAD_ID, item.thread.getId());
                intent.putExtra(EXTRA_THREAD_NAME, item.thread.getTitle());
                //intent.putExtra(EXTRA_NAME_CONV, getArguments().getString("CONV_TITLE"));

                startActivity(intent);
            }
        });
        treeView.setDefaultNodeLongClickListener(new TreeNode.TreeNodeLongClickListener() {
            @Override
            public boolean onLongClick(TreeNode node, Object value) {
                Toast.makeText(getContext(), "long click", Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        containerView.addView(treeView.getView());

        if (savedInstanceState != null) {
            String state = savedInstanceState.getString("tState");
            if (!TextUtils.isEmpty(state)) {
                treeView.restoreState(state);
            }
        }

        // New contact FAB
        FloatingActionButton newThreadFAB = rootView.findViewById(R.id.new_thread_fab);
        newThreadFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "NEW THREAD", Toast.LENGTH_SHORT).show();
            }
        });

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("tState", treeView.getSaveState());
    }

    public void initConvTree(int i) {
        if (isInit) return;
        isInit = true;

        new AsyncTask<Void, Void, List<Thread>>() {
            @Override
            protected List<Thread> doInBackground(Void... params) {
                ThreadDao threadDao = AppDatabase.getInstance(getContext()).threadDao();
                return threadDao.findByConvId(convId);
            }

            @Override
            protected void onPostExecute(List<Thread> threads) {
                buildConvTree(threads);
            }
        }.execute();
    }

    private TreeNode buildNodes(Thread parent) {
        // Build the node corresponding to parent
        TreeNode threadNode = new TreeNode(new ThreadTreeItem(R.string.ic_messenger, parent));

        // Find the children of the parent
        for (Thread thread : threads) {
            if (thread.getId() != rootThreadId && thread.getFk_thread_parent() == parent.getId()) {
                threadNode.addChild(buildNodes(thread));
            }
        }

        // If we are dealing with the direct children of the root thread, add them
        if (parent.getId() != rootThreadId && parent.getFk_thread_parent() == rootThreadId) {
            treeView.addNode(root, threadNode);
        }

        return threadNode;
    }

    private void buildConvTree(List<Thread> threadList) {
        threads = threadList;

        Collection result = CollectionUtils.select(threads, new Predicate() {
            @Override
            public boolean evaluate(Object object) {
                Thread thread = (Thread) object;
                return thread.getId() == rootThreadId;
            }
        });

        Thread rootThread = (Thread) result.toArray()[0];
        buildNodes(rootThread);

        // No threads except the root one
        if (threads.size() == 1) {
            getView().findViewById(R.id.emptyThreads).setVisibility(View.VISIBLE);
        }
    }
}