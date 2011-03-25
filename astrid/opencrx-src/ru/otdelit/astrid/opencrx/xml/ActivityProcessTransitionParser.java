package ru.otdelit.astrid.opencrx.xml;

import java.util.Arrays;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import ru.otdelit.astrid.opencrx.api.OpencrxUtils;
import ru.otdelit.astrid.opencrx.sync.OpencrxActivityProcessState;
import ru.otdelit.astrid.opencrx.sync.OpencrxActivityProcessTransition;




@SuppressWarnings("nls")
public class ActivityProcessTransitionParser extends BaseParser{

    private final static List<String> tags = Arrays.asList("name", "prevState", "nextState");

	private final List<OpencrxActivityProcessTransition> result;
	private OpencrxActivityProcessTransition current;

	public ActivityProcessTransitionParser(List<OpencrxActivityProcessTransition> dest){
		result = dest;
	}

	public List<OpencrxActivityProcessTransition> getResults() {
		return result;
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {

	    if (qName.equals("name"))
	        current.setName(buffer.toString());

        if (qName.equals("prevState")){
            String prevStateXri = buffer.toString();

            current.setPrevState(new OpencrxActivityProcessState(OpencrxUtils.getBaseXri(prevStateXri), null));
        }

        if (qName.equals("nextState")){
            String nextStateXri = buffer.toString();

            current.setNextState(new OpencrxActivityProcessState(OpencrxUtils.getBaseXri(nextStateXri), null));
        }

		if (qName.equals("org.opencrx.kernel.activity1.ActivityProcessTransition"))
			result.add(current);

		super.endElement(uri, localName, qName);

	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {

		super.startElement(uri, localName, qName, attributes);

		if (qName.equals("org.opencrx.kernel.activity1.ActivityProcessTransition")){
			current = new OpencrxActivityProcessTransition();

			current.setId(attributes.getValue("id"));
		}

		if (tags.contains(qName))
		    initBuffer();

	}
}
