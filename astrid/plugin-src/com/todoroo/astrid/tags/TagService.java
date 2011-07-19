package com.todoroo.astrid.tags;

import java.util.ArrayList;
import java.util.LinkedHashSet;

import android.text.TextUtils;

import com.todoroo.andlib.data.DatabaseDao.ModelUpdateListener;
import com.todoroo.andlib.data.Property.CountProperty;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Field;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.utility.Flags;

/**
 * Provides operations for working with tags
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@SuppressWarnings("nls")
public final class TagService {

    // --- public constants

    /** Metadata key for tag data */
    public static final String KEY = "tags-tag";

    /** Property for reading tag values */
    public static final StringProperty TAG = Metadata.VALUE1;

    /** Property for astrid.com remote id */
    public static final LongProperty REMOTE_ID = new LongProperty(Metadata.TABLE, Metadata.VALUE2.name);

    // --- singleton

    private static TagService instance = null;

    public static synchronized TagService getInstance() {
        if(instance == null)
            instance = new TagService();
        return instance;
    }

    // --- implementation details

    @Autowired MetadataDao metadataDao;

    @Autowired TaskService taskService;

    @Autowired TaskDao taskDao;

    @Autowired TagDataService tagDataService;

    public TagService() {
        DependencyInjectionService.getInstance().inject(this);
        //Every time a task is modified, count it as activity on all lists it belongs to
        taskDao.addListener(new ModelUpdateListener<Task>() {
            @Override
            public void onModelUpdated(Task model) {
                TodorooCursor<Metadata> tags = getTags(model.getId());
                for (tags.moveToFirst(); !tags.isAfterLast(); tags.moveToNext()) {
                    String tagName = tags.get(TAG);
                    if (tagName != null) {
                        TagData tagData = tagDataService.getTag(tagName, TagData.PROPERTIES);
                        if (!TextUtils.isEmpty(tagData.getValue(TagData.NAME))) {
                            tagData.setValue(TagData.LAST_ACTIVITY_DATE, DateUtilities.now());
                            tagDataService.save(tagData);
                        }
                    }
                }
            }
        });
    }

    /**
     * Property for retrieving count of aggregated rows
     */
    private static final CountProperty COUNT = new CountProperty();
    public static final Order GROUPED_TAGS_BY_ALPHA = Order.asc(TAG);
    public static final Order GROUPED_TAGS_BY_SIZE = Order.desc(COUNT);

    /**
     * Helper class for returning a tag/task count pair
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    public static final class Tag {
        public String tag;
        int count;
        long remoteId;
        long lastActivity;

        public Tag(String tag, int count, long remoteId) {
            this.tag = tag;
            this.count = count;
            this.remoteId = remoteId;
        }

        public Tag(String tag, int count, long remoteId, long lastActivity) {
            this(tag, count, remoteId);
            this.lastActivity = lastActivity;
        }

        @Override
        public String toString() {
            return tag;
        }

        /**
         * Return SQL selector query for getting tasks with a given tag
         *
         * @param tag
         * @return
         */
        public QueryTemplate queryTemplate(Criterion criterion) {
            return new QueryTemplate().join(Join.inner(Metadata.TABLE,
                    Task.ID.eq(Metadata.TASK))).where(tagEq(tag, criterion));
        }

    }

    private static Criterion tagEq(String tag, Criterion additionalCriterion) {
        return Criterion.and(
                MetadataCriteria.withKey(KEY), TAG.eq(tag),
                additionalCriterion);
    }
    public QueryTemplate untaggedTemplate() {
        return new QueryTemplate().where(Criterion.and(
                Criterion.not(Task.ID.in(Query.select(Metadata.TASK).from(Metadata.TABLE).where(MetadataCriteria.withKey(KEY)))),
                TaskCriteria.isActive(),
                TaskCriteria.ownedByMe(),
                TaskCriteria.isVisible()));
    }

    /**
     * Return all tags ordered by given clause
     *
     * @param order ordering
     * @param activeStatus criterion for specifying completed or uncompleted
     * @return empty array if no tags, otherwise array
     */
    public Tag[] getGroupedTags(Order order, Criterion activeStatus) {
        Query query = Query.select(TAG, REMOTE_ID, COUNT, TagData.LAST_ACTIVITY_DATE).
            join(Join.inner(Task.TABLE, Metadata.TASK.eq(Task.ID))).
            join(Join.left(TagData.TABLE, TAG.eq(TagData.NAME))).
            where(Criterion.and(activeStatus, MetadataCriteria.withKey(KEY))).
            orderBy(order).groupBy(TAG);
        TodorooCursor<Metadata> cursor = metadataDao.query(query);
        try {
            Tag[] array = new Tag[cursor.getCount()];
            for (int i = 0; i < array.length; i++) {
                cursor.moveToNext();
                array[i] = new Tag(cursor.get(TAG), cursor.get(COUNT), cursor.get(REMOTE_ID), cursor.get(TagData.LAST_ACTIVITY_DATE));
            }
            return array;
        } finally {
            cursor.close();
        }
    }

    /**
     * Return tags on the given task
     *
     * @param taskId
     * @return cursor. PLEASE CLOSE THE CURSOR!
     */
    public TodorooCursor<Metadata> getTags(long taskId) {
        Query query = Query.select(TAG, REMOTE_ID).where(Criterion.and(MetadataCriteria.withKey(KEY),
                MetadataCriteria.byTask(taskId)));
        return metadataDao.query(query);
    }

    /**
     * Return tags as a comma-separated list of strings
     *
     * @param taskId
     * @return empty string if no tags, otherwise string
     */
    public String getTagsAsString(long taskId) {
        return getTagsAsString(taskId, ", ");
    }

    /**
     * Return tags as a list of strings separated by given separator
     *
     * @param taskId
     * @return empty string if no tags, otherwise string
     */
    public String getTagsAsString(long taskId, String separator) {
        StringBuilder tagBuilder = new StringBuilder();
        TodorooCursor<Metadata> tags = getTags(taskId);
        try {
            int length = tags.getCount();
            Metadata metadata = new Metadata();
            for (int i = 0; i < length; i++) {
                tags.moveToNext();
                metadata.readFromCursor(tags);
                tagBuilder.append(metadata.getValue(TAG));
                if (i < length - 1)
                    tagBuilder.append(separator);
            }
        } finally {
            tags.close();
        }
        return tagBuilder.toString();
    }

    /**
     * Save the given array of tags into the database
     * @param taskId
     * @param tags
     */
    public boolean synchronizeTags(long taskId, LinkedHashSet<String> tags) {
        MetadataService service = PluginServices.getMetadataService();

        ArrayList<Metadata> metadata = new ArrayList<Metadata>();
        for(String tag : tags) {
            Metadata item = new Metadata();
            item.setValue(Metadata.KEY, KEY);
            item.setValue(TAG, tag);
            TagData tagData = tagDataService.getTag(tag, TagData.REMOTE_ID);
            if(tagData != null)
                item.setValue(REMOTE_ID, tagData.getValue(TagData.REMOTE_ID));

            metadata.add(item);
        }

        return service.synchronizeMetadata(taskId, metadata, Metadata.KEY.eq(KEY));
    }

    public int delete(String tag) {
        invalidateTaskCache(tag);
        return PluginServices.getMetadataService().deleteWhere(tagEq(tag, Criterion.all));
    }

    public int rename(String oldTag, String newTag) {
        // First remove newTag from all tasks that have both oldTag and newTag.
        MetadataService metadataService = PluginServices.getMetadataService();
        metadataService.deleteWhere(
                Criterion.and(
                        Metadata.VALUE1.eq(newTag),
                        Metadata.TASK.in(rowsWithTag(oldTag, Metadata.TASK))));

        // Then rename all instances of oldTag to newTag.
        Metadata metadata = new Metadata();
        metadata.setValue(TAG, newTag);
        int ret = metadataService.update(tagEq(oldTag, Criterion.all), metadata);
        invalidateTaskCache(newTag);
        return ret;
    }

    private Query rowsWithTag(String tag, Field... projections) {
        return Query.select(projections).from(Metadata.TABLE).where(Metadata.VALUE1.eq(tag));
    }

    private void invalidateTaskCache(String tag) {
        taskService.clearDetails(Task.ID.in(rowsWithTag(tag, Task.ID)));
        Flags.set(Flags.REFRESH);
    }

}
