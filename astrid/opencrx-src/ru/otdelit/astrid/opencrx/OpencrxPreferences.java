package ru.otdelit.astrid.opencrx;

import ru.otdelit.astrid.opencrx.sync.OpencrxActivityCreator;
import ru.otdelit.astrid.opencrx.sync.OpencrxDataService;
import ru.otdelit.astrid.opencrx.sync.OpencrxSyncProvider;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.sync.SyncProviderPreferences;
import com.todoroo.astrid.sync.SyncProviderUtilities;

/**
 * Displays synchronization preferences and an action panel so users can
 * initiate actions from the menu.
 *
 * @author timsu
 *
 */
public class OpencrxPreferences extends SyncProviderPreferences {

    @Override
    public int getPreferenceResource() {
        return R.xml.preferences_opencrx;
    }

    @Override
    public void startSync() {
        new OpencrxSyncProvider().synchronize(this);
        finish();
    }

    @Override
    public void logOut() {
        new OpencrxSyncProvider().signOut();
    }

    @Override
    public SyncProviderUtilities getUtilities() {
        return OpencrxUtilities.INSTANCE;
    }

    @Override
    protected void onPause() {
        super.onPause();
        new OpencrxBackgroundService().scheduleService();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ListPreference defaultDash = (ListPreference)findPreference(getString(R.string.opencrx_PPr_defaultcreator_key));
        String[] entries, entryValues;
        StoreObject[] dashboards = OpencrxDataService.getInstance().getCreators();
        if(OpencrxUtilities.INSTANCE.isLoggedIn() && dashboards.length > 0) {
            entries = new String[dashboards.length + 1];
            entryValues = new String[dashboards.length + 1];
            for(int i = 0; i < dashboards.length; i++) {
                entries[i + 1] = dashboards[i].getValue(OpencrxActivityCreator.NAME);
                entryValues[i + 1] = Long.toString(dashboards[i].getValue(OpencrxActivityCreator.REMOTE_ID));
            }
        } else {
            entries = new String[1];
            entryValues = new String[1];
        }
        entries[0] = getString(R.string.opencrx_no_creator);
        entryValues[0] = Long.toString(OpencrxUtilities.DASHBOARD_NO_SYNC);
        defaultDash.setEntries(entries);
        defaultDash.setEntryValues(entryValues);
    }

    @Override
    public void updatePreferences(Preference preference, Object value) {
        super.updatePreferences(preference, value);
        final Resources r = getResources();

        if (r.getString(R.string.opencrx_PPr_defaultcreator_key).equals(
                preference.getKey())) {
            int index = AndroidUtilities.indexOf(((ListPreference)preference).getEntryValues(), (String)value);
            if(index == -1)
                index = 0;
            if(index == 0)
                preference.setSummary(R.string.opencrx_PPr_defaultcreator_summary_none);
            else
                preference.setSummary(r.getString(
                        R.string.opencrx_PPr_defaultcreator_summary,
                        ((ListPreference)preference).getEntries()[index]));
        }
    }
}