/**
 * See the file "LICENSE" for the full license governing this code.
 */
package ru.otdelit.astrid.opencrx.sync;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ru.otdelit.astrid.opencrx.OpencrxBackgroundService;
import ru.otdelit.astrid.opencrx.OpencrxLoginActivity;
import ru.otdelit.astrid.opencrx.OpencrxPreferences;
import ru.otdelit.astrid.opencrx.OpencrxUtilities;
import ru.otdelit.astrid.opencrx.api.ApiResponseParseException;
import ru.otdelit.astrid.opencrx.api.ApiServiceException;
import ru.otdelit.astrid.opencrx.api.ApiUtilities;
import ru.otdelit.astrid.opencrx.api.OpencrxInvoker;
import ru.otdelit.astrid.opencrx.api.OpencrxUtils;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.TimeFormatException;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.sync.SyncContainer;
import com.todoroo.astrid.sync.SyncProvider;
import com.todoroo.astrid.tags.TagService;
import com.todoroo.astrid.utility.Constants;
import com.todoroo.astrid.utility.Flags;

@SuppressWarnings("nls")
public class OpencrxSyncProvider extends SyncProvider<OpencrxTaskContainer> {

    private static final long TASK_ID_UNSYNCED = 1L;
    private OpencrxDataService dataService = null;
    private OpencrxInvoker invoker = null;
    private final OpencrxUtilities preferences = OpencrxUtilities.INSTANCE;

    /** OpenCRX principal id. set during sync */
    private long userId;

    /** map of OpenCRX resource name to id's */
    private HashMap<String, String> labelMap;
    private Time lastSync;
    private OpencrxActivityProcessGraph graph;

    static {
        AstridDependencyInjector.initialize();
    }

    @Autowired
    protected ExceptionService exceptionService;

    public OpencrxSyncProvider() {
        super();
        DependencyInjectionService.getInstance().inject(this);
    }

    // ----------------------------------------------------------------------
    // ------------------------------------------------------ utility methods
    // ----------------------------------------------------------------------

    /**
     * Sign out of service, deleting all synchronization metadata
     */
    public void signOut() {
        preferences.setToken(null);
        Preferences.setString(R.string.opencrx_PPr_login, null);
        Preferences.setString(R.string.opencrx_PPr_password, null);
        Preferences.setString(OpencrxUtilities.PREF_SERVER_LAST_SYNC, null);
        Preferences.setStringFromInteger(R.string.opencrx_PPr_defaultcreator_key, (int)OpencrxUtilities.DASHBOARD_NO_SYNC);
        preferences.clearLastSyncDate();

        dataService = OpencrxDataService.getInstance();
        dataService.clearMetadata();
    }

    /**
     * Deal with a synchronization exception. If requested, will show an error
     * to the user (unless synchronization is happening in background)
     *
     * @param context
     * @param tag
     *            error tag
     * @param e
     *            exception
     * @param showError
     *            whether to display a dialog
     */
    @Override
    protected void handleException(String tag, Exception e, boolean displayError) {
        final Context context = ContextManager.getContext();
        preferences.setLastError(e.toString());

        String message = null;

        // occurs when application was closed
        if(e instanceof IllegalStateException) {
            exceptionService.reportError(tag + "-caught", e); //$NON-NLS-1$

            // occurs when network error
        } else if(!(e instanceof ApiServiceException) && e instanceof IOException) {
            message = context.getString(R.string.opencrx_ioerror);
        } else {
            message = context.getString(R.string.DLG_error, e.toString());
            exceptionService.reportError(tag + "-unhandled", e); //$NON-NLS-1$
        }

        if(context instanceof Activity) {
            DialogUtilities.okDialog((Activity)context,
                    message == null ? "Error." : message, null);
        }
    }

    // ----------------------------------------------------------------------
    // ------------------------------------------------------ initiating sync
    // ----------------------------------------------------------------------

