package ru.otdelit.astrid.opencrx.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

@SuppressWarnings("nls")
public class BaseParser extends DefaultHandler{

	private boolean isResultSet = false;
	private boolean hasMore = false;

	protected StringBuilder buffer = new StringBuilder();

	public boolean isResultSet() {
		return isResultSet;
	}

	public boolean hasMore() {
		return hasMore;
	}

	@Override
    public final void characters(char[] ch, int start, int length)
            throws SAXException {
	    if (buffer != null)
	        buffer.append(ch, start, length);
    }

    @Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {

		if (qName.equals("org.openmdx.kernel.ResultSet")){
			isResultSet = true;

			if ("true".equals(attributes.getValue("hasMore")) )
				hasMore = true;
		}

	}

    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        buffer = null;
    }

    protected void initBuffer() {
        buffer = new StringBuilder();
    }




}
