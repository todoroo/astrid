package ru.otdelit.astrid.opencrx;

import java.util.Set;
import java.util.TreeSet;

import ru.otdelit.astrid.opencrx.sync.OpencrxActivity;
import ru.otdelit.astrid.opencrx.sync.OpencrxActivityCreator;
import ru.otdelit.astrid.opencrx.sync.OpencrxContact;
import ru.otdelit.astrid.opencrx.sync.OpencrxDataService;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;

import com.timsu.astrid.R;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.CustomFilterCriterion;
import com.todoroo.astrid.api.MultipleSelectCriterion;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.data.Task;

public class OpencrxCustomFilterCriteriaExposer extends BroadcastReceiver {
    private static final String IDENTIFIER_OPENCRX_WORKSPACE = "opencrx_workspace"; //$NON-NLS-1$
    private static final String IDENTIFIER_OPENCRX_ASSIGNEE = "opencrx_assignee"; //$NON-NLS-1$

    @SuppressWarnings("nls")
    @Override
    public void onReceive(Context context, Intent intent) {
        // if we aren't logged in, don't expose features
        if(!OpencrxUtilities.INSTANCE.isLoggedIn())
            return;

        Resources r = context.getResources();

        StoreObject[] objects = OpencrxDataService.getInstance().getCreators();
        OpencrxActivityCreator[] dashboards = new OpencrxActivityCreator[objects.length];
        for (int i = 0; i < objects.length; i++) {
            dashboards[i] = new OpencrxActivityCreator(objects[i]);
        }

        CustomFilterCriterion[] ret = new CustomFilterCriterion[2]; // ATTENTION!!!!
        int j = 0;

        {
            String[] workspaceNames = new String[objects.length];
            String[] workspaceIds = new String[objects.length];
            for (int i = 0; i < dashboards.length; i++) {
                workspaceNames[i] = dashboards[i].getName();
                workspaceIds[i] = String.valueOf(dashboards[i].getId());
            }
            ContentValues values = new ContentValues(2);
            values.put(Metadata.KEY.name, OpencrxActivity.METADATA_KEY);
            values.put(OpencrxActivity.ACTIVITY_CREATOR_ID.name, "?");
            CustomFilterCriterion criterion = new MultipleSelectCriterion(
                    IDENTIFIER_OPENCRX_WORKSPACE,
                    context.getString(R.string.CFC_opencrx_in_workspace_text),
                    // Todo: abstract these metadata queries
                    Query.select(Metadata.TASK).from(Metadata.TABLE).join(Join.inner(
                            Task.TABLE, Metadata.TASK.eq(Task.ID))).where(Criterion.and(
                            TaskDao.TaskCriteria.activeAndVisible(),
                            MetadataDao.MetadataCriteria.withKey(OpencrxActivity.METADATA_KEY),
                            OpencrxActivity.ACTIVITY_CREATOR_ID.eq("?"))).toString(),
                    values, // what is this?
                    workspaceNames,
                    workspaceIds,
                    ((BitmapDrawable)r.getDrawable(R.drawable.silk_folder)).getBitmap(),
                    context.getString(R.string.CFC_opencrx_in_workspace_name));
            ret[j++] = criterion;
        }

        {
            Set<OpencrxContact> users = new TreeSet<OpencrxContact>();

            StoreObject[] usersData = OpencrxDataService.getInstance().getContacts();
            for (StoreObject user : usersData){
                users.add(new OpencrxContact(user));
            }
            int numUsers = users.size();
            String[] userNames = new String[numUsers];
            String[] userIds = new String[numUsers];
            int i = 0;
            for (OpencrxContact user : users) {
                userNames[i] = user.toString();
                userIds[i] = String.valueOf(user.getId());
                i++;
            }
            ContentValues values = new ContentValues(2);
            values.put(Metadata.KEY.name, OpencrxActivity.METADATA_KEY);
            values.put(OpencrxActivity.ASSIGNED_TO_ID.name, "?");
            CustomFilterCriterion criterion = new MultipleSelectCriterion(
                    IDENTIFIER_OPENCRX_ASSIGNEE,
                    context.getString(R.string.CFC_opencrx_assigned_to_text),
                    // Todo: abstract these metadata queries, and unify this code with the CustomFilterExposers.
                    Query.select(Metadata.TASK).from(Metadata.TABLE).join(Join.inner(
                            Task.TABLE, Metadata.TASK.eq(Task.ID))).where(Criterion.and(
                            TaskDao.TaskCriteria.activeAndVisible(),
                            MetadataDao.MetadataCriteria.withKey(OpencrxActivity.METADATA_KEY),
                            OpencrxActivity.ASSIGNED_TO_ID.eq("?"))).toString(),
                    values, // what is this?
                    userNames,
                    userIds,
                    ((BitmapDrawable)r.getDrawable(R.drawable.silk_user_gray)).getBitmap(),
                    context.getString(R.string.CFC_opencrx_assigned_to_name));
            ret[j++] = criterion;
        }

        // transmit filter list
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_CUSTOM_FILTER_CRITERIA);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, OpencrxUtilities.IDENTIFIER);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, ret);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }
}
