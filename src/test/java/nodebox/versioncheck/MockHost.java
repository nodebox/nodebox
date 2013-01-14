package nodebox.versioncheck;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Mock implementation for test host.
 */
public class MockHost implements Host {

    public static final int APPCAST_SERVER_PORT = 41555;

    public String getName() {
        return "MockBox";
    }

    public Version getVersion() {
        return new Version("1.0");
    }

    public URL getIconFile() {
        try {
            return new URL("file:src/test/files/mockboxlogo.png");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public String getAppcastURL() {
        return "http://localhost:" + APPCAST_SERVER_PORT + "/appcast.xml";
    }

}
