/**
 * See the file "LICENSE" for the full license governing this code.
 */
package ru.otdelit.astrid.opencrx.sync;

import java.util.ArrayList;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ru.otdelit.astrid.opencrx.OpencrxUtilities;
import ru.otdelit.astrid.opencrx.api.ApiUtilities;

import android.content.Context;
import android.text.TextUtils;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.dao.StoreObjectDao;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.StoreObjectDao.StoreObjectCriteria;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.notes.NoteMetadata;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.tags.TagService;

public final class OpencrxDataService {

    // --- constants

    /** Utility for joining tasks with metadata */
    public static final Join METADATA_JOIN = Join.left(Metadata.TABLE, Task.ID.eq(Metadata.TASK));

    /** NoteMetadata provider string */
    public static final String NOTE_PROVIDER = "opencrx_note_provider"; //$NON-NLS-1$

    // --- singleton

    private static OpencrxDataService instance = null;

    public static synchronized OpencrxDataService getInstance() {
        if(instance == null)
            instance = new OpencrxDataService(ContextManager.getContext());
        return instance;
    }

    // --- instance variables

    protected final Context context;

    @Autowired
    private TaskDao taskDao;

    @Autowired
    private MetadataService metadataService;

    @Autowired
    private StoreObjectDao storeObjectDao;

    private final OpencrxUtilities preferences = OpencrxUtilities.INSTANCE;

    static final Random random = new Random();

    private OpencrxDataService(Context context) {
        this.context = context;
        DependencyInjectionService.getInstance().inject(this);
    }

    // --- task and metadata methods

    /**
     * Clears metadata information. Used when user logs out of service
     */
    public void clearMetadata() {
        metadataService.deleteWhere(Metadata.KEY.eq(OpencrxActivity.METADATA_KEY));
        storeObjectDao.deleteWhere(StoreObject.TYPE.eq(OpencrxActivityCreator.TYPE));
        storeObjectDao.deleteWhere(StoreObject.TYPE.eq(OpencrxContact.TYPE));
        PluginServices.getTaskService().clearDetails(Task.ID.in(Query.select(Metadata.TASK).from(Metadata.TABLE).
                where(MetadataCriteria.withKey(OpencrxActivity.METADATA_KEY))));
    }

    /**
     * Gets tasks that were created since last sync
     * @param properties
     * @return
     */
    public TodorooCursor<Task> getLocallyCreated(Property<?>[] properties) {
        return
            taskDao.query(Query.select(properties).join(OpencrxDataService.METADATA_JOIN).where(
                    Criterion.and(
                            Criterion.not(Task.ID.in(Query.select(Metadata.TASK).from(Metadata.TABLE).
                                    where(Criterion.and(MetadataCriteria.withKey(OpencrxActivity.METADATA_KEY), OpencrxActivity.ID.gt(0))))),
                            TaskCriteria.isActive())).
                    groupBy(Task.ID));
    }

    public TodorooCursor<Task> getSyncedTasks(Property<?>[] properties) {
        return
            taskDao.query(Query.
                            select(properties).
                            join(OpencrxDataService.METADATA_JOIN).
                            where(
                                    Task.ID.in(Query.
                                                  select(Metadata.TASK).from(Metadata.TABLE).
                                                  where(Criterion.and(
                                                              MetadataCriteria.withKey(OpencrxActivity.METADATA_KEY),
                                                              OpencrxActivity.ID.gt(1))
                                                        )
                                               )
                                  ).
                            groupBy(Task.ID));
    }

    /**
     * Gets tasks that were modified since last sync
     * @param properties
     * @return null if never sync'd
     */
    public TodorooCursor<Task> getLocallyUpdated(Property<?>[] properties) {
        long lastSyncDate = preferences.getLastSyncDate();
        if(lastSyncDate == 0)
            return taskDao.query(Query.select(Task.ID).where(Criterion.none));
        return
            taskDao.query(Query.select(properties).join(OpencrxDataService.METADATA_JOIN).where(
                    Criterion.and(
                            MetadataCriteria.withKey(OpencrxActivity.METADATA_KEY),
                            OpencrxActivity.ID.gt(0),
                            Task.MODIFICATION_DATE.gt(lastSyncDate))).
                    groupBy(Task.ID));
    }

    /**
     * Searches for a local task with same remote id, updates this task's id
     * @param remoteTask
     * @return true if found local match
     */
    public boolean findLocalMatch(OpencrxTaskContainer remoteTask) {
        if(remoteTask.task.getId() != Task.NO_ID)
            return true;
        TodorooCursor<Task> cursor = taskDao.query(Query.select(Task.ID).
                join(OpencrxDataService.METADATA_JOIN).where(Criterion.and(MetadataCriteria.withKey(OpencrxActivity.METADATA_KEY),
                        OpencrxActivity.ID.eq(remoteTask.pdvTask.getValue(OpencrxActivity.ID)))));
        try {
            if(cursor.getCount() == 0)
                return false;
            cursor.moveToFirst();
            remoteTask.task.setId(cursor.get(Task.ID));
            return true;
        } finally {
            cursor.close();
        }
    }

