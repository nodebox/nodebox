package nodebox.versioncheck;

import junit.framework.TestCase;
import org.xml.sax.SAXParseException;

import javax.swing.*;
import java.io.FileNotFoundException;

public class UpdaterTest extends TestCase {
    private MockAppcastServer server;
    private Thread serverThread;

    @Override
    protected void setUp() throws Exception {
        server = new MockAppcastServer(MockHost.APPCAST_SERVER_PORT);
        serverThread = new Thread(server);
        serverThread.start();
    }

    @Override
    protected void tearDown() throws Exception {
        server.stop();
        serverThread.join();
    }

    /**
     * Test the regular update process.
     */
    public void testCheckForUpdates() {
        Updater updater = new Updater(new MockHost());
        TestUpdateDelegate delegate = checkForUpdates(updater);
        assertTrue(delegate.checkPerformed);
        assertNull(delegate.throwable);
        AppcastItem item = delegate.appcast.getLatest();
        assertNotNull(item);
        assertEquals(new Version("2.0"), item.getVersion());
        assertTrue(item.isNewerThan(updater.getHost()));
    }

    /**
     * Test what happens if the appcast file can not be found.
     */
    public void testNotFound() {
        Updater updater = new Updater(new NotFoundHost());
        TestUpdateDelegate delegate = checkForUpdates(updater);
        assertFalse(delegate.checkPerformed);
        assertEquals(FileNotFoundException.class, delegate.throwable.getClass());
    }

    /**
     * Test what happens if the appcast file can not be parsed.
     */
    public void testUnreadableAppcast() {
        Updater updater = new Updater(new UnreadableHost());
        TestUpdateDelegate delegate = checkForUpdates(updater);
        assertFalse(delegate.checkPerformed);
        assertEquals(SAXParseException.class, delegate.throwable.getClass());
    }

    private TestUpdateDelegate checkForUpdates(Updater updater) {
        TestUpdateDelegate delegate = new TestUpdateDelegate();
        updater.setDelegate(delegate);
        updater.checkForUpdates(false);
        try {
            updater.getUpdateChecker().join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // The update checker calls invokeLater on the AWT event dispatch thread.
        // We need to make sure this call finishes before we continue our test.
        // By using invokeAndWait to place an empty action on the thread, we know
        // that the calls will have finished.
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    // Do nothing;
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return delegate;
    }

    private class TestUpdateDelegate extends UpdateDelegate {

        private boolean checkPerformed;
        private Appcast appcast;
        private Throwable throwable;

        @Override
        public boolean checkCompleted(UpdateChecker checker, Appcast appcast) {
            checkPerformed = true;
            return true;
        }

        @Override
        public boolean checkerFoundValidUpdate(UpdateChecker checker, Appcast appcast) {
            this.appcast = appcast;
            return true;
        }

        @Override
        public boolean checkerEncounteredError(UpdateChecker checker, Throwable t) {
            this.throwable = t;
            return true;
        }
    }

    public class NotFoundHost extends MockHost {
        @Override
        public String getAppcastURL() {
            return "http://localhost:" + APPCAST_SERVER_PORT + "/this_file_does_not_exist";
        }
    }


    public class UnreadableHost extends MockHost {
        @Override
        public String getAppcastURL() {
            return "http://localhost:" + APPCAST_SERVER_PORT + "/unreadable_appcast.xml";
        }
    }

}
