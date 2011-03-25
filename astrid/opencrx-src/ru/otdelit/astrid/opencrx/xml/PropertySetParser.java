package ru.otdelit.astrid.opencrx.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class PropertySetParser extends BaseParser{
    private String id;

    public String getId(){
        return id;
    }

    @SuppressWarnings("nls")
    @Override
    public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException {
        super.startElement(uri, localName, qName, attributes);

        if (qName.equals("org.opencrx.kernel.generic.PropertySet")){
            id = attributes.getValue("href");
        }
    }


}
