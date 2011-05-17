package com.todoroo.astrid.actfm;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.actfm.sync.ActFmSyncService;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.adapter.UpdateAdapter;
import com.todoroo.astrid.dao.UpdateDao;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Update;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.tags.TagService;
import com.todoroo.astrid.ui.PeopleContainer;

public class TagViewActivity extends TaskListActivity implements OnTabChangeListener {

    public static final String EXTRA_TAG_NAME = "tag"; //$NON-NLS-1$
    public static final String EXTRA_TAG_REMOTE_ID = "remoteId"; //$NON-NLS-1$

    protected static final int MENU_REFRESH_ID = MENU_SYNC_ID;

    protected static final int REQUEST_CODE_PICTURE = 1;

    private TagData tagData;

    @Autowired TagDataService tagDataService;

    @Autowired ActFmSyncService actFmSyncService;

    @Autowired UpdateDao updateDao;

    private TabHost tabHost;
    private UpdateAdapter updateAdapter;
    private PeopleContainer tagMembers;
    private EditText addCommentField;
    private ImageView picture;
    private EditText tagName;

    // --- UI initialization

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        ((EditText) findViewById(R.id.commentField)).setOnTouchListener(onTouch);
    }

    @SuppressWarnings("nls")
    @Override
    protected View getListBody(ViewGroup root) {
        ViewGroup parent = (ViewGroup) getLayoutInflater().inflate(R.layout.task_list_body_tag, root, false);
        ViewGroup tabContent = (ViewGroup) parent.findViewById(android.R.id.tabcontent);

        String[] tabLabels = getResources().getStringArray(R.array.TVA_tabs);
        tabHost = (TabHost) parent.findViewById(android.R.id.tabhost);
        TabWidget tabWidget = (TabWidget) parent.findViewById(android.R.id.tabs);
        tabHost.setup();

        // set up tabs
        View taskList = super.getListBody(parent);
        tabContent.addView(taskList);
        addTab(tabWidget, taskList.getId(), "tasks", tabLabels[0]);
        addTab(tabWidget, R.id.tab_updates, "updates", tabLabels[1]);
        addTab(tabWidget, R.id.tab_settings, "members", tabLabels[2]);

        tabHost.setOnTabChangedListener(this);

        return parent;
    }

    private void addTab(TabWidget tabWidget, int contentId, String id, String label) {
        TabHost.TabSpec spec = tabHost.newTabSpec(id);
        spec.setContent(contentId);
        TextView textIndicator = (TextView) getLayoutInflater().inflate(R.layout.gd_tab_indicator, tabWidget, false);
        textIndicator.setText(label);
        spec.setIndicator(textIndicator);
        tabHost.addTab(spec);
    }

    @SuppressWarnings("nls")
    @Override
    public void onTabChanged(String tabId) {
        if(tabId.equals("tasks"))
            findViewById(R.id.taskListFooter).setVisibility(View.VISIBLE);
        else
            findViewById(R.id.taskListFooter).setVisibility(View.GONE);

        if(tabId.equals("updates"))
            findViewById(R.id.updatesFooter).setVisibility(View.VISIBLE);
        else
            findViewById(R.id.updatesFooter).setVisibility(View.GONE);

        if(tabId.equals("members"))
            findViewById(R.id.membersFooter).setVisibility(View.VISIBLE);
        else
            findViewById(R.id.membersFooter).setVisibility(View.GONE);
    }


    /**
     * Create options menu (displayed when user presses menu key)
     *
     * @return true if menu should be displayed
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if(menu.size() > 0)
            menu.clear();

        MenuItem item;

        item = menu.add(Menu.NONE, MENU_ADDONS_ID, Menu.NONE,
                R.string.TLA_menu_addons);
        item.setIcon(android.R.drawable.ic_menu_set_as);

        item = menu.add(Menu.NONE, MENU_SETTINGS_ID, Menu.NONE,
                R.string.TLA_menu_settings);
        item.setIcon(android.R.drawable.ic_menu_preferences);

        item = menu.add(Menu.NONE, MENU_SORT_ID, Menu.NONE,
                R.string.TLA_menu_sort);
        item.setIcon(android.R.drawable.ic_menu_sort_by_size);

        item = menu.add(Menu.NONE, MENU_REFRESH_ID, Menu.NONE,
                R.string.actfm_TVA_menu_refresh);
        item.setIcon(R.drawable.ic_menu_refresh);

        item = menu.add(Menu.NONE, MENU_HELP_ID, Menu.NONE,
                R.string.TLA_menu_help);
        item.setIcon(android.R.drawable.ic_menu_help);

        return true;
    }

    protected void setUpMemberPage() {
        tagMembers = (PeopleContainer) findViewById(R.id.members_container);
        tagName = (EditText) findViewById(R.id.tag_name);
        picture = (ImageView) findViewById(R.id.picture);

        picture.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*"); //$NON-NLS-1$
                startActivityForResult(Intent.createChooser(intent,
                        getString(R.string.actfm_TVA_tag_picture)), REQUEST_CODE_PICTURE);
            }
        });

        findViewById(R.id.saveMembers).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                saveSettings();
            }
        });

        refreshMembersPage();
    }

    protected void setUpUpdateList() {
        TodorooCursor<Update> currentCursor = tagDataService.getUpdates(tagData);
        startManagingCursor(currentCursor);

        updateAdapter = new UpdateAdapter(this, R.layout.update_adapter_row,
                currentCursor, false, null);
        ((ListView)findViewById(R.id.tab_updates)).setAdapter(updateAdapter);

        final ImageButton quickAddButton = (ImageButton) findViewById(R.id.commentButton);
        addCommentField = (EditText) findViewById(R.id.commentField);
        addCommentField.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_NULL && addCommentField.getText().length() > 0) {
                    addComment();
                    return true;
                }
                return false;
            }
        });
        addCommentField.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                quickAddButton.setVisibility((s.length() > 0) ? View.VISIBLE : View.GONE);
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //
            }
        });
        quickAddButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                addComment();
            }
        });
    }

    // --- data loading

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        String tag = getIntent().getStringExtra(EXTRA_TAG_NAME);
        long remoteId = getIntent().getLongExtra(EXTRA_TAG_REMOTE_ID, 0);

        if(tag == null && remoteId == 0)
            return;

        TodorooCursor<TagData> cursor = tagDataService.query(Query.select(TagData.PROPERTIES).where(Criterion.or(TagData.NAME.eq(tag),
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

        refreshData(false, false);
        setUpUpdateList();
        setUpMemberPage();
    }

    private void refreshUpdatesList() {
        Cursor cursor = updateAdapter.getCursor();
        cursor.requery();
        startManagingCursor(cursor);
    }

    @SuppressWarnings("nls")
    private void refreshMembersPage() {
        tagName.setText(tagData.getValue(TagData.NAME));

        tagMembers.removeAllViews();
        String peopleJson = tagData.getValue(TagData.MEMBERS);
        if(!TextUtils.isEmpty(peopleJson)) {
            try {
                JSONArray people = new JSONArray(peopleJson);
                for(int i = 0; i < people.length(); i++) {
                    JSONObject person = people.getJSONObject(i);
                    TextView textView = null;
                    if(!TextUtils.isEmpty(person.optString("name")))
                        textView = tagMembers.addPerson(person.getString("name"));
                    else if(!TextUtils.isEmpty(person.optString("email")))
                        textView = tagMembers.addPerson(person.getString("email"));
                    if(textView != null) {
                        textView.setTag(person);
                        textView.setEnabled(false);
                    }
                }
            } catch (JSONException e) {
                Log.e("tag-view-activity", "json error refresh members", e);
            }
        }

        tagMembers.addPerson(""); //$NON-NLS-1$
    }

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
                    if(noRemoteId && tagData.getValue(TagData.REMOTE_ID) > 0) {
                        refreshData(manual, true);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                refreshUpdatesList();
                                refreshMembersPage();
                            }
                        });
                    }

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

        if(noRemoteId)
            return;

        actFmSyncService.fetchTasksForTag(tagData, manual, new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loadTaskListContent(true);
                        if(progressDialog != null)
                            progressDialog.dismiss();
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
                        refreshUpdatesList();
                        if(progressDialog != null)
                            progressDialog.dismiss();
                    }
                });
            }
        });
    }

    // --- events

    private void saveSettings() {
        String oldName = tagData.getValue(TagData.NAME);
        String newName = tagName.getText().toString();

        if(!oldName.equals(newName)) {
            tagData.setValue(TagData.NAME, newName);
            TagService.getInstance().rename(oldName, newName);
        }

        JSONArray members = tagMembers.toJSONArray();
        tagData.setValue(TagData.MEMBERS, members.toString());
        tagData.setValue(TagData.MEMBER_COUNT, members.length());
        tagDataService.save(tagData);

        refreshMembersPage();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_CODE_PICTURE && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            String[] projection = { MediaStore.Images.Media.DATA };
            Cursor cursor = managedQuery(uri, projection, null, null, null);
            String path;

            if(cursor != null) {
                try {
                    int column_index = cursor
                            .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    cursor.moveToFirst();
                    path = cursor.getString(column_index);
                } finally {
                    cursor.close();
                }
            } else {
                path = uri.getPath();
            }

            Bitmap bitmap = BitmapFactory.decodeFile(path);
            if(bitmap != null)
                picture.setImageBitmap(bitmap);
        }
    }

    private void addComment() {
        Update update = new Update();
        update.setValue(Update.MESSAGE, addCommentField.getText().toString());
        update.setValue(Update.ACTION_CODE, "tag_comment"); //$NON-NLS-1$
        update.setValue(Update.USER_ID, 0L);
        update.setValue(Update.TAG, tagData.getId());
        update.setValue(Update.CREATION_DATE, DateUtilities.now());
        updateDao.createNew(update);

        addCommentField.setText(""); //$NON-NLS-1$
        refreshUpdatesList();
    }

    @Override
    public boolean onMenuItemSelected(int featureId, final MenuItem item) {
        // handle my own menus
        switch (item.getItemId()) {
        case MENU_REFRESH_ID:
            refreshData(true, true);
            return true;
        }

        return super.onMenuItemSelected(featureId, item);
    }

}
