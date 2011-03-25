package ru.otdelit.astrid.opencrx.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

@SuppressWarnings("nls")
public class ActivityCurrentProcessParser extends BaseParser{


    private String processId;
	private String processStateId;

	public String getProcessId() {
		return processId;
	}

	public String getProcessStateId() {
		return processStateId;
	}

    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        if (qName.equals("processState")){
            String stateXri = buffer.toString();
            System.out.println("State XRI: " + stateXri);

            String arr[] = stateXri.split("/");

            if (arr.length < 3){
                processId = null;
                processStateId = null;
            }else{
                processId = arr[arr.length - 3];
                processStateId = arr[arr.length - 1];
            }
        }

        super.endElement(uri, localName, qName);
    }

    @Override
    public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException {
        super.startElement(uri, localName, qName, attributes);

        if (qName.equals("processState")){
            buffer = new StringBuilder();
        }
    }


}
