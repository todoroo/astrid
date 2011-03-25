package ru.otdelit.astrid.opencrx;

import java.util.ArrayList;

import ru.otdelit.astrid.opencrx.api.OpencrxUtils;
import ru.otdelit.astrid.opencrx.sync.OpencrxActivity;
import ru.otdelit.astrid.opencrx.sync.OpencrxActivityCreator;
import ru.otdelit.astrid.opencrx.sync.OpencrxContact;
import ru.otdelit.astrid.opencrx.sync.OpencrxDataService;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.activity.TaskEditActivity.TaskEditControlSet;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.MetadataService;

/**
 * Control Set for managing task/dashboard assignments in Producteev
 *
 * @author Arne Jans <arne.jans@gmail.com>
 *
 */
public class OpencrxControlSet implements TaskEditControlSet {

    // --- instance variables

    private final Activity activity;

    private final Spinner assignedToSelector;
    private final Spinner creatorSelector;

    private final AutoCompleteTextView assignedToTextInput;
    private final AutoCompleteTextView creatorTextInput;

    private ArrayList<OpencrxContact> users = null;
    private ArrayList<OpencrxActivityCreator> dashboards = null;

    @Autowired
    MetadataService metadataService;

    @SuppressWarnings("unused")
    public OpencrxControlSet(final Activity activity, ViewGroup parent) {
        DependencyInjectionService.getInstance().inject(this);

        this.activity = activity;

        View view = LayoutInflater.from(activity).inflate(R.layout.opencrx_control, parent, true);

        this.assignedToSelector = (Spinner) activity.findViewById(R.id.opencrx_TEA_task_assign);
        TextView emptyView = new TextView(activity);
        emptyView.setText(activity.getText(R.string.opencrx_no_creator));
        assignedToSelector.setEmptyView(emptyView);

        this.creatorSelector = (Spinner) activity.findViewById(R.id.opencrx_TEA_dashboard_assign);

        this.assignedToTextInput = (AutoCompleteTextView) activity.findViewById(R.id.opencrx_TEA_contact_textinput);
        this.creatorTextInput = (AutoCompleteTextView) activity.findViewById(R.id.opencrx_TEA_creator_textinput);

    }

