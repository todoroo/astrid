package ru.otdelit.astrid.opencrx.sync;


import ru.otdelit.astrid.opencrx.OpencrxUtilities;

import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.data.Metadata;

/**
 * Metadata entries for a Producteev Task
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class OpencrxActivity {

    /** metadata key */
    public static final String METADATA_KEY = "opencrx"; //$NON-NLS-1$

    /** task id in producteev */
    public static final LongProperty ID = new LongProperty(Metadata.TABLE,
            Metadata.VALUE1.name);

    /** dashboard id */
    public static final LongProperty ACTIVITY_CREATOR_ID = new LongProperty(Metadata.TABLE,
            Metadata.VALUE2.name);

    /** creator id */
    public static final LongProperty USERCREATOR_ID = new LongProperty(Metadata.TABLE,
            Metadata.VALUE3.name);

    /** responsible id */
    public static final LongProperty ASSIGNED_TO_ID = new LongProperty(Metadata.TABLE,
            Metadata.VALUE4.name);

    /** repeating settings */
    public static final StringProperty CRX_ID = new StringProperty(Metadata.TABLE,
            Metadata.VALUE5.name);

    public static Metadata newMetadata() {
        Metadata metadata = new Metadata();
        metadata.setValue(Metadata.KEY, OpencrxActivity.METADATA_KEY);
        metadata.setValue(ID, 0L);
        metadata.setValue(ACTIVITY_CREATOR_ID, OpencrxUtilities.INSTANCE.getDefaultDashboard());
        metadata.setValue(USERCREATOR_ID, Preferences.getLong(OpencrxUtilities.PREF_USER_ID, 0L));
        metadata.setValue(ASSIGNED_TO_ID, Preferences.getLong(OpencrxUtilities.PREF_USER_ID, 0L));
        metadata.setValue(CRX_ID, ""); //$NON-NLS-1$
        return metadata;
    }

}
