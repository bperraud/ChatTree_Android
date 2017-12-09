package com.chattree.chattree.home.conversation;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.chattree.chattree.R;
import com.chattree.chattree.db.*;
import com.chattree.chattree.db.Thread;
import com.chattree.chattree.home.conversation.ThreadNodeViewHolder.ThreadTreeItem;
import com.chattree.chattree.tools.Toaster;
import com.chattree.chattree.tools.Utils;
import com.chattree.chattree.websocket.WebSocketService;
import com.unnamed.b.atv.model.TreeNode;
import com.unnamed.b.atv.model.TreeNode.TreeNodeLongClickListener;
import com.unnamed.b.atv.view.AndroidTreeView;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.chattree.chattree.home.conversation.ThreadActivity.*;

public class ConversationTreeFragment extends Fragment {

    static final String BUNDLE_CONV_ID        = "com.chattree.chattree.BUNDLE_CONV_ID";
    static final String BUNDLE_ROOT_THREAD_ID = "com.chattree.chattree.BUNDLE_ROOT_THREAD_ID";

    static final int STATE_DEFAULT                 = 0;
    static final int STATE_THREAD_SELECTED         = 1;
    static final int STATE_THREAD_TITLE_ON_EDITION = 2;
    private int currentState;

    private static final String TAG = "CONVERSATION TREE";

    private static final int FABAnimationY = 500;

    private AndroidTreeView      treeView;
    private TreeNode             root;
    private FloatingActionButton createNewThreadFAB;
    private ViewGroup            threadEditionPanel;
    private View                 emptyThreads;

    private int     userId;
    private int     convId;
    private int     rootThreadId;
    private boolean isInit;
    /**
     * True if a sync process has finished and we need to refresh the view
     */
    private boolean pendingRefresh;

    private TreeNode lastSelectedNode;

    private List<Thread> threadList;

    private WebSocketService wsService;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View      rootView      = inflater.inflate(R.layout.fragment_conversation_tree, null, false);
        ViewGroup containerView = rootView.findViewById(R.id.container);
        emptyThreads = rootView.findViewById(R.id.emptyThreads);

        convId = getArguments().getInt(BUNDLE_CONV_ID);
        rootThreadId = getArguments().getInt(BUNDLE_ROOT_THREAD_ID);
        if (rootThreadId == 0)
            throw new RuntimeException("rootThreadId not found, 0 given as default, convId: " + convId);
        isInit = false;
        pendingRefresh = false;

        currentState = STATE_DEFAULT;
        lastSelectedNode = null;

        // Retrieve the user id
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getContext());
        userId = pref.getInt("user_id", 0);

        root = TreeNode.root();

        treeView = new AndroidTreeView(getActivity(), root);
        treeView.setDefaultAnimation(true);
