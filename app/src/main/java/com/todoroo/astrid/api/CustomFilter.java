package com.todoroo.astrid.api;

import android.content.ContentValues;
import android.os.Parcel;
import android.os.Parcelable;

import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.core.SavedFilter;
import com.todoroo.astrid.data.StoreObject;

public class CustomFilter extends Filter {
    private long id;

    private CustomFilter() {

    }

    public CustomFilter(String listingTitle, String sql, ContentValues contentValues, long id) {
        super(listingTitle, sql, contentValues);
        this.id = id;
    }

    public StoreObject toStoreObject() {
        StoreObject storeObject = new StoreObject();
        storeObject.setId(id);
        storeObject.setValue(SavedFilter.NAME, listingTitle);
        storeObject.setValue(SavedFilter.SQL, sqlQuery);
        if (valuesForNewTasks != null && valuesForNewTasks.size() > 0) {
            storeObject.setValue(SavedFilter.VALUES, AndroidUtilities.contentValuesToSerializedString(valuesForNewTasks));
        }
        return storeObject;
    }

    public long getId() {
        return id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeLong(id);
    }

    @Override
    protected void readFromParcel(Parcel source) {
        super.readFromParcel(source);
        id = source.readLong();
    }

    /**
     * Parcelable Creator Object
     */
    public static final Parcelable.Creator<CustomFilter> CREATOR = new Parcelable.Creator<CustomFilter>() {

        /**
         * {@inheritDoc}
         */
        @Override
        public CustomFilter createFromParcel(Parcel source) {
            CustomFilter item = new CustomFilter();
            item.readFromParcel(source);
            return item;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public CustomFilter[] newArray(int size) {
            return new CustomFilter[size];
        }
    };
}
