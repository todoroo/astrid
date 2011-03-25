package ru.otdelit.astrid.opencrx.xml;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import ru.otdelit.astrid.opencrx.api.OpencrxUtils;




@SuppressWarnings("nls")
public class ResourceParser extends BaseParser{

    private final JSONArray destination;
    private JSONObject label;

	public ResourceParser(JSONArray destination) {
        this.destination = destination;
    }

	@Override
	public void endElement(String uri, String localName, String qName)throws SAXException {

	    if (label == null){
	        super.endElement(uri, localName, qName);
	        return;
	    }

        if (qName.equals("name")){
            try {
                label.put("name", buffer.toString());
            } catch (JSONException e) {
                throw new SAXException(e);
            }
        }

		if (qName.equals("org.opencrx.kernel.activity1.Resource")){
			destination.put(label);
		}

		if (qName.equals("disabled")){
		    if ("true".equals(buffer.toString()))
		        label = null;
		}

 		if (qName.equals("contact")){
 		    try {
 		        label.put("contact_id", OpencrxUtils.getBaseXri(buffer.toString()));
            } catch (JSONException e) {
                throw new SAXException(e);
            }
		}

		super.endElement(uri, localName, qName);

	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {

		super.startElement(uri, localName, qName, attributes);

		if (qName.equals("org.opencrx.kernel.activity1.Resource")){

		    label = new JSONObject();

			String id = attributes.getValue("id");
			try {
                label.put("id", id);
                label.put("name", "");
            } catch (JSONException e) {
                throw new SAXException(e);
            }
		}

		if (qName.equals("name") || qName.equals("disabled") || qName.equals("contact"))
		    initBuffer();

	}

}
