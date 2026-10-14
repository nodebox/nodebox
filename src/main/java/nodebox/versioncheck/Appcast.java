package nodebox.versioncheck;

import java.util.List;

/**
 * Contains recent version information for an application.
 */
public class Appcast {

    private String title;
    private String downloadLink;
    private List<AppcastItem> items;

    public Appcast(String title, String downloadLink, List<AppcastItem> items) {
        this.title = title;
        this.downloadLink = downloadLink;
        this.items = items;
    }


    public AppcastItem getLatest() {
        return items.get(0);
    }

    public String getTitle() {
        return title;
    }

    public String getDownloadLink() {
        return downloadLink;
    }
}
