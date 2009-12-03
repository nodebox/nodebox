package nodebox.versioncheck;

import javax.swing.*;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class UpdateChecker extends Thread implements Runnable {

    private Updater updater;
    private boolean silent;

    public UpdateChecker(Updater updater, boolean silent) {
        this.updater = updater;
        this.silent = silent;
    }

    public boolean isSilent() {
        return silent;
    }

    public void run() {
        try {
            URL appcastURL = new URL(updater.getHost().getAppcastURL());
            // Get contents of URL.
            URLConnection conn = appcastURL.openConnection();
            // Parse XML contents.
            final Appcast appcast = parseAppcastXML(conn.getInputStream());
            // Inform the updater that a check was performed.
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    updater.checkCompleted(UpdateChecker.this, appcast);
                }
            });
            // Check if the latest version is newer than the current version.
            AppcastItem latest = appcast.getLatest();
            if (latest.isNewerThan(updater.getHost())) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        updater.checkerFoundValidUpdate(UpdateChecker.this, appcast);
                    }
                });
            } else {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        updater.checkerDetectedLatestVersion(UpdateChecker.this, appcast);
                    }
                });
            }
        } catch (final Exception e) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    updater.checkerEncounteredError(UpdateChecker.this, e);
                }
            });
        }
    }

    private Appcast parseAppcastXML(InputStream is) throws Exception {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        SAXParser parser = spf.newSAXParser();
        AppcastHandler handler = new AppcastHandler();
        parser.parse(is, handler);
        return handler.getAppcast();
    }

}
