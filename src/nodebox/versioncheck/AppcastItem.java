package nodebox.versioncheck;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public class AppcastItem {

    private static final SimpleDateFormat RSSDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");

    private final Properties properties;
    private String title;
    private Date date;
    private URL releaseNotesURL;
    private Version version;

    public AppcastItem(Properties properties) {
        this.properties = properties;
        try {
            title = properties.getProperty(AppcastHandler.TAG_TITLE);
            releaseNotesURL = new URL(properties.getProperty(AppcastHandler.TAG_APPCAST_RELEASE_NOTES_LINK));
            date = RSSDateFormat.parse(properties.getProperty(AppcastHandler.TAG_PUB_DATE));
            version = new Version(properties.getProperty(AppcastHandler.TAG_APPCAST_VERSION));
        } catch (MalformedURLException e) {
            System.out.println("Bad release notes link: " + properties.getProperty(AppcastHandler.TAG_APPCAST_RELEASE_NOTES_LINK));
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public String getTitle() {
        return title;
    }

    public Date getDate() {
        return date;
    }

    public URL getReleaseNotesURL() {
        return releaseNotesURL;
    }

    public Version getVersion() {
        return version;
    }

    public Properties getProperties() {
        return properties;
    }

    @Override
    public String toString() {
        return "AppcastItem{" +
                "title='" + title + '\'' +
                ", date=" + date +
                ", version='" + version + '\'' +
                '}';
    }

    public boolean isNewerThan(Host host) {
        return version.compareTo(host.getVersion()) > 0;
    }
}
