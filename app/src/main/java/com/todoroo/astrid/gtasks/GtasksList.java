/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import com.todoroo.astrid.data.StoreObject;

/**
 * {@link StoreObject} entries for a GTasks List
 *
 * @author Tim Su <tim@todoroo.com>
 */
public class GtasksList {

    private StoreObject storeObject;

    public GtasksList(final String remoteId) {
        this(newStoreObject());
        setLastSync(0L);
        setRemoteId(remoteId);
    }

    private static StoreObject newStoreObject() {
        StoreObject storeObject = new StoreObject();
        storeObject.setType(GtasksList.TYPE);
        return storeObject;
    }

    public GtasksList(StoreObject storeObject) {
        if (!storeObject.getType().equals(TYPE)) {
            throw new RuntimeException("Type is not " + TYPE);
        }
        this.storeObject = storeObject;
    }

    /**
     * type
     */
    public static final String TYPE = "gtasks-list"; //$NON-NLS-1$

    public Long getId() {
        return storeObject.getId();
    }

    public String getRemoteId() {
        return storeObject.getValue(StoreObject.ITEM);
    }

    private void setRemoteId(String remoteId) {
        storeObject.setValue(StoreObject.ITEM, remoteId);
    }

    public String getName() {
        return storeObject.getValue(StoreObject.VALUE1);
    }

    public void setName(String name) {
        storeObject.setValue(StoreObject.VALUE1, name);
    }

    public void setOrder(int order) {
        storeObject.setValue(StoreObject.VALUE2, Integer.toString(order));
    }

    public int getColor() {
        return storeObject.containsNonNullValue(StoreObject.VALUE4)
                ? Integer.parseInt(storeObject.getValue(StoreObject.VALUE4))
                : -1;
    }

    public void setColor(int color) {
        storeObject.setValue(StoreObject.VALUE4, Integer.toString(color));
    }

    public long getLastSync() {
        return storeObject.containsNonNullValue(StoreObject.VALUE3)
                ? Long.parseLong(storeObject.getValue(StoreObject.VALUE3))
                : 0;
    }

    public void setLastSync(long timestamp) {
        storeObject.setValue(StoreObject.VALUE3, Long.toString(timestamp));
    }

    public StoreObject getStoreObject() {
        return storeObject;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GtasksList)) return false;

        GtasksList that = (GtasksList) o;

        if (storeObject != null ? !storeObject.equals(that.storeObject) : that.storeObject != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return storeObject != null ? storeObject.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "GtasksList{" +
                "storeObject=" + storeObject +
                '}';
    }
}
