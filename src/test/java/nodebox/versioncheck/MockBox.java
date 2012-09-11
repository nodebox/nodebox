package nodebox.versioncheck;

import javax.swing.*;
import java.awt.*;

/**
 * Demo host application.
 * <p/>
 * Hosts its own server.
 */
public class MockBox extends MockHost {

    private Updater updater;

    public MockBox() throws HeadlessException {
        updater = new Updater(this);
        MockAppcastServer server = new MockAppcastServer(MockHost.APPCAST_SERVER_PORT);
        Thread serverThread = new Thread(server);
        serverThread.start();
    }

    public void start() {
        JFrame mainFrame = new JFrame("MockBox");
        mainFrame.setSize(700, 500);
        mainFrame.setLocationByPlatform(true);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setVisible(true);
        updater.applicationDidFinishLaunching();
    }

    public static void main(String[] args) {
        MockBox mb = new MockBox();
        mb.start();
    }

}
