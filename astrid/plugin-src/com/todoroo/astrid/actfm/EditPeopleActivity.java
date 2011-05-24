package com.todoroo.astrid.actfm;

import java.io.IOException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.actfm.sync.ActFmInvoker;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.actfm.sync.ActFmSyncService;
import com.todoroo.astrid.actfm.sync.ActFmSyncService.JsonHelper;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.service.ThemeService;
import com.todoroo.astrid.tags.TagService;
import com.todoroo.astrid.ui.PeopleContainer;
import com.todoroo.astrid.ui.PeopleContainer.OnAddNewPersonListener;
import com.todoroo.astrid.utility.Flags;

public class EditPeopleActivity extends Activity {

    public static final String EXTRA_TASK_ID = "task"; //$NON-NLS-1$

    private static final int REQUEST_LOG_IN = 0;

    private Task task;

    private final ArrayList<Metadata> nonSharedTags = new ArrayList<Metadata>();

    @Autowired ActFmPreferenceService actFmPreferenceService;

    @Autowired ActFmSyncService actFmSyncService;

    @Autowired TaskService taskService;

    @Autowired MetadataService metadataService;

    @Autowired ExceptionService exceptionService;

    @Autowired TagDataService tagDataService;

    private PeopleContainer sharedWithContainer;

    private EditText assignedTo;

    private CheckBox cbFacebook;

    private CheckBox cbTwitter;

    private TextView assignedToText;

    public EditPeopleActivity() {
        DependencyInjectionService.getInstance().inject(this);
    }