    /**
     * initiate sync in background
     */
    @Override
    protected void initiateBackground() {
        dataService = OpencrxDataService.getInstance();
        String authToken = preferences.getToken();

        try{

            invoker = new OpencrxInvoker();

            String host = Preferences.getStringValue(R.string.opencrx_PPr_host_key);
            String segment = Preferences.getStringValue(R.string.opencrx_PPr_segment_key);
            String provider = Preferences.getStringValue(R.string.opencrx_PPr_provider_key);

            invoker.setOpencrxPreferences(host, segment, provider);

            String email = Preferences.getStringValue(R.string.opencrx_PPr_login);
            String password = Preferences.getStringValue(R.string.opencrx_PPr_password);

            if (authToken != null){
                invoker.setCredentials(email, password);
                performSync();
            }else{
                if (email == null && password == null) {
                    // we can't do anything, user is not logged in
                } else {
                    invoker.authenticate(email, password);
                    preferences.setToken("token");
                    performSync();
                }
            }
        } catch (IllegalStateException e) {
            // occurs when application was closed
        } catch (Exception e) {
            handleException("pdv-authenticate", e, true);
        } finally {
            preferences.stopOngoing();
        }
    }

    /**
     * If user isn't already signed in, show sign in dialog. Else perform sync.
     */
    @Override
    protected void initiateManual(Activity activity) {
        String authToken = preferences.getToken();
        OpencrxUtilities.INSTANCE.stopOngoing();

        // check if we have a token & it works
        if(authToken == null) {
            // display login-activity
            Intent intent = new Intent(activity, OpencrxLoginActivity.class);
            activity.startActivityForResult(intent, 0);
        } else {
            activity.startService(new Intent(null, null,
                    activity, OpencrxBackgroundService.class));
        }
    }

    // ----------------------------------------------------------------------
    // ----------------------------------------------------- synchronization!
    // ----------------------------------------------------------------------

    protected void performSync() {

        labelMap = new HashMap<String, String>();
        lastSync = new Time();

        StatisticsService.reportEvent("producteev-started");
        preferences.recordSyncStart();

        try {
            // load user information
            JSONObject user = invoker.userUpdateOpencrx();
            saveUserData(user);
            String userCrxId = user.getString("crxid_user");

            Time cur = new Time();

            String lastServerSync = Preferences.getStringValue(OpencrxUtilities.PREF_SERVER_LAST_SYNC);

            try{
                 if (lastServerSync != null){
                     lastSync.parse(lastServerSync);
                 }else{
                     // very long time ago
                     lastSync.set(1, 1, 1980);
                 }
            }catch(TimeFormatException ex){
                lastSync.set(1, 1, 1980);
            }

            String lastNotificationId = Preferences.getStringValue(OpencrxUtilities.PREF_SERVER_LAST_NOTIFICATION);
            String lastActivityId = Preferences.getStringValue(OpencrxUtilities.PREF_SERVER_LAST_ACTIVITY);

            // read dashboards
            JSONArray creators = invoker.dashboardsShowListOpencrx();
            dataService.updateCreators(creators);

            // read contacts
            OpencrxContact[] contacts = invoker.usersShowListOpencrx();
            dataService.updateContacts( contacts );

            // read activity process graph
            graph = invoker.getActivityProcessGraph();

            // read labels and tasks
            ArrayList<OpencrxTaskContainer> remoteTasks = new ArrayList<OpencrxTaskContainer>();

            JSONArray labels = invoker.resourcesShowList();
            readLabels(labels, userCrxId);

            JSONArray tasks = invoker.tasksShowListOpencrx(graph);

            for(int i = 0; i < tasks.length(); i++) {

                JSONObject task = tasks.getJSONObject(i);

                OpencrxTaskContainer remote = parseRemoteTask(task);

                // update reminder flags for incoming remote tasks to prevent annoying
                if(remote.task.hasDueDate() && remote.task.getValue(Task.DUE_DATE) < DateUtilities.now())
                    remote.task.setFlag(Task.REMINDER_FLAGS, Task.NOTIFY_AFTER_DEADLINE, false);

                dataService.findLocalMatch(remote);

                remoteTasks.add(remote);

            }

            matchLocalTasksToRemote(remoteTasks);

            SyncData<OpencrxTaskContainer> syncData = populateSyncData(remoteTasks);
            try {
                synchronizeTasks(syncData);
            } finally {
                syncData.localCreated.close();
                syncData.localUpdated.close();
            }

            cur.setToNow();
            Preferences.setString(OpencrxUtilities.PREF_SERVER_LAST_SYNC, cur.format2445());

            preferences.recordSuccessfulSync();

            Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_EVENT_REFRESH);
            ContextManager.getContext().sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);

