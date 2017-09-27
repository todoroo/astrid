/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao;

import com.todoroo.andlib.data.Callback;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.TagData;

import org.tasks.injection.ApplicationScope;

import javax.inject.Inject;

/**
 * Data Access layer for {@link TagData}-related operations.
 *
 * @author Tim Su <tim@todoroo.com>
 */
@ApplicationScope
public class TagDataDao {

    private final RemoteModelDao<TagData> dao;

    @Inject
    public TagDataDao(Database database) {
        dao = new RemoteModelDao<>(database, TagData.class);
    }

    /**
     * Fetch a model object by UUID
     */
    public TagData fetch(String uuid, Property<?>... properties) {
        return dao.getFirst(Query.select(properties).where(RemoteModel.UUID_PROPERTY.eq(uuid)));
    }

    public TagData getTagByName(String name, Property<?>... properties) {
        return dao.getFirst(Query.select(properties).where(TagData.NAME.eqCaseInsensitive(name)));
    }

    public void allTags(Callback<TagData> callback) {
        // TODO: does this need to be ordered?
        Query query = Query.select(TagData.PROPERTIES)
                .where(TagData.DELETION_DATE.eq(0))
                .orderBy(Order.asc(TagData.ID));
        dao.query(query, callback);
    }

    public TagData getByUuid(String uuid) {
        return getByUuid(uuid, TagData.PROPERTIES);
    }

    public TagData getByUuid(String uuid, Property<?>... properties) {
        return dao.getFirst(Query.select(properties).where(TagData.UUID.eq(uuid)));
    }

    public void tagDataOrderedByName(Callback<TagData> callback) {
        Query query = Query.select(TagData.PROPERTIES).where(Criterion.and(
                TagData.DELETION_DATE.eq(0),
                TagData.NAME.isNotNull()))
                .orderBy(Order.asc(Functions.upper(TagData.NAME)));
        dao.query(query, callback);
    }

    public void persist(TagData tagData) {
        dao.persist(tagData);
    }

    public void update(Criterion where, TagData template) {
        dao.update(where, template);
    }

    public void delete(long id) {
        dao.delete(id);
    }

    public void createNew(TagData tag) {
        dao.createNew(tag);
    }
}

