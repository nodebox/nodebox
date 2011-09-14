package nodebox.versioncheck;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

public class AppcastItem {

    private static final SimpleDateFormat RSSDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);

    private final Properties properties;
    private String title;
    private String description;
    private Date date;
    private Version version;

    public AppcastItem(Properties properties) {
        this.properties = properties;
        try {
            title = properties.getProperty(AppcastHandler.TAG_TITLE);
            description = properties.getProperty(AppcastHandler.TAG_DESCRIPTION);
            date = RSSDateFormat.parse(properties.getProperty(AppcastHandler.TAG_PUB_DATE));
            version = new Version(properties.getProperty(AppcastHandler.TAG_APPCAST_VERSION));
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Date getDate() {
        return date;
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