    @Override
    public void readFromTask(Task task) {

        Metadata metadata = OpencrxDataService.getInstance().getTaskMetadata(task.getId());
        if(metadata == null)
            metadata = OpencrxActivity.newMetadata();

        // Fill the dashboard-spinner and set the current dashboard
        long dashboardId = OpencrxUtilities.INSTANCE.getDefaultDashboard();
        if(metadata.containsNonNullValue(OpencrxActivity.ACTIVITY_CREATOR_ID))
            dashboardId = metadata.getValue(OpencrxActivity.ACTIVITY_CREATOR_ID);

        StoreObject[] dashboardsData = OpencrxDataService.getInstance().getCreators();
        dashboards = new ArrayList<OpencrxActivityCreator>(dashboardsData.length);
        int dashboardSpinnerIndex = -1;

        for (int i=0;i<dashboardsData.length;i++) {
            OpencrxActivityCreator dashboard = new OpencrxActivityCreator(dashboardsData[i]);
            dashboards.add(dashboard);
            if(dashboard.getId() == dashboardId) {
                dashboardSpinnerIndex = i;
            }
        }

        //dashboard to not sync as first spinner-entry
        dashboards.add(0, new OpencrxActivityCreator(OpencrxUtilities.DASHBOARD_NO_SYNC, activity.getString(R.string.opencrx_no_creator), "")); //$NON-NLS-1$

        ArrayAdapter<OpencrxActivityCreator> dashAdapter = new ArrayAdapter<OpencrxActivityCreator>(activity,
                android.R.layout.simple_spinner_item, dashboards);
        dashAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        creatorSelector.setAdapter(dashAdapter);
        creatorSelector.setSelection(dashboardSpinnerIndex+1);

        ArrayAdapter<OpencrxActivityCreator> creatorAdapterTextInput = new ArrayAdapter<OpencrxActivityCreator>(activity,
                android.R.layout.simple_spinner_item, dashboards);
        creatorTextInput.setAdapter(creatorAdapterTextInput);
        creatorTextInput.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapter, View view, int position,
                    long id) {
                OpencrxActivityCreator creatorInput = (OpencrxActivityCreator) adapter.getItemAtPosition(position);

                if (creatorInput == null) return;

                int selectedIndex = creatorSelector.getSelectedItemPosition();

                for (int i = 0; i < creatorSelector.getAdapter().getCount(); ++i){
                    OpencrxActivityCreator current = (OpencrxActivityCreator) creatorSelector.getAdapter().getItem(i);
                    if (current != null && current.getId() == creatorInput.getId()){
                        selectedIndex = i;
                        break;
                    }
                }

                creatorSelector.setSelection(selectedIndex);
            }
        });

        // Assigned user
        long responsibleId = Preferences.getLong(OpencrxUtilities.PREF_USER_ID, -1);
        if (metadata.containsNonNullValue(OpencrxActivity.ASSIGNED_TO_ID)){
            responsibleId = metadata.getValue(OpencrxActivity.ASSIGNED_TO_ID);
        }

        StoreObject[] usersData = OpencrxDataService.getInstance().getContacts();
        this.users = new ArrayList<OpencrxContact>();
        for (StoreObject user : usersData){
            this.users.add(new OpencrxContact(user));
        }

        ArrayAdapter<OpencrxContact> usersAdapter = new ArrayAdapter<OpencrxContact>(activity,
                android.R.layout.simple_spinner_item, this.users);
        usersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        assignedToSelector.setAdapter(usersAdapter);

        int responsibleSpinnerIndex = 0;

        for (int i = 0; i < this.users.size() ; i++) {
            if (this.users.get(i).getId() == responsibleId ) {
                responsibleSpinnerIndex = i;
                break;
            }
        }
        assignedToSelector.setSelection(responsibleSpinnerIndex);

        ArrayAdapter<OpencrxContact> contactAdapterTextInput = new ArrayAdapter<OpencrxContact>(activity,
                android.R.layout.simple_spinner_item, this.users);

        assignedToTextInput.setAdapter(contactAdapterTextInput);
        assignedToTextInput.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapter, View view, int position,
                    long id) {

                OpencrxContact userInput = (OpencrxContact) adapter.getItemAtPosition(position);

                if (userInput == null) return;

                int selectedIndex = assignedToSelector.getSelectedItemPosition();

                for (int i = 0; i < assignedToSelector.getAdapter().getCount(); ++i){
                    OpencrxContact current = (OpencrxContact) assignedToSelector.getAdapter().getItem(i);
                    if (current != null && current.getId() == userInput.getId()){
                        selectedIndex = i;
                        break;
                    }
                }

                assignedToSelector.setSelection(selectedIndex);

            }
        });
    }

    @Override
    public String writeToModel(Task task) {
        Metadata metadata = OpencrxDataService.getInstance().getTaskMetadata(task.getId());
        try {
            if (metadata == null) {
                metadata = new Metadata();
                metadata.setValue(Metadata.KEY, OpencrxActivity.METADATA_KEY);
                metadata.setValue(Metadata.TASK, task.getId());
                metadata.setValue(OpencrxActivity.ID, 0L);
            }

            OpencrxActivityCreator dashboard = (OpencrxActivityCreator) creatorSelector.getSelectedItem();
            metadata.setValue(OpencrxActivity.ACTIVITY_CREATOR_ID, dashboard.getId());

            OpencrxContact responsibleUser = (OpencrxContact) assignedToSelector.getSelectedItem();

            if(responsibleUser == null)
                metadata.setValue(OpencrxActivity.ASSIGNED_TO_ID, 0L);
            else
                metadata.setValue(OpencrxActivity.ASSIGNED_TO_ID, responsibleUser.getId());

            if(metadata.getSetValues().size() > 0) {
                metadataService.save(metadata);
                task.setValue(Task.MODIFICATION_DATE, DateUtilities.now());
            }
        } catch (Exception e) {
            Log.e(OpencrxUtils.TAG, "Error Saving Metadata", e); //$NON-NLS-1$
        }
        return null;
    }
}