/**
 * See the file "LICENSE" for the full license governing this code.
 */
package ru.otdelit.astrid.opencrx;

import ru.otdelit.astrid.opencrx.sync.OpencrxActivity;
import ru.otdelit.astrid.opencrx.sync.OpencrxActivityCreator;
import ru.otdelit.astrid.opencrx.sync.OpencrxDataService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.StoreObject;

/**
 * Exposes Task Details for OpenCRX:
 * - notes
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class OpencrxDetailExposer extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        ContextManager.setContext(context);
        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1)
            return;

        String taskDetail;
        try {
            taskDetail = getTaskDetails(context, taskId);
        } catch (Exception e) {
            return;
        }
        if(taskDetail == null)
            return;

        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_DETAILS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, OpencrxUtilities.IDENTIFIER);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, taskDetail);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

    @SuppressWarnings({"nls", "unused"})
    public String getTaskDetails(Context context, long id) {
        Metadata metadata = OpencrxDataService.getInstance().getTaskMetadata(id);
        if(metadata == null)
            return null;

        StringBuilder builder = new StringBuilder();

        if(!OpencrxUtilities.INSTANCE.isLoggedIn())
            return null;

        long dashboardId = -1;
        if(metadata.containsNonNullValue(OpencrxActivity.ACTIVITY_CREATOR_ID))
            dashboardId = metadata.getValue(OpencrxActivity.ACTIVITY_CREATOR_ID);
        long responsibleId = -1;
        if(metadata.containsNonNullValue(OpencrxActivity.ASSIGNED_TO_ID))
            responsibleId = metadata.getValue(OpencrxActivity.ASSIGNED_TO_ID);

        // display dashboard if not "no sync" or "default"
        StoreObject ownerDashboard = null;
        for(StoreObject dashboard : OpencrxDataService.getInstance().getCreators()) {
            if(dashboard == null || !dashboard.containsNonNullValue(OpencrxActivityCreator.REMOTE_ID))
                continue;

            if(dashboard.getValue(OpencrxActivityCreator.REMOTE_ID) == dashboardId) {
                ownerDashboard = dashboard;
                break;
            }
        }
        if(dashboardId != OpencrxUtilities.DASHBOARD_NO_SYNC && ownerDashboard != null) {
            String dashboardName = ownerDashboard.getValue(OpencrxActivityCreator.NAME);
            builder.append("<img src='silk_folder'/> ").append(dashboardName).append(TaskAdapter.DETAIL_SEPARATOR);
        }

        // display responsible user if not current one
        if(responsibleId > 0 && responsibleId != Preferences.getLong(OpencrxUtilities.PREF_USER_ID, 0L)) {
            String user = getUser(responsibleId);
            if(user != null)
                builder.append("<img src='silk_user_gray'/> ").append(user).append(TaskAdapter.DETAIL_SEPARATOR);
        }

        if(builder.length() == 0)
            return null;
        String result = builder.toString();
        return result.substring(0, result.length() - TaskAdapter.DETAIL_SEPARATOR.length());
    }

    /** Try and find user in the dashboard. return null if un-findable */
    private String getUser(long userId) {
        return OpencrxDataService.getInstance().getUserName(userId);
    }

}
