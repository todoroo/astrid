package ru.otdelit.astrid.opencrx.xml;

import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import ru.otdelit.astrid.opencrx.api.OpencrxUtils;
import ru.otdelit.astrid.opencrx.sync.OpencrxContact;



@SuppressWarnings("nls")
public class ContactParser extends BaseParser{

    private final List<OpencrxContact> destination;

    private String crxId;
    private long id;
    private String firstName;
    private String lastName;

	public ContactParser(List<OpencrxContact> destination) {
        this.destination = destination;
    }

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {

	    if (qName.equals("firstName"))
            firstName = buffer.toString();

	    if (qName.equals("lastName"))
            lastName = buffer.toString();

		if (qName.equals("org.opencrx.kernel.account1.Contact"))
		    destination.add(new OpencrxContact(id, "", firstName, lastName, crxId));

		super.endElement(uri, localName, qName);

	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

		super.startElement(uri, localName, qName, attributes);

		if (qName.equals("org.opencrx.kernel.account1.Contact")){

			crxId = attributes.getValue("id");
			id = OpencrxUtils.hash(crxId);

		}

		if (qName.equals("firstName"))
            initBuffer();

        if (qName.equals("lastName"))
            initBuffer();

	}

}
