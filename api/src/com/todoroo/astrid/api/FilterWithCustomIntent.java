package com.todoroo.astrid.api;


import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

import com.todoroo.andlib.sql.QueryTemplate;

public class FilterWithCustomIntent extends Filter {

    /**
     * Custom activity name
     */
    public ComponentName customTaskList = null;

    /**
     * Intent with extras set. Can be null
     */
    public Intent customExtras = null;

    /**
     * Count override - if set, count will be displayed here instead of by
     * executing the SQL command. Use when actual task count differs from what
     * is in the database
     */
    public int countOverride = -1;

    protected FilterWithCustomIntent() {
        super();
    }

    public FilterWithCustomIntent(String listingTitle, String title,
            QueryTemplate sqlQuery, ContentValues valuesForNewTasks) {
        super(listingTitle, title, sqlQuery, valuesForNewTasks);
    }

    public FilterWithCustomIntent(String listingTitle, String title,
            String sqlQuery, ContentValues valuesForNewTasks) {
        super(listingTitle, title, sqlQuery, valuesForNewTasks);
    }


    // --- parcelable

    /**
     * {@inheritDoc}
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeParcelable(customTaskList, 0);
        dest.writeParcelable(customExtras, 0);
        dest.writeInt(countOverride);
    }

    @Override
    public void readFromParcel(Parcel source) {
        super.readFromParcel(source);
        customTaskList = source.readParcelable(ComponentName.class.getClassLoader());
        customExtras = source.readParcelable(Intent.class.getClassLoader());
        countOverride = source.readInt();
    }

    /**
     * Parcelable Creator Object
     */
    @SuppressWarnings("hiding")
    public static final Parcelable.Creator<FilterWithCustomIntent> CREATOR = new Parcelable.Creator<FilterWithCustomIntent>() {

        /**
         * {@inheritDoc}
         */
        public FilterWithCustomIntent createFromParcel(Parcel source) {
            FilterWithCustomIntent item = new FilterWithCustomIntent();
            item.readFromParcel(source);
            return item;
        }

        /**
         * {@inheritDoc}
         */
        public FilterWithCustomIntent[] newArray(int size) {
            return new FilterWithCustomIntent[size];
        }

    };

}
