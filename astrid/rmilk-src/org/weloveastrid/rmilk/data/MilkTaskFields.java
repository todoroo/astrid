/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.weloveastrid.rmilk.data;

import org.weloveastrid.rmilk.sync.MilkTaskContainer;

import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.astrid.data.Metadata;

/**
 * Metadata entries for a Remember The Milk Task
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class MilkTaskFields {

    /** metadata key */
    public static final String METADATA_KEY = "rmilk"; //$NON-NLS-1$

    /** {@link MilkListFields} id */
    public static final LongProperty LIST_ID = new LongProperty(Metadata.TABLE,
            Metadata.VALUE1.name);

    /** RTM Task Series Id */
    public static final LongProperty TASK_SERIES_ID = new LongProperty(Metadata.TABLE,
            Metadata.VALUE2.name);

    /** RTM Task Id */
    public static final LongProperty TASK_ID = new LongProperty(Metadata.TABLE,
            Metadata.VALUE3.name);

    /** Whether task repeats in RTM (1 or 0) */
    public static final IntegerProperty REPEATING = new IntegerProperty(Metadata.TABLE,
            Metadata.VALUE4.name);

    /**
     * Creates a piece of metadata from a remote task
     * @param rtmTaskSeries
     * @return
     */
    public static Metadata create(MilkTaskContainer container) {
        Metadata metadata = new Metadata();
        metadata.setValue(Metadata.KEY, METADATA_KEY);
        metadata.setValue(MilkTaskFields.LIST_ID, container.listId);
        metadata.setValue(MilkTaskFields.TASK_SERIES_ID, container.taskSeriesId);
        metadata.setValue(MilkTaskFields.TASK_ID, container.taskId);
        metadata.setValue(MilkTaskFields.REPEATING, container.repeating ? 1 : 0);

        return metadata;
    }

}
