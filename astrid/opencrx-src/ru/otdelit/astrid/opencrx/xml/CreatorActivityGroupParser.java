package ru.otdelit.astrid.opencrx.xml;

import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

@SuppressWarnings("nls")
public class CreatorActivityGroupParser extends BaseParser{

    private final List<String> dest;
    private boolean activityGroup = false;

    public CreatorActivityGroupParser(List<String> dest){
        this.dest = dest;
    }

    @Override
    public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException {
        super.startElement(uri, localName, qName, attributes);

        if (qName.equals("activityGroup"))
            activityGroup = true;

        if (activityGroup && qName.equals("_item"))
            initBuffer();

    }

    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {

        if (qName.equals("activityGroup"))
            activityGroup = false;

        if (activityGroup && qName.equals("_item"))
            dest.add(buffer.toString());

        super.endElement(uri, localName, qName);
    }


}