            // store lastIds in Preferences
            Preferences.setString(OpencrxUtilities.PREF_SERVER_LAST_NOTIFICATION, lastNotificationId);
            Preferences.setString(OpencrxUtilities.PREF_SERVER_LAST_ACTIVITY, lastActivityId);

            labelMap = null;
            lastSync = null;

            StatisticsService.reportEvent("opencrx-sync-finished"); //$NON-NLS-1$
            Flags.set(Flags.REFRESH);
        } catch (IllegalStateException e) {
        	// occurs when application was closed
        } catch (Exception e) {
            handleException("opencrx-sync", e, true); //$NON-NLS-1$
        }
    }

    private void matchLocalTasksToRemote(
            ArrayList<OpencrxTaskContainer> remoteTasks) throws IOException,
            JSONException {
        // try to mark local tasks as deleted if there are no remote tasks matching
        TodorooCursor<Task> locals = dataService.getSyncedTasks(PROPERTIES);

        try{
            int count = locals.getCount();
            for (int i = 0; i < count; ++i){
                locals.moveToNext();
                OpencrxTaskContainer local = read(locals);

                if (! local.pdvTask.containsNonNullValue(OpencrxActivity.CRX_ID))
                    continue;

                String idActivity = local.pdvTask.getValue(OpencrxActivity.CRX_ID);

                if (! existRemoteMatch(idActivity, remoteTasks)){
                    try{
                        OpencrxTaskContainer remote = parseRemoteTask(invoker.tasksViewOpencrx(idActivity, graph));

                        if (remote.task.isDeleted()){
                            // remote task is closed
                            if (local.task.isDeleted()){
                                // local task is closed too - just delete
                                dataService.deleteTaskAndMetadata(local.task.getId());
                            }else if (local.task.isCompleted()){
                                // local task is completed - check modification time
                                if (isTaskChangedAfter(remote, local)){
                                    dataService.deleteTaskAndMetadata(local.task.getId());
                                }else{
                                    invoker.taskComplete(idActivity, graph);
                                    dataService.deleteTaskAndMetadata(local.task.getId());
                                }
                            }else{
                                // local task is open - check modification time
                                if (isTaskChangedAfter(remote, local)){
                                    dataService.deleteTaskAndMetadata(local.task.getId());
                                }else{
                                    if (! invoker.taskOpen(idActivity, graph))
                                        dataService.deleteTaskAndMetadata(local.task.getId());
                                }
                            }
                        }else if (remote.task.isCompleted()){
                            // remote task is completed
                            if (local.task.isDeleted()){
                                // local task is closed
                                if (isTaskChangedAfter(remote, local)){
                                    dataService.deleteTaskAndMetadata(local.task.getId());
                                }else{
                                    invoker.taskClose(idActivity, graph);
                                    dataService.deleteTaskAndMetadata(local.task.getId());
                                }
                            }else if (local.task.isCompleted()){
                                // local task is completed too - just delete
                                dataService.deleteTaskAndMetadata(local.task.getId());
                            }else{
                                // local task is open - check modification time
                                if (isTaskChangedAfter(remote, local)){
                                    dataService.deleteTaskAndMetadata(local.task.getId());
                                }else{
                                    if (! invoker.taskOpen(idActivity, graph))
                                        dataService.deleteTaskAndMetadata(local.task.getId());
                                }
                            }
                        }else{
                            dataService.deleteTaskAndMetadata(local.task.getId());
                        }
                    }catch(ApiServiceException ex){
                        // no such task on remote server - delete local
                        dataService.deleteTaskAndMetadata(local.task.getId());
                    }
                }
            }
        }finally{
            locals.close();
        }
    }

    private boolean isTaskChangedAfter(OpencrxTaskContainer task, OpencrxTaskContainer task2) {
        Time modTime = new Time();
        modTime.set(task.task.getValue(Task.MODIFICATION_DATE));

        Time time = new Time();
        time.set(task2.task.getValue(Task.MODIFICATION_DATE));

        return modTime.after(time);
    }

    // ----------------------------------------------------------------------
    // ------------------------------------------------------------ sync data
    // ----------------------------------------------------------------------

    // IKARI: don't synchronize default dashboard
    private void saveUserData(JSONObject user) throws JSONException {

        userId = user.getLong("id_user");

        Preferences.setLong(OpencrxUtilities.PREF_USER_ID, userId);

    }

    // all synchronized properties
    private static final Property<?>[] PROPERTIES = new Property<?>[] {
            Task.ID,
            Task.TITLE,
            Task.IMPORTANCE,
            Task.DUE_DATE,
            Task.CREATION_DATE,
            Task.COMPLETION_DATE,
            Task.DELETION_DATE,
            Task.REMINDER_FLAGS,
            Task.NOTES,
            Task.RECURRENCE,
            Task.ELAPSED_SECONDS,
            Task.ESTIMATED_SECONDS,
            Task.MODIFICATION_DATE
    };

    /**
     * Populate SyncData data structure
     * @throws JSONException
     */
    private SyncData<OpencrxTaskContainer> populateSyncData(ArrayList<OpencrxTaskContainer> remoteTasks) throws JSONException {
        // fetch locally created tasks
        TodorooCursor<Task> localCreated = dataService.getLocallyCreated(PROPERTIES);

        // fetch locally updated tasks
        TodorooCursor<Task> localUpdated = dataService.getLocallyUpdated(PROPERTIES);

        return new SyncData<OpencrxTaskContainer>(remoteTasks, localCreated, localUpdated);
    }

    // ----------------------------------------------------------------------
    // ------------------------------------------------- create / push / pull
    // ----------------------------------------------------------------------

    @Override
    protected OpencrxTaskContainer create(OpencrxTaskContainer local) throws IOException {
        Task localTask = local.task;
        long dashboard = OpencrxUtilities.INSTANCE.getDefaultDashboard();
        if(local.pdvTask.containsNonNullValue(OpencrxActivity.ACTIVITY_CREATOR_ID))
            dashboard = local.pdvTask.getValue(OpencrxActivity.ACTIVITY_CREATOR_ID);

        if(dashboard == OpencrxUtilities.DASHBOARD_NO_SYNC) {
            // set a bogus task id, then return without creating
            local.pdvTask.setValue(OpencrxActivity.ID, TASK_ID_UNSYNCED);
            return local;
        }

        String idCreator = OpencrxDataService.getInstance().getCreatorCrxId(dashboard);

        long responsibleId = local.pdvTask.getValue(OpencrxActivity.ASSIGNED_TO_ID);
        String idContact = OpencrxDataService.getInstance().getContactCrxId(responsibleId);

        JSONObject response = invoker.tasksCreateOpencrx(localTask.getValue(Task.TITLE),
                                         idCreator, idContact, formatDataAsOpencrx(localTask), createStars(localTask), graph );

        OpencrxTaskContainer newRemoteTask;
        try {
            newRemoteTask = parseRemoteTask(response);
        } catch (JSONException e) {
            throw new ApiResponseParseException(e);
        }
        transferIdentifiers(newRemoteTask, local);
        push(local, newRemoteTask);
        return newRemoteTask;
    }

    /** Create a task container for the given RtmTaskSeries
     * @throws JSONException
     * @throws IOException
     * @throws ApiServiceException */
    private OpencrxTaskContainer parseRemoteTask(JSONObject remoteTask) throws JSONException, ApiServiceException, IOException {

        String resourceId = Preferences.getStringValue(OpencrxUtilities.PREF_RESOURCE_ID);

        String crxId = remoteTask.getString("repeating_value");

        JSONArray labels = invoker.resourcesShowForTask(crxId);

        int secondsSpentOnTask = invoker.getSecondsSpentOnTask(crxId, resourceId);

        Task task = new Task();
        ArrayList<Metadata> metadata = new ArrayList<Metadata>();

        if(remoteTask.has("task"))
            remoteTask = remoteTask.getJSONObject("task");

        task.setValue(Task.TITLE, ApiUtilities.decode(remoteTask.getString("title")));
        task.setValue(Task.CREATION_DATE, ApiUtilities.producteevToUnixTime(remoteTask.getString("time_created"), 0));
        task.setValue(Task.COMPLETION_DATE, remoteTask.getInt("status") == 1 ? DateUtilities.now() : 0);
        task.setValue(Task.DELETION_DATE, remoteTask.getInt("deleted") == 1 ? DateUtilities.now() : 0);
        task.setValue(Task.ELAPSED_SECONDS, secondsSpentOnTask);
        task.setValue(Task.MODIFICATION_DATE, remoteTask.getLong("modifiedAt"));

        long dueDate = ApiUtilities.producteevToUnixTime(remoteTask.getString("deadline"), 0);
        if(remoteTask.optInt("all_day", 0) == 1)
            task.setValue(Task.DUE_DATE, task.createDueDate(Task.URGENCY_SPECIFIC_DAY, dueDate));
        else
            task.setValue(Task.DUE_DATE, task.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, dueDate));
        task.setValue(Task.IMPORTANCE, 5 - remoteTask.getInt("star"));

        for(int i = 0; i < labels.length(); i++) {
            JSONObject label = labels.getJSONObject(i);

            Metadata tagData = new Metadata();
            tagData.setValue(Metadata.KEY, TagService.KEY);
            tagData.setValue(TagService.TAG, label.getString("name"));
            metadata.add(tagData);
        }

        OpencrxTaskContainer container = new OpencrxTaskContainer(task, metadata, remoteTask);

        return container;
    }

    @Override
    protected OpencrxTaskContainer pull(OpencrxTaskContainer task) throws IOException {
        if(! task.pdvTask.containsNonNullValue(OpencrxActivity.ID))
            throw new ApiServiceException("Wrong task.");

        if (task.task.isDeleted())
            return task;

        if (! task.pdvTask.containsNonNullValue(OpencrxActivity.CRX_ID))
            return null;

        String crx_id = task.pdvTask.getValue(OpencrxActivity.CRX_ID);

        if (TextUtils.isEmpty(crx_id))
            return null;

        try{
            JSONObject remote = invoker.tasksViewOpencrx(crx_id, graph);

            return parseRemoteTask(remote);
        } catch (ApiServiceException e) {
            return task;
        }catch (JSONException ex){
            throw new ApiResponseParseException(ex);
        }
    }

    /**
     * Send changes for the given Task across the wire. If a remoteTask is
     * supplied, we attempt to intelligently only transmit the values that
     * have changed.
     */
    @Override
    protected void push(OpencrxTaskContainer local, OpencrxTaskContainer remote) throws IOException {

        long idTask = local.pdvTask.getValue(OpencrxActivity.ID);

        long idDashboard = local.pdvTask.getValue(OpencrxActivity.ACTIVITY_CREATOR_ID);

        // if local is marked do not sync, handle accordingly
        if(idDashboard == OpencrxUtilities.DASHBOARD_NO_SYNC) {
            return;
        }

        String idActivity = local.pdvTask.containsNonNullValue(OpencrxActivity.CRX_ID) ? local.pdvTask.getValue(OpencrxActivity.CRX_ID)
                                                                                        : "";
        String idCreator = OpencrxDataService.getInstance().getCreatorCrxId(idDashboard);

        long idResponsible = local.pdvTask.getValue(OpencrxActivity.ASSIGNED_TO_ID);
        String idContact = OpencrxDataService.getInstance().getContactCrxId(idResponsible);

        // fetch remote task for comparison
        if(remote == null)
            remote = pull(local);

        // deleted
        if(remote != null && shouldTransmit(local, Task.DELETION_DATE, remote) && local.task.isDeleted() && !TextUtils.isEmpty(idActivity)){
            try{
                if (isTaskChangedAfter(remote, local) ){
                    local.task.setValue(Task.DELETION_DATE, 0L);
                }else{
                    invoker.taskClose(idActivity, graph);
                    remote.task.setValue(Task.DELETION_DATE, local.task.getValue(Task.DELETION_DATE));
                    return;
                }
            }catch(ApiServiceException ex){
                local.task.setValue(Task.DELETION_DATE, 0L);
            }
        }

        // completed
        if(remote != null && shouldTransmit(local, Task.COMPLETION_DATE, remote) && local.task.isCompleted() && !TextUtils.isEmpty(idActivity)){
            try{
                if (isTaskChangedAfter(remote, local) ){
                    local.task.setValue(Task.COMPLETION_DATE, 0L);
                }else{
                    invoker.taskComplete(idActivity, graph);
                    remote.task.setValue(Task.COMPLETION_DATE, local.task.getValue(Task.COMPLETION_DATE));
                    return;
                }
            }catch(ApiServiceException ex){
                local.task.setValue(Task.COMPLETION_DATE, 0L);
            }
        }

        // dashboard
        if(remote != null && idDashboard != remote.pdvTask.getValue(OpencrxActivity.ACTIVITY_CREATOR_ID)) {
            invoker.taskSetCreator(idActivity, idCreator);
            remote = pull(local);
        } else if(remote == null && idTask == TASK_ID_UNSYNCED) {
            // was un-synced, create remote
            remote = create(local);
        }

        if (remote == null)
            return;

        // responsible
        if(idResponsible != remote.pdvTask.getValue(OpencrxActivity.ASSIGNED_TO_ID) )
            invoker.taskSetAssignedTo(idActivity, idContact);

        // core properties
        if(shouldTransmit(local, Task.TITLE, remote))
            invoker.taskSetName(idActivity, local.task.getValue(Task.TITLE));

        if(shouldTransmit(local, Task.IMPORTANCE, remote))
            invoker.taskSetPriority(idActivity, createStars(local.task));

        if(shouldTransmit(local, Task.DUE_DATE, remote))
            invoker.tasksSetDueBy(idActivity, formatDataAsOpencrx(local.task));

        // scheduled start
        if (local.task.containsNonNullValue(Task.DUE_DATE) && local.task.containsNonNullValue(Task.ESTIMATED_SECONDS)){
            long dueDate = local.task.getValue(Task.DUE_DATE); // millis
            long estimated = local.task.getValue(Task.ESTIMATED_SECONDS) * 1000L; //millis

            if (dueDate != 0 && estimated != 0)
                invoker.taskSetScheduledStart(idActivity, dueDate - estimated);
        }

        // tags
        if (transmitTags(local, remote) )
            invoker.taskFollowUpToInProgress(idActivity, graph);

        // elapsed seconds
        Integer localElapsed = local.task.getValue(Task.ELAPSED_SECONDS);
        Integer remoteElapsed = remote.task.getValue(Task.ELAPSED_SECONDS);

        if (localElapsed == null)
            localElapsed = 0;

        if (remoteElapsed == null)
            remoteElapsed = 0;

        if (localElapsed > remoteElapsed){
            String resourceId = Preferences.getStringValue(OpencrxUtilities.PREF_RESOURCE_ID);

            if (! TextUtils.isEmpty(resourceId))
                invoker.createWorkRecord(idActivity, resourceId, localElapsed - remoteElapsed);
        }

        // notes
        if( ! TextUtils.isEmpty(local.task.getValue(Task.NOTES)) ) {
            String note = local.task.getValue(Task.NOTES);

            invoker.taskAddNote(idActivity, note, graph);

            local.metadata.add(ApiUtilities.createNoteMetadata(note));
            local.task.setValue(Task.NOTES, "");
        }

    }

    /**
     * Transmit tags
     *
     * @param local
     * @param remote
     * @param idTask
     * @param idDashboard
     * @throws ApiServiceException
     * @throws JSONException
     * @throws IOException
     */
    private boolean transmitTags(OpencrxTaskContainer local,
            OpencrxTaskContainer remote) throws ApiServiceException, IOException {

        boolean transmitted = false;

        String activityId = local.pdvTask.getValue(OpencrxActivity.CRX_ID);

        if (TextUtils.isEmpty(activityId))
            return false;

        HashMap<String, OpencrxResourceAssignment> assignments = new HashMap<String, OpencrxResourceAssignment>();
        for (OpencrxResourceAssignment assignment : invoker.resourceAssignmentsShowForTask(activityId))
            assignments.put(assignment.getResourceId(), assignment);

        HashSet<String> localTags = new HashSet<String>();
        HashSet<String> remoteTags = new HashSet<String>();

        for(Metadata item : local.metadata)
            if(TagService.KEY.equals(item.getValue(Metadata.KEY)))
                localTags.add(item.getValue(TagService.TAG));

        if(remote != null && remote.metadata != null)
            for(Metadata item : remote.metadata)
                if(TagService.KEY.equals(item.getValue(Metadata.KEY)))
                    remoteTags.add(item.getValue(TagService.TAG));

        if(! localTags.equals(remoteTags)) {

            for(String label : localTags) {
                if( labelMap.containsKey(label) && ! remoteTags.contains(label)) {
                    String resourceId = labelMap.get(label);

                    try{
                        invoker.taskAssignResource(activityId, resourceId);
                    }catch(ApiServiceException ex){
                        // Possible internal server error if resource is bad formed - ignore it
                    }

                    transmitted = true;
                }
            }

            for (String label : remoteTags){
                if (labelMap.containsKey(label) && ! localTags.contains(label) ){
                    String resourceId = labelMap.get(label);

                    OpencrxResourceAssignment assignment = assignments.get(resourceId);

                    if (assignment == null || assignment.getAssignmentDate() == null)
                        continue;

                    Time assignTime = new Time();
                    assignTime.set(assignment.getAssignmentDate().getTime());

                    if (lastSync.after(assignTime)){
                        try{
                            invoker.resourceAssignmentDelete(activityId, assignment.getAssignmentId());
                        }catch(IOException ex){
                            // Possible internal server error if we don't have rights to delete this - ignore it
                        }
                    }
                }
            }
        }

        return transmitted;
    }

    // ----------------------------------------------------------------------
    // --------------------------------------------------------- read / write
    // ----------------------------------------------------------------------

    @Override
    protected OpencrxTaskContainer read(TodorooCursor<Task> cursor) throws IOException {
        return dataService.readTaskAndMetadata(cursor);
    }

    @Override
    protected void write(OpencrxTaskContainer task) throws IOException {
        dataService.saveTaskAndMetadata(task);
    }

    // ----------------------------------------------------------------------
    // --------------------------------------------------------- misc helpers
    // ----------------------------------------------------------------------

    @Override
    protected int matchTask(ArrayList<OpencrxTaskContainer> tasks, OpencrxTaskContainer target) {
        int length = tasks.size();
        for(int i = 0; i < length; i++) {
            OpencrxTaskContainer task = tasks.get(i);
            if (target.pdvTask.containsNonNullValue(OpencrxActivity.ID) &&
                    task.pdvTask.getValue(OpencrxActivity.ID).equals(target.pdvTask.getValue(OpencrxActivity.ID)))
                return i;
        }
        return -1;
    }

    /**
     * get stars in producteev format
     * @param local
     * @return
     */
    private Integer createStars(Task local) {
        return 5 - local.getValue(Task.IMPORTANCE);
    }

    /**
     * get deadline in producteev format
     * @param task
     * @return
     */
    private String formatDataAsOpencrx(Task task) {
        if(!task.hasDueDate())
            return "";

        return OpencrxUtils.formatAsOpencrx(task.getValue(Task.DUE_DATE));
    }

    /**
     * Determine whether this task's property should be transmitted
     * @param task task to consider
     * @param property property to consider
     * @param remoteTask remote task proxy
     * @return
     */
    private boolean shouldTransmit(SyncContainer task, Property<?> property, SyncContainer remoteTask) {
        if(!task.task.containsValue(property))
            return false;

        if(remoteTask == null)
            return true;
        if(!remoteTask.task.containsValue(property))
            return true;

        // special cases - match if they're zero or nonzero
        if(property == Task.COMPLETION_DATE ||
                property == Task.DELETION_DATE)
            return !AndroidUtilities.equals((Long)task.task.getValue(property) == 0,
                    (Long)remoteTask.task.getValue(property) == 0);

        return !AndroidUtilities.equals(task.task.getValue(property),
                remoteTask.task.getValue(property));
    }

    @Override
    protected int updateNotification(Context context, Notification notification) {
        String notificationTitle = context.getString(R.string.opencrx_notification_title);
        Intent intent = new Intent(context, OpencrxPreferences.class);
        PendingIntent notificationIntent = PendingIntent.getActivity(context, 0,
                intent, 0);
        notification.setLatestEventInfo(context,
                notificationTitle, context.getString(R.string.SyP_progress),
                notificationIntent);
        return Constants.NOTIFICATION_SYNC;
    }

    @Override
    protected void transferIdentifiers(OpencrxTaskContainer source,
            OpencrxTaskContainer destination) {
        destination.pdvTask = source.pdvTask;
    }

    /**
     * Read labels into label map
     * @param userCrxId
     * @param dashboardId
     * @throws JSONException
     * @throws ApiServiceException
     * @throws IOException
     */
    private void readLabels(JSONArray labels, String userCrxId) throws JSONException, ApiServiceException, IOException {
        for(int i = 0; i < labels.length(); i++) {
            JSONObject label = labels.getJSONObject(i);
            putLabelIntoCache(label);

            String contactId = label.optString("contact_id");
            if (userCrxId.equals(contactId)){
                Preferences.setString(OpencrxUtilities.PREF_RESOURCE_ID, label.getString("id"));
            }
        }
    }

    /**
     * Puts a single label into the cache
     * @param dashboardId
     * @param label
     * @throws JSONException
     */
    private String putLabelIntoCache(JSONObject label)
            throws JSONException {

        String name = label.getString("name");
        String id = label.getString("id");

        labelMap.put(name, id);

        return id;
    }

    private boolean existRemoteMatch(String crxId,
            ArrayList<OpencrxTaskContainer> remoteTasks) {

        for (OpencrxTaskContainer task : remoteTasks){
            if (task.pdvTask.containsNonNullValue(OpencrxActivity.CRX_ID) &&
                    crxId.equals(task.pdvTask.getValue(OpencrxActivity.CRX_ID))){
                return true;
            }
        }

        return false;
    }

}
