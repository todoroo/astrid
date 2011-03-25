package ru.otdelit.astrid.opencrx.xml;

import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import ru.otdelit.astrid.opencrx.sync.OpencrxActivityProcessState;



@SuppressWarnings("nls")
public class ActivityProcessStateParser extends BaseParser{

	private final List<OpencrxActivityProcessState> dest;
	private OpencrxActivityProcessState current;

    public ActivityProcessStateParser(List<OpencrxActivityProcessState> dest) {
		this.dest = dest;
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {

		if (qName.equals("name"))
			current.setName(buffer.toString());

        if (qName.equals("org.opencrx.kernel.activity1.ActivityProcessState"))
        	dest.add(current);

		super.endElement(uri, localName, qName);
	}

    @Override
    public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException {

        super.startElement(uri, localName, qName, attributes);

        if (qName.equals("org.opencrx.kernel.activity1.ActivityProcessState")){
        	current = new OpencrxActivityProcessState();

            current.setId(attributes.getValue("id"));
        }

        if (qName.equals("name"))
        	initBuffer();

    }

}