    /**
     * Saves a task and its metadata
     * @param task
     */
    public void saveTaskAndMetadata(OpencrxTaskContainer task) {
        taskDao.save(task.task);

        task.metadata.add(task.pdvTask);
        metadataService.synchronizeMetadata(task.task.getId(), task.metadata,
                Criterion.or(MetadataCriteria.withKey(OpencrxActivity.METADATA_KEY),
                        Criterion.and(MetadataCriteria.withKey(NoteMetadata.METADATA_KEY),
                                NoteMetadata.EXT_PROVIDER.eq(NOTE_PROVIDER)),
                        MetadataCriteria.withKey(TagService.KEY)));
    }

    /**
     * Reads a task and its metadata
     * @param task
     * @return
     */
    public OpencrxTaskContainer readTaskAndMetadata(TodorooCursor<Task> taskCursor) {
        Task task = new Task(taskCursor);

        // read tags, notes, etc
        ArrayList<Metadata> metadata = new ArrayList<Metadata>();
        TodorooCursor<Metadata> metadataCursor = metadataService.query(Query.select(Metadata.PROPERTIES).
                where(Criterion.and(MetadataCriteria.byTask(task.getId()),
                        Criterion.or(MetadataCriteria.withKey(TagService.KEY),
                                MetadataCriteria.withKey(OpencrxActivity.METADATA_KEY),
                                MetadataCriteria.withKey(NoteMetadata.METADATA_KEY)))));
        try {
            for(metadataCursor.moveToFirst(); !metadataCursor.isAfterLast(); metadataCursor.moveToNext()) {
                metadata.add(new Metadata(metadataCursor));
            }
        } finally {
            metadataCursor.close();
        }

        return new OpencrxTaskContainer(task, metadata);
    }

    /**
     * Reads metadata out of a task
     * @return null if no metadata found
     */
    public Metadata getTaskMetadata(long taskId) {
        TodorooCursor<Metadata> cursor = metadataService.query(Query.select(
                Metadata.PROPERTIES).where(
                MetadataCriteria.byTaskAndwithKey(taskId, OpencrxActivity.METADATA_KEY)));
        try {
            if(cursor.getCount() == 0)
                return null;
            cursor.moveToFirst();
            return new Metadata(cursor);
        } finally {
            cursor.close();
        }
    }

    /**
     * Reads task notes out of a task
     */
    public TodorooCursor<Metadata> getTaskNotesCursor(long taskId) {
        TodorooCursor<Metadata> cursor = metadataService.query(Query.select(Metadata.PROPERTIES).
                where(MetadataCriteria.byTaskAndwithKey(taskId, NoteMetadata.METADATA_KEY)));
        return cursor;
    }

    private void readCreators() {
        if (creators == null) {
            creators = readStoreObjects(OpencrxActivityCreator.TYPE);
            creatorExists = new boolean[creators.length];
        }
    }

    private void readContacts() {
        if (contacts == null) {
            contacts = readStoreObjects(OpencrxContact.TYPE);
            contactExists = new boolean[contacts.length];
        }
    }

    /**
     * Reads store objects.
     */
    public StoreObject[] readStoreObjects(String type) {
        StoreObject[] ret;
        TodorooCursor<StoreObject> cursor = storeObjectDao.query(Query.select(StoreObject.PROPERTIES).
                where(StoreObjectCriteria.byType(type)));
        try {
            ret = new StoreObject[cursor.getCount()];
            for(int i = 0; i < ret.length; i++) {
                cursor.moveToNext();
                StoreObject object = new StoreObject(cursor);
                ret[i] = object;
            }
        } finally {
            cursor.close();
        }

        return ret;
    }

    // --- dashboard methods

    private StoreObject[] creators = null;
    private boolean[] creatorExists = null; // array of flags to determine whether dashboard still exists on remote server

    /**
     * @return a list of dashboards
     */
    public StoreObject[] getCreators() {
        readCreators();
        return creators;
    }

    /**
     * Reads dashboards
     * @throws JSONException
     */
    @SuppressWarnings("nls")
    public void updateCreators(JSONArray changedDashboards) throws JSONException {
        readCreators();

        for(int i = 0; i < changedDashboards.length(); i++) {
            JSONObject remote = changedDashboards.getJSONObject(i).getJSONObject("dashboard");
            updateCreator(remote, false);
        }

        // check if there are dashboards which does not exist on remote server
        for (int i = 0; i < creators.length; ++i){
            if (! creatorExists[i])
                storeObjectDao.delete(creators[i].getId());
        }

        // clear dashboard cache
        creators = null;
        creatorExists = null;
    }

