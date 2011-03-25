package ru.otdelit.astrid.opencrx.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import ru.otdelit.astrid.opencrx.api.OpencrxUtils;




@SuppressWarnings("nls")
public class ActivityCreationResultParser extends BaseParser{
	private String id;

	public String getId() {
		return id;
	}

    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {

        if (qName.equals("activity")){
            id = OpencrxUtils.getBaseXri(buffer.toString());
        }

        super.endElement(uri, localName, qName);

    }

    @Override
    public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException {
        super.startElement(uri, localName, qName, attributes);

        if (qName.equals("activity")){
            buffer = new StringBuilder();
        }
    }

}
