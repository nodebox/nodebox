package nodebox.versioncheck;

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

    public String getIconFile() {
        return "test/mockboxlogo.png";
    }

    public String getAppcastURL() {
        return "http://localhost:" + APPCAST_SERVER_PORT + "/appcast.xml";
    }
}
