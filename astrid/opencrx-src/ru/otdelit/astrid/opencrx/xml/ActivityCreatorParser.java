package ru.otdelit.astrid.opencrx.xml;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import ru.otdelit.astrid.opencrx.api.OpencrxUtils;




@SuppressWarnings("nls")
public class ActivityCreatorParser extends BaseParser{

    private final JSONArray destination;
    private JSONObject dashboard;

	public ActivityCreatorParser(JSONArray destination) {
        this.destination = destination;
    }

    @Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {

        try{
            if (qName.equals("name"))
                dashboard.put("title", buffer.toString());
        }catch(Exception e){
            throw new SAXException(e);

        }

		if (qName.equals("org.opencrx.kernel.activity1.ActivityCreator")){

		    JSONObject wrap = new JSONObject();
		    try {
                wrap.put("dashboard", dashboard);
            } catch (JSONException e) {
                throw new SAXException(e);
            }

		    destination.put(wrap);

		}

		super.endElement(uri, localName, qName);

	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {

		super.startElement(uri, localName, qName, attributes);

		if (qName.equals("org.opencrx.kernel.activity1.ActivityCreator")){
            dashboard = new JSONObject();

            try {
                dashboard.put("deleted", 0);
                dashboard.put("accesslist", new JSONArray());
                dashboard.put("title", "");

                String crxId = attributes.getValue("id");
                dashboard.put("id_dashboard", OpencrxUtils.hash(crxId) );
                dashboard.put("crx_id", crxId);
            } catch (JSONException e) {
                throw new SAXException(e);
            }

		}

		if (qName.equals("name"))
            buffer = new StringBuilder();

	}

}
