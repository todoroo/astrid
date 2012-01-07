package com.todoroo.astrid.actfm;

import greendroid.widget.AsyncImageView;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.NotificationManager;
import com.todoroo.andlib.service.NotificationManager.AndroidNotificationManager;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.actfm.sync.ActFmSyncService;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.core.SortHelper;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.tags.TagFilterExposer;
import com.todoroo.astrid.tags.TagService;
import com.todoroo.astrid.tags.TagService.Tag;
import com.todoroo.astrid.welcome.HelpInfoPopover;

public class TagViewActivity extends TaskListActivity {

    private static final String LAST_FETCH_KEY = "tag-fetch-"; //$NON-NLS-1$

    public static final String BROADCAST_TAG_ACTIVITY = AstridApiConstants.PACKAGE + ".TAG_ACTIVITY"; //$NON-NLS-1$

    public static final String EXTRA_TAG_NAME = "tag"; //$NON-NLS-1$
    public static final String EXTRA_TAG_REMOTE_ID = "remoteId"; //$NON-NLS-1$

    public static final String EXTRA_TAG_DATA = "tagData"; //$NON-NLS-1$

    protected static final int MENU_REFRESH_ID = MENU_SYNC_ID;

    private static final int REQUEST_CODE_SETTINGS = 0;

    public static final String TOKEN_START_ACTIVITY = "startActivity";

    private TagData tagData;

    @Autowired TagDataService tagDataService;

    @Autowired ActFmSyncService actFmSyncService;

    @Autowired ActFmPreferenceService actFmPreferenceService;

    private View taskListView;

    private boolean dataLoaded = false;

    private long currentId;

    private Filter originalFilter;

    //private ImageAdapter galleryAdapter;

    // --- UI initialization

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getListView().setOnKeyListener(null);

