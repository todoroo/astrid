package ru.otdelit.astrid.opencrx.xml;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import ru.otdelit.astrid.opencrx.api.OpencrxUtils;



import android.text.TextUtils;


@SuppressWarnings("nls")
public class ActivityParser extends BaseParser {

    private final static List<String> tags = Arrays.asList("name", "createdAt", "dueBy", "priority", "lastAppliedCreator", "assignedTo",
            "processState", "modifiedAt");

    private final JSONArray destination;
    private final String closedStateId;
    private final String completeStateId;

    private JSONObject task;

	public ActivityParser(JSONArray destination, String completeStateId, String closedStateid) {
        this.destination = destination;
        this.closedStateId = closedStateid;
        this.completeStateId = completeStateId;
    }

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {

	    if (task == null){
	        super.endElement(uri, localName, qName);
	        return;
	    }

        try {
            if (qName.equals("name")){
                 task.put("title", buffer.toString());
            }

            if (qName.equals("processState")){
                String stateId = OpencrxUtils.getBaseXri(buffer.toString());

                if (TextUtils.isEmpty(stateId))
                    return;

                if (stateId.equals(completeStateId))
                    task.put("status", 1);

                if (stateId.equals(closedStateId))
                    task.put("deleted", 1);
            }

            if (qName.equals("createdAt")){
                String raw = buffer.toString();

                if (!TextUtils.isEmpty(raw)){
                    Date createdAt = OpencrxUtils.parseFromOpencrx(raw);
                    String asProducteev = OpencrxUtils.formatAsProducteev(createdAt);

                    task.put("time_created", asProducteev);
                }
            }

            if (qName.equals("modifiedAt")){
                String raw = buffer.toString();

                if (!TextUtils.isEmpty(raw)){
                    Date modifiedAt = OpencrxUtils.parseFromOpencrx(raw);

                    task.put("modifiedAt", modifiedAt.getTime());
                }

            }

            if (qName.equals("dueBy")){
                String raw = buffer.toString();

                if (!TextUtils.isEmpty(raw)){

                    Date createdAt = OpencrxUtils.parseFromOpencrx(raw);
                    String asProducteev = OpencrxUtils.formatAsProducteev(createdAt);

                    task.put("deadline", asProducteev);
                }
            }

            if (qName.equals("priority")){
                task.put("star", buffer.toString());
            }

            if (qName.equals("lastAppliedCreator")){
                String creatorId = OpencrxUtils.getBaseXri(buffer.toString());
                task.put("id_dashboard", OpencrxUtils.hash(creatorId) );
            }

            if (qName.equals("assignedTo")){
                String contactId = OpencrxUtils.getBaseXri(buffer.toString());
                task.put("id_responsible", OpencrxUtils.hash(contactId) );
            }
        } catch (ParseException e) {
            throw new SAXException(e);
        } catch (JSONException e) {
            throw new SAXException(e);
        }

		if (qName.startsWith("org.opencrx.kernel.activity1")){
			destination.put(task);
		}

		super.endElement(uri, localName, qName);

	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {

		super.startElement(uri, localName, qName, attributes);

		if (qName.startsWith("org.opencrx.kernel.activity1")){

		    task = new JSONObject();

		    String id = attributes.getValue("id");

            try {
                task.put("repeating_value", id);
                task.put("id_task", OpencrxUtils.hash(id) );
                task.put("status", 0);
                task.put("deleted", 0);
                task.put("labels", new JSONArray());

                task.put("title", "");
                task.put("time_created", "");
                task.put("deadline", "");
                task.put("star", "0");
                task.put("id_dashboard", 0);
                task.put("id_responsible", 0);
                task.put("modifiedAt", 0);
            } catch (JSONException e) {
                throw new SAXException(e);
            }

		}

        if (tags.contains(qName))
            initBuffer();

	}

}
