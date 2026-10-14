package nodebox.versioncheck;

import org.junit.Test;

import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import static junit.framework.TestCase.*;

/**
 * Checks the appcast that secure.nodebox.net serves (appcast/public/app/nodebox/appcast.xml)
 * with the same parser the app uses.
 */
public class PublishedAppcastTest {

    private static final File APPCAST_FILE = new File("appcast/public/app/nodebox/appcast.xml");

    @Test
    public void testLatestItemIsCurrentVersion() throws Exception {
        Appcast appcast = parse();
        assertEquals("The newest appcast item should be the version in version.properties.",
                new Version(currentVersion()), appcast.getLatest().getVersion());
    }

    @Test
    public void testItemsAreReadable() throws Exception {
        Appcast appcast = parse();
        List<AppcastItem> items = appcast.getItems();
        assertFalse(items.isEmpty());
        Version previous = null;
        for (AppcastItem item : items) {
            assertNotNull("Unreadable pubDate in " + item, item.getDate());
            assertNotNull("Missing appcast:version in " + item, item.getVersion());
            assertNotNull("Missing description in " + item, item.getDescription());
            if (previous != null) {
                assertTrue("Items should go from newest to oldest: " + item.getVersion() + " is after " + previous,
                        previous.compareTo(item.getVersion()) > 0);
            }
            previous = item.getVersion();
        }
    }

    @Test
    public void testDownloadLink() throws Exception {
        assertEquals("https://nodebox.net/download/", parse().getDownloadLink());
    }

    private static Appcast parse() throws Exception {
        AppcastHandler handler = new AppcastHandler();
        SAXParserFactory.newInstance().newSAXParser().parse(APPCAST_FILE, handler);
        return handler.getAppcast();
    }

    private static String currentVersion() throws Exception {
        Properties properties = new Properties();
        try (InputStream in = PublishedAppcastTest.class.getResourceAsStream("/version.properties")) {
            properties.load(in);
        }
        return properties.getProperty("nodebox.version");
    }

}