    // --- UI initialization

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.edit_people_activity);
        setTitle(getString(R.string.actfm_EPA_title));
        ThemeService.applyTheme(this);

        task = taskService.fetchById(
                getIntent().getLongExtra(EXTRA_TASK_ID, 41L), Task.ID, Task.REMOTE_ID,
                Task.TITLE, Task.USER, Task.USER_ID, Task.SHARED_WITH, Task.FLAGS);
        if(task == null) {
            finish();
            return;
        }

        ((TextView) findViewById(R.id.title)).setText(task.getValue(Task.TITLE));
        sharedWithContainer = (PeopleContainer) findViewById(R.id.share_container);
        assignedTo = (EditText) findViewById(R.id.assigned_to);
        assignedToText = (TextView) findViewById(R.id.assigned_to_text);
        cbFacebook = (CheckBox) findViewById(R.id.checkbox_facebook);
        cbTwitter = (CheckBox) findViewById(R.id.checkbox_twitter);

        sharedWithContainer.addPerson(""); //$NON-NLS-1$
        setUpListeners();
        setUpData();
    }

    @SuppressWarnings("nls")
    private void setUpData() {
        try {
            if(task.getValue(Task.USER_ID) != 0) {
                JSONObject user = new JSONObject(task.getValue(Task.USER));
                assignedToText.setText(user.getString("name"));
                assignedToText.setVisibility(View.VISIBLE);
                assignedTo.setTag(user);
                assignedTo.setEnabled(false);
                assignedTo.setVisibility(View.GONE);
            }

            JSONObject sharedWith;
            if(task.getValue(Task.SHARED_WITH).length() > 0)
                sharedWith = new JSONObject(task.getValue(Task.SHARED_WITH));
            else
                sharedWith = new JSONObject();

            cbFacebook.setChecked(sharedWith.optBoolean("fb", false));
            cbTwitter.setChecked(sharedWith.optBoolean("tw", false));

            TodorooCursor<Metadata> tags = TagService.getInstance().getTags(task.getId());
            try {
                Metadata metadata = new Metadata();
                for(tags.moveToFirst(); !tags.isAfterLast(); tags.moveToNext()) {
                    metadata.readFromCursor(tags);
                    String tag = metadata.getValue(TagService.TAG);
                    TagData tagData = tagDataService.getTag(tag, TagData.MEMBER_COUNT);
                    if(tagData.getValue(TagData.MEMBER_COUNT) > 0) {
                        TextView textView = sharedWithContainer.addPerson("#" + tag);
                        textView.setEnabled(false);
                    } else {
                        nonSharedTags.add((Metadata) metadata.clone());
                    }
                }
            } finally {
                tags.close();
            }

            JSONArray people = sharedWith.optJSONArray("p");
            if(people != null) {
                for(int i = 0; i < people.length(); i++) {
                    String person = people.getString(i);
                    TextView textView = sharedWithContainer.addPerson(person);
                    textView.setEnabled(false);
                }
            }

        } catch (JSONException e) {
            exceptionService.reportError("json-reading-data", e);
        }
    }

    private void setUpListeners() {
        findViewById(R.id.discard).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        findViewById(R.id.discard).requestFocus();

        findViewById(R.id.save).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
            }
        });

        findViewById(R.id.assigned_clear).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                assignedTo.setVisibility(View.VISIBLE);
                assignedToText.setVisibility(View.GONE);
                assignedTo.setText(""); //$NON-NLS-1$
                assignedTo.setEnabled(true);
            }
        });

        sharedWithContainer.setOnAddNewPerson(new OnAddNewPersonListener() {
            @Override
            public void textChanged(String text) {
                findViewById(R.id.share_additional).setVisibility(View.VISIBLE);
                if(text.indexOf('@') > -1) {
                    findViewById(R.id.tag_label).setVisibility(View.VISIBLE);
                    findViewById(R.id.tag_name).setVisibility(View.VISIBLE);
                }
            }
        });
    }

    // --- events

    /** Save sharing settings */
    @SuppressWarnings("nls")
    private void save() {
        if(!actFmPreferenceService.isLoggedIn()) {
            startActivityForResult(new Intent(this, ActFmLoginActivity.class),
                    REQUEST_LOG_IN);
            return;
        }

        setResult(RESULT_OK);
        Flags.set(Flags.REFRESH);
        try {
            JSONObject userJson = PeopleContainer.createUserJson(assignedTo);
            if(userJson == null) {
                task.setValue(Task.USER_ID, 0L);
                task.setValue(Task.USER, "{}");
                task.setFlag(Task.FLAGS, Task.FLAG_IS_READONLY, false);
            } else {
                task.setValue(Task.USER_ID, -1L);
                task.setValue(Task.USER, userJson.toString());
                task.setFlag(Task.FLAGS, Task.FLAG_IS_READONLY, true);
            }

            ArrayList<Metadata> metadata = new ArrayList<Metadata>(nonSharedTags);
            JSONObject sharedWith = parseSharedWithAndTags(metadata);
            task.setValue(Task.SHARED_WITH, sharedWith.toString());

            metadataService.synchronizeMetadata(task.getId(), metadata, MetadataCriteria.withKey(TagService.KEY));

            taskService.save(task);
            shareTask(sharedWith, metadata);
        } catch (JSONException e) {
            exceptionService.displayAndReportError(this, "save-people", e);
        } catch (ParseSharedException e) {
            e.view.setTextColor(Color.RED);
            e.view.requestFocus();
            System.err.println(e.message);
            DialogUtilities.okDialog(this, e.message, null);
        }
    }

    private class ParseSharedException extends Exception {
        private static final long serialVersionUID = -4135848250086302970L;
        public TextView view;
        public String message;

        public ParseSharedException(TextView view, String message) {
            this.view = view;
            this.message = message;
        }
    }

    @SuppressWarnings("nls")
    private JSONObject parseSharedWithAndTags(ArrayList<Metadata> metadata) throws
            JSONException, ParseSharedException {
        JSONObject sharedWith = new JSONObject();
        if(cbFacebook.isChecked())
            sharedWith.put("fb", true);
        if(cbTwitter.isChecked())
            sharedWith.put("tw", true);

        JSONArray peopleList = new JSONArray();
        for(int i = 0; i < sharedWithContainer.getChildCount(); i++) {
            TextView textView = sharedWithContainer.getTextView(i);
            textView.setTextAppearance(this, android.R.style.TextAppearance_Medium_Inverse);
            String text = textView.getText().toString();

            if(text.length() == 0)
                continue;
            if(text.startsWith("#")) {
                text = text.substring(1);
                TagData tagData = tagDataService.getTag(text, TagData.REMOTE_ID);
                if(tagData == null)
                    throw new ParseSharedException(textView,
                            getString(R.string.actfm_EPA_invalid_tag, text));
                Metadata tag = new Metadata();
                tag.setValue(Metadata.KEY, TagService.KEY);
                tag.setValue(TagService.TAG, text);
                tag.setValue(TagService.REMOTE_ID, tagData.getValue(TagData.REMOTE_ID));
                metadata.add(tag);

            } else {
                if(text.indexOf('@') == -1)
                    throw new ParseSharedException(textView,
                            getString(R.string.actfm_EPA_invalid_email, text));
                peopleList.put(text);
            }
        }
        sharedWith.put("p", peopleList);

        return sharedWith;
    }

    @SuppressWarnings("nls")
    private void shareTask(final JSONObject sharedWith, final ArrayList<Metadata> metadata) {
        final JSONArray emails = sharedWith.optJSONArray("p");

        final ProgressDialog pd = DialogUtilities.progressDialog(this,
                getString(R.string.DLG_please_wait));
        new Thread() {
            @Override
            public void run() {
                ActFmInvoker invoker = new ActFmInvoker(actFmPreferenceService.getToken());
                try {
                    if(task.getValue(Task.REMOTE_ID) == 0) {
                        actFmSyncService.pushTask(task.getId());
                        task.setValue(Task.REMOTE_ID, taskService.fetchById(task.getId(),
                                Task.REMOTE_ID).getValue(Task.REMOTE_ID));
                    }

                    Object[] args = buildSharingArgs(emails, metadata);
                    JSONObject result = invoker.invoke("task_share", args);

                    sharedWith.remove("p");
                    task.setValue(Task.SHARED_WITH, sharedWith.toString());
                    task.setValue(Task.DETAILS_DATE, 0L);

                    readTagData(result.getJSONArray("tags"));
                    JsonHelper.readUser(result.getJSONObject("assignee"),
                            task, Task.USER_ID, Task.USER);
                    taskService.save(task);

                    int count = result.optInt("shared", 0);
                    final String toast;
                    if(count > 0)
                        toast = getString(R.string.actfm_EPA_emailed_toast,
                            getResources().getQuantityString(R.plurals.Npeople, count, count));
                    else
                        toast = getString(R.string.actfm_EPA_saved_toast);

                    Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_EVENT_REFRESH);
                    ContextManager.getContext().sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);

                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(EditPeopleActivity.this, toast, Toast.LENGTH_LONG).show();
                            finish();
                        }
                    });
                } catch (IOException e) {
                    DialogUtilities.okDialog(EditPeopleActivity.this, getString(R.string.SyP_ioerror),
                            android.R.drawable.ic_dialog_alert, e.toString(), null);
                } catch (JSONException e) {
                    DialogUtilities.okDialog(EditPeopleActivity.this, getString(R.string.SyP_ioerror),
                            android.R.drawable.ic_dialog_alert, e.toString(), null);
                } finally {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            pd.dismiss();
                        }
                    });
                }
            }

        }.start();
    }

    @SuppressWarnings("nls")
    private void readTagData(JSONArray tags) throws JSONException {
        ArrayList<Metadata> metadata = new ArrayList<Metadata>();
        for(int i = 0; i < tags.length(); i++) {
            JSONObject tagObject = tags.getJSONObject(i);
            TagData tagData = tagDataService.getTag(tagObject.getString("name"), TagData.ID);
            if(tagData == null)
                tagData = new TagData();
            ActFmSyncService.JsonHelper.tagFromJson(tagObject, tagData);
            tagDataService.save(tagData);

            Metadata tagMeta = new Metadata();
            tagMeta.setValue(Metadata.KEY, TagService.KEY);
            tagMeta.setValue(TagService.TAG, tagData.getValue(TagData.NAME));
            tagMeta.setValue(TagService.REMOTE_ID, tagData.getValue(TagData.REMOTE_ID));
            metadata.add(tagMeta);
        }

        metadataService.synchronizeMetadata(task.getId(), metadata, MetadataCriteria.withKey(TagService.KEY));
    }

    @SuppressWarnings("nls")
    protected Object[] buildSharingArgs(JSONArray emails, ArrayList<Metadata>
            tags) throws JSONException {
        ArrayList<Object> values = new ArrayList<Object>();
        long currentTaskID = task.getValue(Task.REMOTE_ID);
        values.add("id");
        values.add(currentTaskID);

        for(int i = 0; i < emails.length(); i++) {
            String email = emails.optString(i);
            if(email == null || email.indexOf('@') == -1)
                continue;
            values.add("emails[]");
            values.add(email);
        }

        for(int i = 0; i < tags.size(); i++) {
            Metadata tag = tags.get(i);
            if(tag.containsNonNullValue(TagService.REMOTE_ID) &&
                    tag.getValue(TagService.REMOTE_ID) > 0) {
                values.add("tag_ids[]");
                values.add(tag.getValue(TagService.REMOTE_ID));
            } else {
                values.add("tags[]");
                values.add(tag.getValue(TagService.TAG));
            }
        }

        values.add("assignee");
        if(task.getValue(Task.USER_ID) == 0) {
            values.add("");
        } else {
            JSONObject user = new JSONObject(task.getValue(Task.USER));
            values.add(user.getString("email"));
        }



        String message = ((TextView) findViewById(R.id.message)).getText().toString();
        if(!TextUtils.isEmpty(message) && findViewById(R.id.share_additional).getVisibility() == View.VISIBLE) {
            values.add("message");
            values.add(message);
        }

        String tag = ((TextView) findViewById(R.id.tag_name)).getText().toString();
        if(!TextUtils.isEmpty(tag)) {
            values.add("tag");
            values.add(tag);
        }

        return values.toArray(new Object[values.size()]);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_LOG_IN) {
            if(resultCode == RESULT_OK)
                save();
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}