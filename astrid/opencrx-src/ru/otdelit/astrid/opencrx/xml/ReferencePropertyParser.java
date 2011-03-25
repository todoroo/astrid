package ru.otdelit.astrid.opencrx.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

@SuppressWarnings("nls")
public class ReferencePropertyParser extends BaseParser{

    private String referenceValueXri;
    private String referencePropertyAddress;

    public String getReferencePropertyAddress() {
        return referencePropertyAddress;
    }

    public String getReferenceValueXri(){
        return referenceValueXri;
    }

    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        if (qName.equals("referenceValue")){
            referenceValueXri = buffer.toString();
        }

        super.endElement(uri, localName, qName);
    }

    @Override
    public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException {
        super.startElement(uri, localName, qName, attributes);

        if (qName.equals("org.opencrx.kernel.base.ReferenceProperty")){
            referencePropertyAddress = attributes.getValue("href");
        }

        if (qName.equals("referenceValue")){
            initBuffer();
        }
    }


}