//        treeView.setUse2dScroll(true); TODO: if we activate 2dScroll, we need to think about tags positioning
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
                intent.putExtra(EXTRA_CONV_ID, convId);

                if (currentState == STATE_THREAD_SELECTED) {
                    clearThreadSelection();
                }
                startActivity(intent);
            }
        });
        treeView.setDefaultNodeLongClickListener(new TreeNodeLongClickListener() {
            @Override
            public boolean onLongClick(final TreeNode node, Object threadTreeItem) {
                node.setSelected(true);
                if (lastSelectedNode != null) {
                    lastSelectedNode.setSelected(false);
                    ((ThreadNodeViewHolder) lastSelectedNode.getViewHolder()).toggleItemSelectedBackground(false);
                }
                lastSelectedNode = node;

                // TODO: see how to handle the blur (focus lost) event so any selected item is not selected anymore
                // Node selected: change background
                ((ThreadNodeViewHolder) node.getViewHolder()).toggleItemSelectedBackground(true);

                // If we are already on the selection state, return
                if (currentState == STATE_THREAD_SELECTED) return true;
                currentState = STATE_THREAD_SELECTED;

                // Change the FAB icon
                createNewThreadFAB.setImageDrawable(
                        getResources().getDrawable(R.drawable.ic_arrow_back_white_24dp, getContext().getTheme())
                );

                // Bottom panel slide up animation
                Animation bottomUp = AnimationUtils.loadAnimation(getContext(), R.anim.bottom_up);
                threadEditionPanel.startAnimation(bottomUp);
                threadEditionPanel.setVisibility(View.VISIBLE);

                // Thread creation FAB slide up animation
                ObjectAnimator moveBottomUp = ObjectAnimator.ofFloat(createNewThreadFAB, "translationY", 0, -FABAnimationY);
                moveBottomUp.setStartDelay(200);
                moveBottomUp.setDuration(300);
                moveBottomUp.setInterpolator(new DecelerateInterpolator());
                moveBottomUp.start();

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
                switch (currentState) {
                    case STATE_DEFAULT:
                        createNewThreadToRoot();
                        break;
                    case STATE_THREAD_SELECTED:
                        clearThreadSelection();
                        break;
                    case STATE_THREAD_TITLE_ON_EDITION:
                        ((ThreadNodeViewHolder) lastSelectedNode.getViewHolder()).validateTitleEdition();
                        break;
                }
            }
        });

        initBottomToolbar(rootView);

        threadEditionPanel = rootView.findViewById(R.id.thread_edition_panel);

        initConvTree();

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("tState", treeView.getSaveState());
    }

    private void initConvTree() {
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

    private void buildConvTree(List<Thread> threadList) {
        this.threadList = threadList;

        Collection result = CollectionUtils.select(this.threadList, new Predicate() {
            @Override
            public boolean evaluate(Object object) {
                Thread thread = (Thread) object;
                return thread.getId() == rootThreadId;
            }
        });

        Thread rootThread = (Thread) result.toArray()[0];
        buildNodes(rootThread);

        // No threadList except the root one
        if (this.threadList.size() == 1) {
            emptyThreads.setVisibility(View.VISIBLE);
        }

        isInit = true;
        if (pendingRefresh) {
            refreshConvTree();
        }
    }

    private TreeNode buildNodes(Thread parent) {
        // Build the node corresponding to parent
        TreeNode threadNode = new TreeNode(new ThreadTreeItem(R.string.ic_messenger, parent));

        // Find the children of the parent
        for (Thread thread : threadList) {
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

    public void refreshConvTree() {
        if (!isInit) {
            pendingRefresh = true;
            return;
        }

        new AsyncTask<Void, Void, List<Thread>>() {
            @Override
            protected List<Thread> doInBackground(Void... params) {
                int       maxThreadId = Collections.max(threadList).getId();
                ThreadDao threadDao   = AppDatabase.getInstance(getContext()).threadDao();
                return threadDao.findByConvIdAndOffset(convId, maxThreadId);
            }

            @Override
            protected void onPostExecute(List<Thread> threads) {
                refreshTreeNodes(threads);
            }
        }.execute();
    }

    private void refreshTreeNodes(List<Thread> threadList) {
        this.threadList.addAll(threadList);
        for (Thread thread : threadList) {
            addThreadNode(thread, false);
        }
    }

    // ----------------------------------------------------------------------- //
    // --------------------------- THREAD CREATION --------------------------- //
    // ----------------------------------------------------------------------- //

    private void createNewThreadToRoot() {
        // WebSocket call
        wsService = ((ConversationActivity) getActivity()).getWebSocketService();
        assert wsService != null;
        wsService.createThread(rootThreadId, convId);
    }

    void clearThreadSelection() {
        if (lastSelectedNode != null) {
            lastSelectedNode.setSelected(false);
            ((ThreadNodeViewHolder) lastSelectedNode.getViewHolder()).toggleItemSelectedBackground(false);
        }

        createNewThreadFAB.setImageDrawable(
                getResources().getDrawable(R.drawable.ic_chat_white_24dp, getContext().getTheme())
        );

        currentState = STATE_DEFAULT;
        slidePanelDown();
    }

    void clearThreadTitleBeingEdited() {
        createNewThreadFAB.setImageDrawable(
                getResources().getDrawable(R.drawable.ic_chat_white_24dp, getContext().getTheme())
        );

        currentState = STATE_DEFAULT;
    }

    void cancelTitleEdition() {
        ThreadNodeViewHolder viewHolder = (ThreadNodeViewHolder) lastSelectedNode.getViewHolder();
        viewHolder.cancelTitleEdition();
    }

    public void addThread(final int threadId) {
        // Check first if we already have displayed the thread
        Collection result = CollectionUtils.select(threadList, new Predicate() {
            @Override
            public boolean evaluate(Object object) {
                Thread thread = (Thread) object;
                return thread.getId() == threadId;
            }
        });
        if (result.size() > 0) return;

        // Retrieve the thread from db and add it to the tree
        new AsyncTask<Void, Void, Thread>() {
            @Override
            protected Thread doInBackground(Void... params) {
                ThreadDao threadDao = AppDatabase.getInstance(getContext()).threadDao();
                return threadDao.findById(threadId);
            }

            @Override
            protected void onPostExecute(Thread thread) {
                threadList.add(thread);
                addThreadNode(thread, true);
            }
        }.execute();
    }

    private void addThreadNode(final Thread thread, boolean notifyUser) {
        // Create the new node
        final TreeNode newNode        = new TreeNode(new ThreadTreeItem(R.string.ic_messenger, thread));
        TreeNode       parentNode;
        int            parentThreadId = thread.getFk_thread_parent();

        if (parentThreadId == rootThreadId) {
            // Add to root thread
            parentNode = root;
            treeView.addNode(root, newNode);
        } else {
            // Add to another parent thread
            parentNode = findTreeNodeByThreadId(root, parentThreadId);
            if (parentNode != null) {
                treeView.addNode(parentNode, newNode);
                // If the view has been generated, display the arrow
                if (parentNode.getViewHolder() != null) {
                    ((ThreadNodeViewHolder) parentNode.getViewHolder()).displayArrow();
                }
            } // TODO: add to pending queue so we resolve the threads to add, in case they would arrive in bad order
            else throw new RuntimeException("Missing thread parent, can't add new thread to tree");
        }

        // Go to title edition for the new thread if we just have created it
        if (thread.getFk_author() == userId) {

            lastSelectedNode = newNode;
            currentState = STATE_THREAD_TITLE_ON_EDITION;

            treeView.expandNode(parentNode);
            ThreadNodeViewHolder viewHolder = (ThreadNodeViewHolder) newNode.getViewHolder();
            viewHolder.enableTitleEdition(false);
            createNewThreadFAB.setImageDrawable(
                    getResources().getDrawable(R.drawable.ic_check_white_24dp, getContext().getTheme())
            );
        }
        // Notify with a toast the thread creation
        else if (notifyUser) {
            treeView.expandNode(parentNode);
            new AsyncTask<Void, Void, User>() {
                @Override
                protected User doInBackground(Void... voids) {
                    UserDao userDao = AppDatabase.getInstance(getContext()).userDao();
                    return userDao.findById(thread.getFk_author());
                }

                @Override
                protected void onPostExecute(User user) {
                    Toaster.showCustomToast(getActivity(), getString(R.string.thread_created).replace(
                            "$user", Utils.getLabelFromUser(user)
                    ), null);
                }
            }.execute();
        }

        emptyThreads.setVisibility(View.GONE);
    }

    private TreeNode findTreeNodeByThreadId(TreeNode currentNode, int id) {
        // If we are considering the root node, skip these lines as we only want to explore the children
        if (currentNode != root) {
            if (((ThreadTreeItem) currentNode.getValue()).thread.getId() == id)
                return currentNode;
            if (currentNode.getChildren().size() == 0)
                return null;
        }
        for (TreeNode treeNode : currentNode.getChildren()) {
            TreeNode res = findTreeNodeByThreadId(treeNode, id);
            if (res != null) return res;
        }
        return null;
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
                clearThreadSelection();

                createNewThreadFAB.setImageDrawable(
                        getResources().getDrawable(R.drawable.ic_check_white_24dp, getContext().getTheme())
                );

                ThreadNodeViewHolder viewHolder = (ThreadNodeViewHolder) lastSelectedNode.getViewHolder();
                viewHolder.enableTitleEdition(true);
                currentState = STATE_THREAD_TITLE_ON_EDITION;
            }
        });

        // Create thread action
        View createThreadAction = toolbarBottomMenu.findItem(R.id.action_create).getActionView();
        ((ImageView) createThreadAction.findViewById(R.id.menu_item_icon)).setImageResource(R.drawable.ic_add_black_24dp);
        ((TextView) createThreadAction.findViewById(R.id.menu_item_title)).setText(R.string.new_thread_action);
        createThreadAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearThreadSelection();

                // WebSocket call
                wsService = ((ConversationActivity) getActivity()).getWebSocketService();
                assert wsService != null;
                wsService.createThread(((ThreadTreeItem) lastSelectedNode.getValue()).thread.getId(), convId);

                slidePanelDown();
            }
        });
    }

    private void slidePanelDown() {
        // Bottom panel slide down animation
        Animation bottomDown = AnimationUtils.loadAnimation(getContext(), R.anim.bottom_down);
        threadEditionPanel.startAnimation(bottomDown);
        threadEditionPanel.setVisibility(View.GONE);

        // Thread creation FAB slide down animation
        ObjectAnimator moveUpBottom = ObjectAnimator.ofFloat(createNewThreadFAB, "translationY", -FABAnimationY, 0);
        moveUpBottom.setStartDelay(200);
        moveUpBottom.setDuration(300);
        moveUpBottom.setInterpolator(new DecelerateInterpolator());
        moveUpBottom.start();
    }

    // --------------------------------------------------------------- //
    // ----------------------- GETTERS/SETTERS ----------------------- //
    // --------------------------------------------------------------- //

    public TreeNode getLastSelectedNode() {
        return lastSelectedNode;
    }

    int getCurrentState() {
        return currentState;
    }
}