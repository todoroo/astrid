package ru.otdelit.astrid.opencrx.sync;

import org.json.JSONException;
import org.json.JSONObject;

import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.astrid.data.StoreObject;

/**
 *
 * @author Arne Jans <arne.jans@gmail.com>
 */
@SuppressWarnings("nls")
public class OpencrxContact implements Comparable<OpencrxContact> {

    /** type*/
    public static final String TYPE = "opencrx-contacts"; //$NON-NLS-1$

    /** dashboard id in producteev */
    public static final LongProperty REMOTE_ID = new LongProperty(StoreObject.TABLE,
            StoreObject.ITEM.name);

    /** user first name */
    public static final StringProperty FIRST_NAME = new StringProperty(StoreObject.TABLE,
            StoreObject.VALUE1.name);

    /** user last name */
    public static final StringProperty LAST_NAME = new StringProperty(StoreObject.TABLE,
            StoreObject.VALUE2.name);

    /** id in OpenCRX as string */
    public static final StringProperty CRX_ID = new StringProperty(StoreObject.TABLE,
            StoreObject.VALUE3.name);

    private final long id;

    private final String email;

    private final String firstname;

    private final String lastname;

    private final String crxId;

    public OpencrxContact(long id, String email, String firstname,
            String lastname) {
        this.id = id;
        this.email = email;
        this.firstname = firstname;
        this.lastname = lastname;
        this.crxId = "";
    }

    public OpencrxContact(long id, String email, String firstname,
            String lastname, String crxId) {
        this.id = id;
        this.email = email;
        this.firstname = firstname;
        this.lastname = lastname;
        this.crxId = crxId;
    }

    public OpencrxContact(JSONObject elt) throws JSONException {
        this.id = elt.getLong("id");
        this.email = elt.getString("email");
        this.firstname = elt.getString("firstname");
        this.lastname = elt.getString("lastname");
        this.crxId = elt.optString("crx_id");
    }

    public OpencrxContact(StoreObject userData){
        this(userData.getValue(REMOTE_ID), "", userData.getValue(FIRST_NAME), userData.getValue(LAST_NAME),
                userData.getValue(CRX_ID) );
    }

    /**
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * @return the firstname
     */
    public String getFirstname() {
        return firstname;
    }

    /**
     * @return the lastname
     */
    public String getLastname() {
        return lastname;
    }

    /**
     * @return the CRX Id
     */
    public String getCrxId() {
        return crxId;
    }
    /**
     * @return the id
     */
    public long getId() {
        return id;
    }

    @Override
    public String toString() {
        String displayString = "";
        boolean hasFirstname = false;
        boolean hasLastname = false;
        if (firstname != null && firstname.length() > 0) {
            displayString += firstname;
            hasFirstname = true;
        }
        if (lastname != null && lastname.length() > 0)
            hasLastname = true;
        if (hasFirstname && hasLastname)
            displayString += " ";
        if (hasLastname)
            displayString += lastname;

        if (!hasFirstname && !hasLastname && email != null
                && email.length() > 0)
            displayString += email;
        return displayString;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OpencrxContact that = (OpencrxContact) o;

        if (id != that.id) return false;
        if (email != null ? !email.equals(that.email) : that.email != null) return false;
        if (firstname != null ? !firstname.equals(that.firstname) : that.firstname != null) return false;
        if (lastname != null ? !lastname.equals(that.lastname) : that.lastname != null) return false;
        if (crxId != null ? !crxId.equals(that.crxId) : that.crxId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (email != null ? email.hashCode() : 0);
        result = 31 * result + (firstname != null ? firstname.hashCode() : 0);
        result = 31 * result + (lastname != null ? lastname.hashCode() : 0);
        return result;
    }

    @Override
    public int compareTo(OpencrxContact o) {
        int ret = toString().compareTo(o.toString());
        return ret == 0 ? (new Long(id).compareTo(o.id)) : ret;
    }
}
