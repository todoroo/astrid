package ru.otdelit.astrid.opencrx.xml;

import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import ru.otdelit.astrid.opencrx.api.OpencrxUtils;




@SuppressWarnings("nls")
public class ResourceContactIdParser extends BaseParser{

    private final List<String> destination;

    public ResourceContactIdParser(List<String> destination){
        this.destination = destination;
    }

    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {

        if (qName.equals("contact")){
            String contactId = OpencrxUtils.getBaseXri(buffer.toString());
            if (contactId != null)
                destination.add(contactId);
        }

        super.endElement(uri, localName, qName);
    }

    @Override
    public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException {
        super.startElement(uri, localName, qName, attributes);

        if (qName.equals("contact"))
            initBuffer();

    }

}
