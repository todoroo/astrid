package com.todoroo.astrid.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.PendingIntent.CanceledException;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.activity.ShortcutActivity;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.adapter.FilterAdapter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterCategory;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.api.IntentFilter;
import com.todoroo.astrid.core.SearchFilter;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.StartupService;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.service.ThemeService;

public class FilterListFragment extends ExpandableListFragment {

    // --- menu codes

    private static final int MENU_SEARCH_ID = R.string.FLA_menu_search;
    private static final int MENU_HELP_ID = R.string.FLA_menu_help;

    private static final int CONTEXT_MENU_SHORTCUT = R.string.FLA_context_shortcut;
    private static final int CONTEXT_MENU_INTENT = Menu.FIRST + 3;

    private static final int REQUEST_CUSTOM_INTENT = 1;

    // --- instance variables

    @Autowired
    protected ExceptionService exceptionService;

    FilterAdapter adapter = null;
    private boolean mDualPane;

    /* ======================================================================
     * ======================================================= initialization
     * ====================================================================== */

    public FilterListFragment() {
        DependencyInjectionService.getInstance().inject(this);
    }

    /**  Called when loading up the activity */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Tell the framework to try to keep this fragment around
        // during a configuration change.
        setRetainInstance(true);

        new StartupService().onStartupApplication(getActivity());
    }

    /* (non-Javadoc)
     * @see com.todoroo.astrid.fragment.ExpandableListFragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        ViewGroup parent = (ViewGroup) getActivity().getLayoutInflater().inflate(R.layout.filter_list_activity, container, false);
        ThemeService.applyTheme(parent);
        return parent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().setDefaultKeyMode(Activity.DEFAULT_KEYS_SEARCH_LOCAL);
        getView().findViewById(R.id.back).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
                AndroidUtilities.callApiMethod(5, getActivity(), "overridePendingTransition", //$NON-NLS-1$
                        new Class<?>[] { Integer.TYPE, Integer.TYPE },
                        R.anim.slide_left_in, R.anim.slide_left_out);
            }
        });
        // We have a menu item to show in action bar.
        setHasOptionsMenu(true);

        onContentChanged();

        onNewIntent(getActivity().getIntent());

        Fragment tasklistFrame = getFragmentManager().findFragmentById(R.id.tasklist_fragment);
        mDualPane = (tasklistFrame != null) && tasklistFrame.isInLayout();

        if (mDualPane) {
            // In dual-pane mode, the list view highlights the selected item.
            getExpandableListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        }

        // dithering
        getActivity().getWindow().setFormat(PixelFormat.RGBA_8888);
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);

    }

    /**
     * Called when receiving a new intent. Intents this class handles:
     * <ul>
     * <li>ACTION_SEARCH - displays a search bar
     * <li>ACTION_ADD_LIST - adds new lists to the merge adapter
     * </ul>
     */
    public void onNewIntent(Intent intent) {
        final String intentAction = intent.getAction();
        if (Intent.ACTION_SEARCH.equals(intentAction)) {
            String query = intent.getStringExtra(SearchManager.QUERY).trim();
            Filter filter = new Filter(null, getString(R.string.FLA_search_filter, query),
                    new QueryTemplate().where(Functions.upper(Task.TITLE).like("%" + //$NON-NLS-1$
                            query.toUpperCase() + "%")), //$NON-NLS-1$
                    null);
            intent = new Intent(getActivity(), TaskListActivity.class);
            intent.putExtra(TaskListFragment.TOKEN_FILTER, filter);
            startActivity(intent);
        } else {
            setUpList();
        }
    }

    /**
     * Create options menu (displayed when user presses menu key)
     *
     * @return true if menu should be displayed
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem item;

        item = menu.add(Menu.NONE, MENU_SEARCH_ID, Menu.NONE,
                R.string.FLA_menu_search);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        item.setIcon(android.R.drawable.ic_menu_search);

        item = menu.add(Menu.NONE, MENU_HELP_ID, 1,
                R.string.FLA_menu_help);
        item.setIcon(android.R.drawable.ic_menu_help);
    }

    /* ======================================================================
     * ============================================================ lifecycle
     * ====================================================================== */

    @Override
    public void onStart() {
        super.onStart();
        StatisticsService.sessionStart(getActivity());
    }

    @Override
    public void onStop() {
        super.onStop();
        StatisticsService.sessionStop(getActivity());
    }

    @Override
    public void onResume() {
        super.onResume();
        if(adapter != null)
            adapter.registerRecevier();
    }

    @Override
    public void onPause() {
        super.onPause();
        if(adapter != null)
            adapter.unregisterRecevier();
    }

    /* ======================================================================
     * ===================================================== populating lists
     * ====================================================================== */

    /** Sets up the coach list adapter */
    protected void setUpList() {
        adapter = new FilterAdapter(getActivity(), getExpandableListView(),
                R.layout.filter_adapter_row, false);
        setListAdapter(adapter);

        registerForContextMenu(getExpandableListView());
    }

    /* ======================================================================
     * ============================================================== actions
     * ====================================================================== */

    /**
     * Handles items being clicked. Return true if item is handled.
     */
    protected boolean onItemClicked(FilterListItem item) {
        if(item instanceof Filter) {

            Filter filter = (Filter)item;
            Intent intent = new Intent(getActivity(), TaskListActivity.class);
            intent.putExtra(TaskListFragment.TOKEN_FILTER, filter);
            if(filter instanceof FilterWithCustomIntent) {
                FilterWithCustomIntent customFilter = ((FilterWithCustomIntent)filter);
                intent.setComponent(customFilter.customTaskList);
                if(customFilter.customExtras != null)
                    intent.getExtras().putAll(customFilter.customExtras);
            }
            // choose whether we have to start a new activity (usually for portrait mode),
            // or just update the tasklist-fragment (usually in landscape)
            TaskListFragment tasklist = (TaskListFragment) getFragmentManager()
            .findFragmentById(R.id.tasklist_fragment);
            if (tasklist == null || !tasklist.isInLayout() ||
                    (intent.getComponent() != null && !("com.todoroo.astrid.activity.TaskListActivity".equals(intent.getComponent().getClassName())))) {
                startActivity(intent);
                if (getResources().getConfiguration().orientation
                        == Configuration.ORIENTATION_LANDSCAPE)
                    AndroidUtilities.callApiMethod(5, getActivity(), "overridePendingTransition", //$NON-NLS-1$
                            new Class<?>[] { Integer.TYPE, Integer.TYPE }, 0, 0);
                else
                    AndroidUtilities.callApiMethod(5, getActivity(), "overridePendingTransition", //$NON-NLS-1$
                            new Class<?>[] { Integer.TYPE, Integer.TYPE },
                            R.anim.slide_left_in, R.anim.slide_left_out);
            } else {
                tasklist.onNewIntent(intent);
            }
            return true;
        } else if(item instanceof SearchFilter) {
            getActivity().onSearchRequested();
        } else if(item instanceof IntentFilter) {
            try {
                ((IntentFilter)item).intent.send();
            } catch (CanceledException e) {
                // ignore
            }
        }
        return false;
    }

    @Override
    public boolean onChildClick(ExpandableListView parent, View v,
            int groupPosition, int childPosition, long id) {
        FilterListItem item = (FilterListItem) adapter.getChild(groupPosition,
                childPosition);
        return onItemClicked(item);
    }

    @Override
    public void onGroupExpand(int groupPosition) {
        FilterListItem item = (FilterListItem) adapter.getGroup(groupPosition);
        onItemClicked(item);
        if(item instanceof FilterCategory)
            adapter.saveExpansionSetting((FilterCategory) item, true);
    }

    @Override
    public void onGroupCollapse(int groupPosition) {
        FilterListItem item = (FilterListItem) adapter.getGroup(groupPosition);
        onItemClicked(item);
        if(item instanceof FilterCategory)
            adapter.saveExpansionSetting((FilterCategory) item, false);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;

        int type = ExpandableListView.getPackedPositionType(info.packedPosition);
        FilterListItem item;
        if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
            int groupPos = ExpandableListView.getPackedPositionGroup(info.packedPosition);
            int childPos = ExpandableListView.getPackedPositionChild(info.packedPosition);
            item = (FilterListItem) adapter.getChild(groupPos, childPos);
        } else if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
            int groupPos = ExpandableListView.getPackedPositionGroup(info.packedPosition);
            item = (FilterListItem) adapter.getGroup(groupPos);
        } else {
            return;
        }

        MenuItem menuItem;

        if(item instanceof Filter) {
            Filter filter = (Filter) item;
            menuItem = menu.add(0, CONTEXT_MENU_SHORTCUT, 0, R.string.FLA_context_shortcut);
            menuItem.setIntent(ShortcutActivity.createIntent(filter));
        }

        for(int i = 0; i < item.contextMenuLabels.length; i++) {
            if(item.contextMenuIntents.length <= i)
                break;
            menuItem = menu.add(0, CONTEXT_MENU_INTENT, 0, item.contextMenuLabels[i]);
            menuItem.setIntent(item.contextMenuIntents[i]);
        }

        if(menu.size() > 0)
            menu.setHeaderTitle(item.listingTitle);
    }

    /**
     * Creates a shortcut on the user's home screen
     *
     * @param shortcutIntent
     * @param label
     */
    private void createShortcut(Filter filter, Intent shortcutIntent, String label) {
        if(label.length() == 0)
            return;

        Bitmap emblem = filter.listingIcon;
        if(emblem == null)
            emblem = ((BitmapDrawable) getResources().getDrawable(
                    R.drawable.filter_tags1)).getBitmap();

        // create icon by superimposing astrid w/ icon
        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        Bitmap bitmap = ((BitmapDrawable) getResources().getDrawable(
                R.drawable.icon_blank)).getBitmap();
        bitmap = bitmap.copy(bitmap.getConfig(), true);
        Canvas canvas = new Canvas(bitmap);
        int dimension = 22;
        canvas.drawBitmap(emblem, new Rect(0, 0, emblem.getWidth(), emblem.getHeight()),
                new Rect(bitmap.getWidth() - dimension, bitmap.getHeight() - dimension,
                        bitmap.getWidth(), bitmap.getHeight()), null);

        Intent createShortcutIntent = new Intent();
        createShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        createShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, label);
        createShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, bitmap);
        createShortcutIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT"); //$NON-NLS-1$

        getActivity().sendBroadcast(createShortcutIntent);
        Toast.makeText(getActivity(),
                getString(R.string.FLA_toast_onCreateShortcut, label), Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // handle my own menus
        switch (item.getItemId()) {
            case MENU_SEARCH_ID: {
                getActivity().onSearchRequested();
                return true;
            }
            case MENU_HELP_ID: {
                Intent intent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://weloveastrid.com/help-user-guide-astrid-v3/filters/")); //$NON-NLS-1$
                startActivity(intent);
                return true;
            }
            case CONTEXT_MENU_SHORTCUT: {
                ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo)item.getMenuInfo();

                final Intent shortcutIntent = item.getIntent();
                FilterListItem filter = ((FilterAdapter.ViewHolder)info.targetView.getTag()).item;
                if(filter instanceof Filter)
                    showCreateShortcutDialog(shortcutIntent, (Filter)filter);

                return true;
            }
            case CONTEXT_MENU_INTENT: {
                Intent intent = item.getIntent();
                startActivityForResult(intent, REQUEST_CUSTOM_INTENT);
                return true;
            }
            default: {
                TaskListFragment tasklist = (TaskListFragment) getFragmentManager()
                .findFragmentById(R.id.tasklist_fragment);
                if (tasklist != null && tasklist.isInLayout())
                    return tasklist.onOptionsItemSelected(item);
            }
        }
        return false;
    }

    private void showCreateShortcutDialog(final Intent shortcutIntent,
            final Filter filter) {
        FrameLayout frameLayout = new FrameLayout(getActivity());
        frameLayout.setPadding(10, 0, 10, 0);
        final EditText editText = new EditText(getActivity());
        if(filter.listingTitle == null)
            filter.listingTitle = ""; //$NON-NLS-1$
        editText.setText(filter.listingTitle.
                replaceAll("\\(\\d+\\)$", "").trim()); //$NON-NLS-1$ //$NON-NLS-2$
        frameLayout.addView(editText, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));

        final Runnable createShortcut = new Runnable() {
            @Override
            public void run() {
                String label = editText.getText().toString();
                createShortcut(filter, shortcutIntent, label);
            }
        };
        editText.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_NULL) {
                    createShortcut.run();
                    return true;
                }
                return false;
            }
        });

        new AlertDialog.Builder(getActivity())
        .setTitle(R.string.FLA_shortcut_dialog_title)
        .setMessage(R.string.FLA_shortcut_dialog)
        .setView(frameLayout)
        .setIcon(android.R.drawable.ic_dialog_info)
        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                createShortcut.run();
            }
        })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode != Activity.RESULT_CANCELED)
            adapter.clear();
        // will get lists automatically

        super.onActivityResult(requestCode, resultCode, data);
    }
}
