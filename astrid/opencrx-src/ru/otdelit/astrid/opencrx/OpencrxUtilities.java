package ru.otdelit.astrid.opencrx;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.sync.SyncProviderUtilities;

/**
 * Displays synchronization preferences and an action panel so users can
 * initiate actions from the menu.
 *
 * @author timsu
 *
 */
public class OpencrxUtilities extends SyncProviderUtilities {

    /** add-on identifier */
    public static final String IDENTIFIER = "crx"; //$NON-NLS-1$

    public static final OpencrxUtilities INSTANCE = new OpencrxUtilities();

    /** setting for dashboard to not synchronize */
    public static final long DASHBOARD_NO_SYNC = -1;

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public int getSyncIntervalKey() {
        return R.string.opencrx_PPr_interval_key;
    }

    // --- opencrx-specific preferences

    public static final String PREF_SERVER_LAST_SYNC = IDENTIFIER + "_last_server"; //$NON-NLS-1$

    public static final String PREF_SERVER_LAST_NOTIFICATION = IDENTIFIER + "_last_notification"; //$NON-NLS-1$

    public static final String PREF_SERVER_LAST_ACTIVITY = IDENTIFIER + "_last_activity"; //$NON-NLS-1$

    /** Producteev user's id */
    public static final String PREF_USER_ID = IDENTIFIER + "_userid"; //$NON-NLS-1$

    public static final String PREF_RESOURCE_ID = IDENTIFIER + "_resourceid"; //$NON-NLS-1$

    /**
     * Gets default dashboard from setting
     * @return DASHBOARD_NO_SYNC if should not sync, otherwise remote id
     */
    public long getDefaultDashboard() {

        String defDashboard = Preferences.getStringValue(R.string.opencrx_PPr_defaultcreator_key);

        long defaultDashboard = DASHBOARD_NO_SYNC ;
        try{
            defaultDashboard = Long.parseLong(defDashboard);
        }catch(Exception ex){
            defaultDashboard = DASHBOARD_NO_SYNC;
        }
        return defaultDashboard;
    }

    public String getHost(){
        return Preferences.getStringValue(R.string.opencrx_PPr_host_key);
    }

    public String getSegment(){
        return Preferences.getStringValue(R.string.opencrx_PPr_segment_key);
    }

    private OpencrxUtilities() {
        // prevent instantiation
    }

}