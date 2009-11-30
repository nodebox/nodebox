package nodebox.versioncheck;

import junit.framework.TestCase;

public class UpdaterTest extends TestCase {


    private Host host;
    private Updater updater;
    private MockAppcastServer server;
    private Thread serverThread;

    @Override
    protected void setUp() throws Exception {
        host = new MockHost();
        updater = new Updater(host);
        server = new MockAppcastServer(MockHost.APPCAST_SERVER_PORT);
        serverThread = new Thread(server);
        serverThread.start();
    }

    public void testCheckForUpdates() {
        TestUpdateDelegate delegate = new TestUpdateDelegate();
        updater.setDelegate(delegate);
        updater.checkForUpdates(false);
        try {
            updater.getUpdateChecker().join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        assertTrue(delegate.checkPerformed);
        assertNull(delegate.throwable);
        AppcastItem item = delegate.appcast.getLatest();
        assertNotNull(item);
        assertEquals(new Version("2.0"), item.getVersion());
        assertTrue(item.isNewerThan(host));
    }

    private class TestUpdateDelegate extends UpdateDelegate {

        private boolean checkPerformed;
        private Appcast appcast;
        private Throwable throwable;

        @Override
        public boolean checkPerformed(UpdateChecker checker, Appcast appcast) {
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
}
