package com.todoroo.astrid.tags;

import java.util.HashMap;

import android.content.Context;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.service.TaskService;

public class TagCaseMigrator {

    @Autowired TaskService taskService;
    @Autowired TagDataService tagDataService;
    @Autowired MetadataService metadataService;

    public static final String PREF_SHOW_MIGRATION_ALERT = "tag_case_migration_alert"; //$NON-NLS-1$
    private static final String PREF_CASE_MIGRATION_PERFORMED = "tag_case_migration"; //$NON-NLS-1$

    public TagCaseMigrator() {
        DependencyInjectionService.getInstance().inject(this);
    }

    private final HashMap<String, String> renameMap = new HashMap<String, String>();
    private final HashMap<String, Long> nameToRemoteId = new HashMap<String, Long>();
    private final HashMap<String, Integer> nameCountMap = new HashMap<String, Integer>();

    public void performTagCaseMigration(@SuppressWarnings("unused") Context context) {
        if (!Preferences.getBoolean(PREF_CASE_MIGRATION_PERFORMED, false)) {
            TagService.Tag[] allTagData = TagService.getInstance().getGroupedTags(
                    TagService.GROUPED_TAGS_BY_ALPHA, Criterion.all, false);

            boolean shouldShowDialog = false;
            for (int i = 0; i < allTagData.length - 1; i++) {
                TagService.Tag first = allTagData[i];
                TagService.Tag second = allTagData[i+1];

                if (first.tag.equalsIgnoreCase(second.tag)) {
                    shouldShowDialog = true;
                    markForRenaming(first.tag, first.remoteId);
                    markForRenaming(second.tag, second.remoteId);
                }
            }

            for (String key : renameMap.keySet()) {
                TagService.getInstance().renameCaseSensitive(key, renameMap.get(key));
                updateTagData(key);
            }

            Preferences.setBoolean(PREF_CASE_MIGRATION_PERFORMED, true);
            Preferences.setBoolean(PREF_SHOW_MIGRATION_ALERT, shouldShowDialog);
        }
    }

    private String targetNameForTag(String tag) {
        String targetName = tag.toLowerCase();
        targetName = targetName.substring(0, 1).toUpperCase() + targetName.substring(1);
        return targetName;
    }

    private void markForRenaming(String tag, long remoteId) {
        if (renameMap.containsKey(tag)) return;

        String targetName = targetNameForTag(tag);

        int suffix = 1;
        if (nameCountMap.containsKey(targetName)) {
            suffix = nameCountMap.get(targetName);
        }

        String newName = targetName + "_" + suffix; //$NON-NLS-1$
        nameCountMap.put(targetName, suffix + 1);
        renameMap.put(tag, newName);
        nameToRemoteId.put(tag, remoteId);
    }

    private void updateTagData(String tag) {
        long remoteId = nameToRemoteId.get(tag);
        TodorooCursor<TagData> tagData = tagDataService.query(Query.select(TagData.NAME, TagData.REMOTE_ID)
                                                                   .where(Criterion.and(
                                                                           TagData.NAME.eq(tag), TagData.REMOTE_ID.eq(remoteId))));
        try {
            for (tagData.moveToFirst(); !tagData.isAfterLast(); tagData.moveToNext()) {
                TagData curr = new TagData(tagData);
                curr.setValue(TagData.NAME, renameMap.get(tag));
                tagDataService.save(curr);
            }
        } finally {
            tagData.close();
        }

        addTasksToTargetTag(renameMap.get(tag), targetNameForTag(tag));
    }

    private void addTasksToTargetTag(String tag, String target) {
        TodorooCursor<Task> tasks = taskService.query(Query.select(Task.ID).join(Join.inner(Metadata.TABLE,
                    Task.ID.eq(Metadata.TASK))).where(TagService.tagEq(tag, Criterion.all)));
        try {
            for (tasks.moveToFirst(); !tasks.isAfterLast(); tasks.moveToNext()) {
                Task curr = new Task(tasks);
                TodorooCursor<Metadata> tagMetadata = metadataService.query(Query.select(TagService.TAG)
                                                                 .where(Criterion.and(TagService.TAG.eq(target), Metadata.KEY.eq(TagService.KEY), Metadata.TASK.eq(curr.getId()))));
                try {
                    if (tagMetadata.getCount() == 0) {
                        Metadata newTag = new Metadata();
                        newTag.setValue(Metadata.KEY, TagService.KEY);
                        newTag.setValue(Metadata.TASK, curr.getId());
                        newTag.setValue(TagService.TAG, target);
                        metadataService.save(newTag);
                    } // else already exists for some weird reason
                } finally {
                    tagMetadata.close();
                }
            }
        } finally {
            tasks.close();
        }

    }
}
