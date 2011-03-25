package ru.otdelit.astrid.opencrx.sync;

import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.astrid.data.StoreObject;

/**
 * {@link StoreObject} entries for a Producteev Dashboard
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class OpencrxActivityCreator {

    /** type*/
    public static final String TYPE = "opencrx-creator"; //$NON-NLS-1$

    /** creator id in producteev */
    public static final LongProperty REMOTE_ID = new LongProperty(StoreObject.TABLE,
            StoreObject.ITEM.name);

    /** creator name */
    public static final StringProperty NAME = new StringProperty(StoreObject.TABLE,
            StoreObject.VALUE1.name);

    /**
     * String ID in OpenCRX system (ActivityCreator)
     */
    public static final StringProperty CRX_ID = new StringProperty(StoreObject.TABLE,
            StoreObject.VALUE3.name);

    // data class-part
    private final long id;

    private final String name;
    private final String crxId;

    public OpencrxActivityCreator (StoreObject dashboardData) {
        this(dashboardData.getValue(REMOTE_ID),dashboardData.getValue(NAME),
               dashboardData.containsValue(CRX_ID) ? dashboardData.getValue(CRX_ID) : ""); //$NON-NLS-1$
    }

    /**
     * Constructor for a dashboard.
     *
     * @param id id of the remote dashboard
     * @param name name of the remote dashboard
     * @param usercsv csv-userstring as returned by a StoreObject-dashboard with property ProducteevDashboard.USERS
     */
    public OpencrxActivityCreator(long id, String name, String crxId) {
        this.id = id;
        this.name = name;
        this.crxId = crxId;
    }

    /**
     * @return the id
     */
    public long getId() {
        return id;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    public String getCrxId() {
        return crxId;
    }
    /**
     * return the name of this dashboard
     */
    @Override
    public String toString() {
        return name;
    }
}