    @SuppressWarnings("nls")
    public StoreObject updateCreator(JSONObject remote, boolean reinitCache) throws JSONException {
        if (reinitCache)
            readCreators();
        long id = remote.getLong("id_dashboard");
        StoreObject local = null;
        for(int i = 0; i < creators.length; ++i) {
            if(creators[i].getValue(OpencrxActivityCreator.REMOTE_ID).equals(id)) {
                local = creators[i];
                creatorExists[i] = true;
                break;
            }
        }

        if(remote.getInt("deleted") != 0) {
            if(local != null)
                storeObjectDao.delete(local.getId());
        }

        if(local == null)
            local = new StoreObject();

        local.setValue(StoreObject.TYPE, OpencrxActivityCreator.TYPE);
        local.setValue(OpencrxActivityCreator.REMOTE_ID, id);
        local.setValue(OpencrxActivityCreator.NAME, ApiUtilities.decode(remote.getString("title")));
        local.setValue(OpencrxActivityCreator.CRX_ID, remote.getString("crx_id"));

        storeObjectDao.persist(local);
        if (reinitCache)
            creators = null;
        return local;
    }

    // user methods

    private StoreObject[] contacts = null;
    private boolean[] contactExists = null;

    /**
     * @return a list of users
     */
    public StoreObject[] getContacts() {
        readContacts();
        return contacts;
    }

    /**
     * Reads users
     */
    public void updateContacts(OpencrxContact[] remoteUsers){
        readContacts();

        for(int i = 0; i < remoteUsers.length; i++) {
            OpencrxContact remote = remoteUsers[i];
            updateContact(remote, false);
        }

        // check if there are users which does not exist on remote server
        for (int i = 0; i < contacts.length; ++i){
            if (! contactExists[i])
                storeObjectDao.delete(contacts[i].getId());
        }

        // clear user cache
        contacts = null;
        contactExists = null;
    }

    public StoreObject updateContact(OpencrxContact remote, boolean reinitCache){
        if (reinitCache)
            readContacts();

        long id = remote.getId();

        StoreObject local = null;

        for(int i = 0; i < contacts.length; ++i) {
            if(contacts[i].getValue(OpencrxContact.REMOTE_ID).equals(id)) {
                local = contacts[i];
                contactExists[i] = true;
                break;
            }
        }

        if(local == null)
            local = new StoreObject();

        local.setValue(StoreObject.TYPE, OpencrxContact.TYPE);
        local.setValue(OpencrxContact.REMOTE_ID, id);
        local.setValue(OpencrxContact.FIRST_NAME, remote.getFirstname());
        local.setValue(OpencrxContact.LAST_NAME, remote.getLastname());
        local.setValue(OpencrxContact.CRX_ID, remote.getCrxId());

        storeObjectDao.persist(local);

        if (reinitCache){
            contacts = null;
            contactExists = null;
        }

        return local;
    }

    public String getCreatorCrxId(long idDashboard) {
        TodorooCursor<StoreObject> res = storeObjectDao.query(Query.
                                                   select(OpencrxActivityCreator.CRX_ID).
                                                   where(Criterion.and
                                                                   (StoreObject.TYPE.eq(OpencrxActivityCreator.TYPE),
                                                                    OpencrxActivityCreator.REMOTE_ID.eq(idDashboard))
                                                         )
                                                    );

        try{
            if (res.getCount() > 0){
                res.moveToFirst();
                String id = res.get(OpencrxActivityCreator.CRX_ID);
                return id;
            }else{
                return null;
            }
        }finally{
            res.close();
        }

    }

    public String getContactCrxId(long idUser) {
        TodorooCursor<StoreObject> res = storeObjectDao.query(Query.
                                                   select(OpencrxContact.CRX_ID).
                                                   where(Criterion.and
                                                                   (StoreObject.TYPE.eq(OpencrxContact.TYPE),
                                                                    OpencrxContact.REMOTE_ID.eq(idUser))
                                                         )
                                                    );

        try{
            if (res.getCount() > 0){
                res.moveToFirst();
                String id = res.get(OpencrxContact.CRX_ID);
                return id;
            }else{
                return null;
            }
        }finally{
            res.close();
        }

    }

    @SuppressWarnings("nls")
    public String getUserName(long idUser) {
        TodorooCursor<StoreObject> res = storeObjectDao.query(Query.
                                                   select(OpencrxContact.LAST_NAME, OpencrxContact.FIRST_NAME).
                                                   where(Criterion.and
                                                                   (StoreObject.TYPE.eq(OpencrxContact.TYPE),
                                                                    OpencrxContact.REMOTE_ID.eq(idUser))
                                                         )
                                                    );

        try{
            if (res.getCount() > 0){
                res.moveToFirst();
                String firstName = res.get(OpencrxContact.FIRST_NAME);
                String lastName = res.get(OpencrxContact.LAST_NAME);

                boolean hasFirstName = !TextUtils.isEmpty(firstName);
                boolean hasLastName = !TextUtils.isEmpty(lastName);

                return TextUtils.concat(hasFirstName ? firstName : "",
                                       hasFirstName && hasLastName ? " " : "",
                                       hasLastName ? lastName : "").toString();
            }else{
                return null;
            }
        }finally{
            res.close();
        }

    }

    public void deleteTaskAndMetadata(long taskId){
        taskDao.delete(taskId);
        metadataService.deleteWhere(Metadata.TASK.eq(taskId));
    }

}
