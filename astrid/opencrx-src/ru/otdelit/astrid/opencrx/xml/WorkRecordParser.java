package ru.otdelit.astrid.opencrx.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import ru.otdelit.astrid.opencrx.api.OpencrxUtils;



import android.text.TextUtils;


@SuppressWarnings("nls")
public class WorkRecordParser extends BaseParser{

    private final String resourceId;

	private int elapsedSeconds = 0;

	private String currentUom;
	private double currentQuantity;
	private boolean count;

	public WorkRecordParser(String resourceId) {
        this.resourceId = resourceId;
    }

    public int getElapsedSeconds() {
		return elapsedSeconds;
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {

	    if (qName.equals("quantity")){
            String q = buffer.toString();
            if (!TextUtils.isEmpty(q))
                currentQuantity = Double.parseDouble(q);
            else
                currentQuantity = 0.0;
        }

        if (qName.equals("quantityUom")){
            currentUom = buffer.toString();
        }

        if (qName.equals("resource")){
            String id = OpencrxUtils.getBaseXri(buffer.toString());
            if (!resourceId.equals(id))
                count = false;
        }

		if (qName.equals("org.opencrx.kernel.activity1.ActivityWorkRecord")){

		    if (currentUom == null || !count){
		        super.endElement(uri, localName, qName);
		        return;
		    }

			if (currentUom.endsWith("sec"))
				elapsedSeconds += (int) (currentQuantity);
			else if (currentUom.endsWith("min"))
				elapsedSeconds += (int) (currentQuantity * 60.0);
			else if (currentUom.endsWith("day"))
				elapsedSeconds += (int) (currentQuantity * 86400.0);
			else
				elapsedSeconds += (int) (currentQuantity * 3600.0);
		}

		super.endElement(uri, localName, qName);

	}

    @Override
    public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException {
        super.startElement(uri, localName, qName, attributes);

        if (qName.equals("org.opencrx.kernel.activity1.ActivityWorkRecord")){
            count = true;
        }

        if (qName.equals("quantity") || qName.equals("quantityUom") || qName.equals("resource")){
            initBuffer();
        }

    }



}
