package ru.otdelit.astrid.opencrx.xml;

import java.text.ParseException;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import ru.otdelit.astrid.opencrx.api.OpencrxUtils;
import ru.otdelit.astrid.opencrx.sync.OpencrxResourceAssignment;




@SuppressWarnings("nls")
public class ResourceAssignmentParser extends BaseParser{

	private final List<OpencrxResourceAssignment> destination;
	private OpencrxResourceAssignment current;

	public ResourceAssignmentParser(List<OpencrxResourceAssignment> destination){
		this.destination = destination;
	}

    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {

        if (qName.equals("resource"))
            current.setResourceId(OpencrxUtils.getBaseXri(buffer.toString()));

        if (qName.equals("org.opencrx.kernel.activity1.ResourceAssignment"))
            destination.add(current);

        if (qName.equals("createdAt")){
            try {
                current.setAssignmentDate(OpencrxUtils.parseFromOpencrx(buffer.toString()));
            } catch (ParseException e) {
                throw new SAXException("Unparseable date in CreatedAt field of ResourceAssignment. Reason: " + e.getMessage());
            }
        }

        super.endElement(uri, localName, qName);
    }

    @Override
    public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException {
        super.startElement(uri, localName, qName, attributes);

        if (qName.equals("org.opencrx.kernel.activity1.ResourceAssignment")){
            current = new OpencrxResourceAssignment();

            current.setAssignmentId(attributes.getValue("id"));
        }

        if (qName.equals("resource") || qName.equals("createdAt"))
            initBuffer();

    }
}
