package nodebox.versioncheck;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class AppcastHandler extends DefaultHandler {

    public static final String TAG_ITEM = "item";
    public static final String TAG_TITLE = "title";
    public static final String TAG_LINK = "link";
    public static final String TAG_DESCRIPTION = "description";
    public static final String TAG_APPCAST_VERSION = "appcast:version";
    public static final String TAG_PUB_DATE = "pubDate";

    private List<AppcastItem> items = new ArrayList<AppcastItem>();
    private Properties currentItemProperties;
    private StringBuffer characterData = new StringBuffer();
    private String title;
    private String link;

    public Appcast getAppcast() {
        return new Appcast(title, link, items);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        characterData = new StringBuffer();
        if (qName.equals(TAG_ITEM)) {
            currentItemProperties = new Properties();
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        String text = characterData.toString();
        if (qName.equals(TAG_ITEM)) {
            AppcastItem item = new AppcastItem(currentItemProperties);
            items.add(item);
            currentItemProperties = null;
        } else if (qName.equals(TAG_TITLE)) {
            // We're after the title tag in item, not the one at the top. 
            if (currentItemProperties != null) {
                currentItemProperties.setProperty(TAG_TITLE, text);
            } else {
                title = text;
            }
        } else if (qName.equals(TAG_DESCRIPTION)) {
            // We're after the description tag in item.
            if (currentItemProperties != null) {
                currentItemProperties.setProperty(TAG_DESCRIPTION, text);
            }
        } else if (qName.equals(TAG_LINK)) {
            link = text;
        } else if (qName.equals(TAG_APPCAST_VERSION)) {
            currentItemProperties.setProperty(TAG_APPCAST_VERSION, text);
        } else if (qName.equals(TAG_PUB_DATE)) {
            currentItemProperties.setProperty(TAG_PUB_DATE, text);
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        // We have a valid character state, so we can safely append to characterData.
        characterData.append(ch, start, length);
    }
}
