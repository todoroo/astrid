package ru.otdelit.astrid.opencrx.sync;

import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONObject;

import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.sync.SyncContainer;

/**
 * RTM Task Container
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class OpencrxTaskContainer extends SyncContainer {

    public Metadata pdvTask;

    public OpencrxTaskContainer(Task task, ArrayList<Metadata> metadata, Metadata pdvTask) {
        this.task = task;
        this.metadata = metadata;
        this.pdvTask = pdvTask;
        if(this.pdvTask == null) {
            this.pdvTask = OpencrxActivity.newMetadata();
        }
    }

    @SuppressWarnings("nls")
    public OpencrxTaskContainer(Task task, ArrayList<Metadata> metadata, JSONObject remoteTask) {
        this(task, metadata, new Metadata());
        pdvTask.setValue(Metadata.KEY, OpencrxActivity.METADATA_KEY);
        pdvTask.setValue(OpencrxActivity.ID, remoteTask.optLong("id_task"));
        pdvTask.setValue(OpencrxActivity.ACTIVITY_CREATOR_ID, remoteTask.optLong("id_dashboard"));
        pdvTask.setValue(OpencrxActivity.ASSIGNED_TO_ID, remoteTask.optLong("id_responsible"));
        pdvTask.setValue(OpencrxActivity.USERCREATOR_ID, remoteTask.optLong("id_creator"));
        pdvTask.setValue(OpencrxActivity.CRX_ID, remoteTask.optString("repeating_value"));
    }

    public OpencrxTaskContainer(Task task, ArrayList<Metadata> metadata) {
        this.task = task;
        this.metadata = metadata;

        for(Iterator<Metadata> iterator = metadata.iterator(); iterator.hasNext(); ) {
            Metadata item = iterator.next();
            if(OpencrxActivity.METADATA_KEY.equals(item.getValue(Metadata.KEY))) {
                pdvTask = item;
                iterator.remove();
                // don't break, could be multiple
            }
        }
        if(this.pdvTask == null) {
            this.pdvTask = OpencrxActivity.newMetadata();
        }
    }


}