        // allow for text field entry, needed for android bug #2516
        OnTouchListener onTouch = new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.requestFocusFromTouch();
                return false;
            }
        };
        ((EditText) findViewById(R.id.quickAddText)).setOnTouchListener(onTouch);

        View membersEdit = findViewById(R.id.members_edit);
        membersEdit.setOnClickListener(settingsListener);

        if (actFmPreferenceService.isLoggedIn()) {
            View activityContainer = findViewById(R.id.activityContainer);
            activityContainer.setVisibility(View.VISIBLE);

            findViewById(R.id.listLabel).setPadding(0, 0, 0, 0);

            ImageView activity = (ImageView) findViewById(R.id.activity);
            activity.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(TagViewActivity.this, TagUpdatesActivity.class);
                    intent.putExtra(EXTRA_TAG_DATA, tagData);
                    startActivity(intent);
                    AndroidUtilities.callApiMethod(5, TagViewActivity.this, "overridePendingTransition", //$NON-NLS-1$
                            new Class<?>[] { Integer.TYPE, Integer.TYPE },
                            R.anim.slide_left_in, R.anim.slide_left_out);
                }
            });
        }

        originalFilter = filter;
        showListSettingsPopover();
    }

    private final OnClickListener settingsListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(TagViewActivity.this, TagSettingsActivity.class);
            intent.putExtra(EXTRA_TAG_DATA, tagData);
            startActivityForResult(intent, REQUEST_CODE_SETTINGS);
            AndroidUtilities.callApiMethod(5, TagViewActivity.this, "overridePendingTransition", //$NON-NLS-1$
                    new Class<?>[] { Integer.TYPE, Integer.TYPE },
                    R.anim.slide_left_in, R.anim.slide_left_out);
        }
    };

    /* (non-Javadoc)
     * @see com.todoroo.astrid.activity.TaskListActivity#getListBody(android.view.ViewGroup)
     */
    @Override
    protected View getListBody(ViewGroup root) {
        ViewGroup parent = (ViewGroup) getLayoutInflater().inflate(R.layout.task_list_body_tag, root, false);

        taskListView = super.getListBody(parent);
        if(actFmPreferenceService.isLoggedIn())
            ((TextView)taskListView.findViewById(android.R.id.empty)).setText(R.string.DLG_loading);
        parent.addView(taskListView);

        return parent;
    }

    private void showListSettingsPopover() {
        if (!Preferences.getBoolean(R.string.p_showed_list_settings_help, false)) {
            View tabView = findViewById(R.id.members_edit);
            HelpInfoPopover.showPopover(this, tabView, R.string.help_popover_list_settings, null);
            Preferences.setBoolean(R.string.p_showed_list_settings_help, true);
        }
    }

    @Override
    protected void addSyncRefreshMenuItem(Menu menu) {
        if(actFmPreferenceService.isLoggedIn()) {
            MenuItem item = menu.add(Menu.NONE, MENU_REFRESH_ID, Menu.NONE,
                    R.string.actfm_TVA_menu_refresh);
            item.setIcon(R.drawable.ic_menu_refresh);
        } else {
            super.addSyncRefreshMenuItem(menu);
        }
    }

    // --- data loading

    @Override
    protected void onNewIntent(Intent intent) {
        synchronized(this) {
            if(dataLoaded)
                return;
            dataLoaded = true;
        }

        String tag = getIntent().getStringExtra(EXTRA_TAG_NAME);
        long remoteId = getIntent().getLongExtra(EXTRA_TAG_REMOTE_ID, 0);

        if(tag == null && remoteId == 0)
            return;

        TodorooCursor<TagData> cursor = tagDataService.query(Query.select(TagData.PROPERTIES).where(Criterion.or(TagData.NAME.eqCaseInsensitive(tag),
                Criterion.and(TagData.REMOTE_ID.gt(0), TagData.REMOTE_ID.eq(remoteId)))));
        try {
            tagData = new TagData();
            if(cursor.getCount() == 0) {
                tagData.setValue(TagData.NAME, tag);
                tagData.setValue(TagData.REMOTE_ID, remoteId);
                tagDataService.save(tagData);
            } else {
                cursor.moveToFirst();
                tagData.readFromCursor(cursor);
            }
        } finally {
            cursor.close();
        }

        if(tagData.getValue(TagData.REMOTE_ID) > 0) {
            String fetchKey = LAST_FETCH_KEY + tagData.getId();
            long lastFetchDate = Preferences.getLong(fetchKey, 0);
            if(DateUtilities.now() > lastFetchDate + 300000L) {
                refreshData(false, false);
                Preferences.setLong(fetchKey, DateUtilities.now());
            }
        } else {
            ((TextView)taskListView.findViewById(android.R.id.empty)).setText(R.string.TLA_no_items);
        }

        setUpMembersGallery();

        super.onNewIntent(intent);

        if (intent.getBooleanExtra(TOKEN_START_ACTIVITY, false)) {
            findViewById(R.id.activity).postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent i = new Intent(TagViewActivity.this, TagUpdatesActivity.class);
                    i.putExtra(EXTRA_TAG_DATA, tagData);
                    startActivity(i);
                    AndroidUtilities.callApiMethod(5, TagViewActivity.this, "overridePendingTransition", //$NON-NLS-1$
                            new Class<?>[] { Integer.TYPE, Integer.TYPE },
                            R.anim.slide_left_in, R.anim.slide_left_out);
                }
            }, 500);
        }
    }

    @Override
    public void loadTaskListContent(boolean requery) {
        super.loadTaskListContent(requery);
        int count = taskAdapter.getCursor().getCount();

        if(tagData != null && sortFlags <= SortHelper.FLAG_REVERSE_SORT &&
                count != tagData.getValue(TagData.TASK_COUNT)) {
            tagData.setValue(TagData.TASK_COUNT, count);
            tagDataService.save(tagData);
        }
    }

    // --------------------------------------------------------- refresh data

    /** refresh the list with latest data from the web */
    private void refreshData(final boolean manual, boolean bypassTagShow) {
        final boolean noRemoteId = tagData.getValue(TagData.REMOTE_ID) == 0;

        final ProgressDialog progressDialog;
        if(manual && !noRemoteId)
            progressDialog = DialogUtilities.progressDialog(this, getString(R.string.DLG_please_wait));
        else
            progressDialog = null;

        Thread tagShowThread = new Thread(new Runnable() {
            @SuppressWarnings("nls")
            @Override
            public void run() {
                try {
                    String oldName = tagData.getValue(TagData.NAME);
                    actFmSyncService.fetchTag(tagData);

                    DialogUtilities.dismissDialog(TagViewActivity.this, progressDialog);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(noRemoteId && tagData.getValue(TagData.REMOTE_ID) > 0)
                                refreshData(manual, true);
                        }
                    });

                    if(!oldName.equals(tagData.getValue(TagData.NAME))) {
                        TagService.getInstance().rename(oldName,
                                tagData.getValue(TagData.NAME));
                    }

                } catch (IOException e) {
                    Log.e("tag-view-activity", "error-fetching-task-io", e);
                } catch (JSONException e) {
                    Log.e("tag-view-activity", "error-fetching-task", e);
                }
            }
        });
        if(!bypassTagShow)
            tagShowThread.start();

        if(noRemoteId) {
            ((TextView)taskListView.findViewById(android.R.id.empty)).setText(R.string.TLA_no_items);
            return;
        }

        setUpMembersGallery();
        actFmSyncService.fetchTasksForTag(tagData, manual, new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loadTaskListContent(true);
                        ((TextView)taskListView.findViewById(android.R.id.empty)).setText(R.string.TLA_no_items);
                        DialogUtilities.dismissDialog(TagViewActivity.this, progressDialog);
                    }
                });
            }
        });

        actFmSyncService.fetchUpdatesForTag(tagData, manual, new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //refreshUpdatesList();
                        DialogUtilities.dismissDialog(TagViewActivity.this, progressDialog);
                    }
                });
            }
        });

    }

    private void setUpMembersGallery() {
        LinearLayout membersView = (LinearLayout)findViewById(R.id.shared_with);
        membersView.setOnClickListener(settingsListener);
        try {
            String membersString = tagData.getValue(TagData.MEMBERS);
            JSONArray members = new JSONArray(membersString);
            if (members.length() > 0) {
                membersView.setOnClickListener(null);
                membersView.removeAllViews();
                for (int i = 0; i < members.length(); i++) {
                    JSONObject member = members.getJSONObject(i);
                    addImageForMember(membersView, member);
                }
                // Handle creator
                if(tagData.getValue(TagData.USER_ID) != 0) {
                    JSONObject owner = new JSONObject(tagData.getValue(TagData.USER));
                    addImageForMember(membersView, owner);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        findViewById(R.id.filter_assigned).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                resetAssignedFilter();
            }
        });
    }

    @SuppressWarnings("nls")
    private void addImageForMember(LinearLayout membersView, JSONObject member) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        AsyncImageView image = new AsyncImageView(this);
        image.setLayoutParams(new LinearLayout.LayoutParams((int)(50 * displayMetrics.density),
                (int)(50 * displayMetrics.density)));
        image.setDefaultImageResource(R.drawable.icn_default_person_image);
        image.setScaleType(ImageView.ScaleType.FIT_XY);
        try {
            final long id = member.getLong("id");
            if (id == ActFmPreferenceService.userId())
                member = ActFmPreferenceService.thisUser();
            final JSONObject memberToUse = member;

            final String memberName = displayName(memberToUse);
            if (memberToUse.has("picture")) {
                image.setUrl(memberToUse.getString("picture"));
            }
            image.setOnClickListener(listenerForImage(memberToUse, id, memberName));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        membersView.addView(image);
    }

    @SuppressWarnings("unused")
    private OnClickListener listenerForImage(final JSONObject member, final long id, final String displayName) {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentId == id) {
                    // Back to all
                    resetAssignedFilter();
                } else {
                    // New filter
                    currentId = id;
                    Criterion assignedCriterion;
                    if (currentId == ActFmPreferenceService.userId())
                        assignedCriterion = Criterion.or(Task.USER_ID.eq(0), Task.USER_ID.eq(id));
                    else
                        assignedCriterion = Task.USER_ID.eq(id);
                    Criterion assigned = Criterion.and(TaskCriteria.activeAndVisible(), assignedCriterion);
                    filter = TagFilterExposer.filterFromTag(TagViewActivity.this, new Tag(tagData), assigned);
                    TextView filterByAssigned = (TextView) findViewById(R.id.filter_assigned);
                    filterByAssigned.setVisibility(View.VISIBLE);
                    filterByAssigned.setText(getString(R.string.actfm_TVA_filtered_by_assign, displayName));
                    setUpTaskList();
                }
            }
        };
    }

    @Override
    protected Intent getOnClickQuickAddIntent(Task t) {
        Intent intent = super.getOnClickQuickAddIntent(t);
        // Customize extras
        return intent;
    }

    @Override
    protected Intent getOnLongClickQuickAddIntent(Task t) {
        Intent intent = super.getOnClickQuickAddIntent(t);
        // Customize extras
        return intent;
    }

    private void resetAssignedFilter() {
        currentId = -1;
        filter = originalFilter;
        findViewById(R.id.filter_assigned).setVisibility(View.GONE);
        setUpTaskList();
    }

    @SuppressWarnings("nls")
    private String displayName(JSONObject user) {
        String name = user.optString("name");
        if (!TextUtils.isEmpty(name) && !"null".equals(name)) {
            name = name.trim();
            int index = name.indexOf(' ');
            if (index > 0) {
                return name.substring(0, index);
            } else {
                return name;
            }
        } else {
            String email = user.optString("email");
            email = email.trim();
            int index = email.indexOf('@');
            if (index > 0) {
                return email.substring(0, index);
            } else {
                return email;
            }
        }
    }

    // --- receivers

    private final BroadcastReceiver notifyReceiver = new BroadcastReceiver() {
        @SuppressWarnings("nls")
        @Override
        public void onReceive(Context context, Intent intent) {
            if(!intent.hasExtra("tag_id"))
                return;
            if(!Long.toString(tagData.getValue(TagData.REMOTE_ID)).equals(intent.getStringExtra("tag_id")))
                return;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //refreshUpdatesList();
                }
            });
            refreshData(false, true);

            NotificationManager nm = new AndroidNotificationManager(ContextManager.getContext());
            nm.cancel(tagData.getValue(TagData.REMOTE_ID).intValue());
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter intentFilter = new IntentFilter(BROADCAST_TAG_ACTIVITY);
        registerReceiver(notifyReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(notifyReceiver);
    }

    @Override
    protected Task quickAddTask(String title, boolean selectNewTask) {
        if(!tagData.containsNonNullValue(TagData.NAME) ||
                tagData.getValue(TagData.NAME).length() == 0) {
            DialogUtilities.okDialog(this, getString(R.string.tag_no_title_error), null);
            return null;
        }
        return super.quickAddTask(title, selectNewTask);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_SETTINGS && resultCode == RESULT_OK) {
            tagData = tagDataService.fetchById(tagData.getId(), TagData.PROPERTIES);
            filter = TagFilterExposer.filterFromTagData(this, tagData);
            taskAdapter = null;
            loadTaskListContent(true);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean onMenuItemSelected(int featureId, final MenuItem item) {
        // handle my own menus
        switch (item.getItemId()) {
        case MENU_REFRESH_ID:
            refreshData(true, false);
            return true;
        }

        return super.onMenuItemSelected(featureId, item);
    }

}
