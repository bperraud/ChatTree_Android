package com.chattree.chattree.home.conversation;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.*;
import android.view.animation.*;
import android.widget.ImageView;
import android.widget.TextView;
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

    private AndroidTreeView      treeView;
    private TreeNode             root;
    private FloatingActionButton createNewThreadFAB;

    private int      convId;
    private int      rootThreadId;
    private boolean  isInit;
    private boolean  onThreadSelectedState;
    private TreeNode lastSelectedNode;

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

        onThreadSelectedState = false;
        lastSelectedNode = null;

        root = TreeNode.root();

        treeView = new AndroidTreeView(getActivity(), root);
        treeView.setDefaultAnimation(true);
//        treeView.setUse2dScroll(true);
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

                clearThreadSelection();
                startActivity(intent);
            }
        });
        treeView.setDefaultNodeLongClickListener(new TreeNode.TreeNodeLongClickListener() {
            @Override
            public boolean onLongClick(final TreeNode node, Object value) {
                node.setSelected(true);
                if (lastSelectedNode != null) {
                    lastSelectedNode.setSelected(false);
                    ((ThreadNodeViewHolder) lastSelectedNode.getViewHolder()).toggleItemSelectedBackground(false);
                }
                lastSelectedNode = node;

                // Node selected: change background
                ((ThreadNodeViewHolder) node.getViewHolder()).toggleItemSelectedBackground(true);

                // If we are already on the selection state, return
                if (onThreadSelectedState) return true;
                onThreadSelectedState = true;

                // Bottom panel slide up animation
                Animation bottomUp           = AnimationUtils.loadAnimation(getContext(), R.anim.bottom_up);
                ViewGroup threadEditionPanel = getView().findViewById(R.id.thread_edition_panel);
                threadEditionPanel.startAnimation(bottomUp);
                threadEditionPanel.setVisibility(View.VISIBLE);

                // Thread creation FAB slide up animation
                TranslateAnimation moveBottomUp = new TranslateAnimation(0, 0, 0, -400);
                moveBottomUp.setInterpolator(new DecelerateInterpolator());
                moveBottomUp.setStartOffset(200);
                moveBottomUp.setDuration(300);
                moveBottomUp.setFillEnabled(true);
                moveBottomUp.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) createNewThreadFAB.getLayoutParams();
                        lp.bottomMargin += 400;
                        createNewThreadFAB.setLayoutParams(lp);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
                createNewThreadFAB.startAnimation(moveBottomUp);

                return true;
            }
        });
        treeView.setSelectionModeEnabled(true);

        // TODO: see how to make node views full width
//        treeView.getView().setLayoutParams(new ViewGroup.LayoutParams(
//                ViewGroup.LayoutParams.MATCH_PARENT,
//                ViewGroup.LayoutParams.WRAP_CONTENT
//        ));
//        Log.d(TAG, "onCreateView: " + (treeView.getView().getLayoutParams().width == ViewGroup.LayoutParams.MATCH_PARENT ? "match" : "no match"));

        containerView.addView(treeView.getView());

        if (savedInstanceState != null) {
            String state = savedInstanceState.getString("tState");
            if (!TextUtils.isEmpty(state)) {
                treeView.restoreState(state);
            }
        }

        // New thread FAB
        createNewThreadFAB = rootView.findViewById(R.id.new_thread_fab);
        createNewThreadFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createNewThread();
            }
        });

        initBottomToolbar(rootView);

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("tState", treeView.getSaveState());
    }

    public void initConvTree() {
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

    // ----------------------------------------------------------------------- //
    // --------------------------- THREAD CREATION --------------------------- //
    // ----------------------------------------------------------------------- //

    private void createNewThread() {
        // TODO: createNewThread
        Toast.makeText(getContext(), "New thread to create", Toast.LENGTH_SHORT).show();
    }

    boolean isOnThreadSelectedState() {
        return onThreadSelectedState;
    }

    void clearThreadSelection() {
        if (lastSelectedNode != null) {
            lastSelectedNode.setSelected(false);
            ((ThreadNodeViewHolder) lastSelectedNode.getViewHolder()).toggleItemSelectedBackground(false);
        }

        onThreadSelectedState = false;

        // Bottom panel slide up animation
        Animation bottomDown         = AnimationUtils.loadAnimation(getContext(), R.anim.bottom_down);
        ViewGroup threadEditionPanel = getView().findViewById(R.id.thread_edition_panel);
        threadEditionPanel.startAnimation(bottomDown);
        threadEditionPanel.setVisibility(View.GONE);

        // Thread creation FAB slide up animation
        TranslateAnimation moveUpBottom = new TranslateAnimation(0, 0, 0, 400);
        moveUpBottom.setInterpolator(new DecelerateInterpolator());
        moveUpBottom.setStartOffset(200);
        moveUpBottom.setDuration(300);
        moveUpBottom.setFillEnabled(true);
        moveUpBottom.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) createNewThreadFAB.getLayoutParams();
                lp.bottomMargin -= 400;
                createNewThreadFAB.setLayoutParams(lp);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        createNewThreadFAB.startAnimation(moveUpBottom);
    }

    // -------------------------------------------------------------- //
    // ----------------------- BOTTOM TOOLBAR ----------------------- //
    // -------------------------------------------------------------- //

    private void initBottomToolbar(View rootView) {

        Toolbar toolbarBottom = rootView.findViewById(R.id.toolbar_bottom);
        // Inflate a menu to be displayed in the toolbar
        toolbarBottom.inflateMenu(R.menu.conversation_tree_toolbar_bottom_menu);
        Menu toolbarBottomMenu = toolbarBottom.getMenu();

        // Move action
        View moveActionView = toolbarBottomMenu.findItem(R.id.action_move).getActionView();
        ((ImageView) moveActionView.findViewById(R.id.menu_item_icon)).setImageResource(R.drawable.ic_subdirectory_arrow_right_black_24dp);
        ((TextView) moveActionView.findViewById(R.id.menu_item_title)).setText(R.string.move_action);
        moveActionView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "Move thread", Toast.LENGTH_SHORT).show();
            }
        });

        // Rename action
        View renameActionView = toolbarBottomMenu.findItem(R.id.action_rename).getActionView();
        ((ImageView) renameActionView.findViewById(R.id.menu_item_icon)).setImageResource(R.drawable.ic_mode_edit_black_24dp);
        ((TextView) renameActionView.findViewById(R.id.menu_item_title)).setText(R.string.rename_action);
        renameActionView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "Rename thread", Toast.LENGTH_SHORT).show();
            }
        });

        // Create thread action
        View createThreadAction = toolbarBottomMenu.findItem(R.id.action_create).getActionView();
        ((ImageView) createThreadAction.findViewById(R.id.menu_item_icon)).setImageResource(R.drawable.ic_add_black_24dp);
        ((TextView) createThreadAction.findViewById(R.id.menu_item_title)).setText(R.string.new_thread_action);
        createThreadAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "Create thread", Toast.LENGTH_SHORT).show();
            }
        });
    }
}