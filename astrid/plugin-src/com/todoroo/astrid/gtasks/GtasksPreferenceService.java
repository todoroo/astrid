package com.todoroo.astrid.gtasks;

import com.timsu.astrid.R;
import com.todoroo.astrid.sync.SyncProviderUtilities;

/**
 * Methods for working with GTasks preferences
 *
 * @author timsu
 *
 */
public class GtasksPreferenceService extends SyncProviderUtilities {

    /** add-on identifier */
    public static final String IDENTIFIER = "gtasks"; //$NON-NLS-1$

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public int getSyncIntervalKey() {
        return R.string.gtasks_GPr_interval_key;
    }

    /** GTasks user's default list id */
    public static final String PREF_DEFAULT_LIST = IDENTIFIER + "_defaultlist"; //$NON-NLS-1$

    /** GTasks user name */
    public static final String PREF_USER_NAME = IDENTIFIER + "_user"; //$NON-NLS-1$

    /** GTasks is apps for domain boolean */
    public static final String PREF_IS_DOMAIN = IDENTIFIER + "_domain"; //$NON-NLS-1$

    /** GTasks whether we have shown list help boolean */
    public static final String PREF_SHOWN_LIST_HELP = IDENTIFIER + "_list_help"; //$NON-NLS-1$

}