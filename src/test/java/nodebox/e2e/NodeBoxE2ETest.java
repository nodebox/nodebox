package nodebox.e2e;

import nodebox.client.Application;
import nodebox.client.NodeBoxDocument;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class NodeBoxE2ETest {

    private static final String E2E_ENV = "NODEBOX_E2E";
    private static final long DEFAULT_TIMEOUT_MS = 20000;

    @BeforeClass
    public static void requireE2E() {
        Assume.assumeTrue("E2E tests require NODEBOX_E2E=1", "1".equals(System.getenv(E2E_ENV)));
        Assume.assumeFalse("E2E tests require a graphics environment", GraphicsEnvironment.isHeadless());
    }

    @AfterClass
    public static void closeDocuments() throws Exception {
        Application app = Application.getInstance();
        if (app == null) return;
        List<NodeBoxDocument> docs = new ArrayList<NodeBoxDocument>(app.getDocuments());
        for (final NodeBoxDocument doc : docs) {
            if (doc == null) continue;
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    doc.dispose();
                }
            });
        }
    }

    @Test
    public void launchesAndOpensExample() throws Exception {
        Application.main(new String[]{});

        waitFor("Application instance", DEFAULT_TIMEOUT_MS, new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return Application.getInstance() != null;
            }
        });

        waitFor("Initial document", DEFAULT_TIMEOUT_MS, new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                Application app = Application.getInstance();
                return app != null && app.getDocumentCount() > 0 && app.getCurrentDocument() != null;
            }
        });

        final NodeBoxDocument[] current = new NodeBoxDocument[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                current[0] = Application.getInstance().getCurrentDocument();
                if (current[0] != null) {
                    current[0].toFront();
                    current[0].requestFocus();
                }
            }
        });

        assertNotNull(current[0]);
        assertTrue(current[0].isVisible());

        int initialCount = Application.getInstance().getDocumentCount();

        Robot robot = new Robot();
        robot.setAutoWaitForIdle(true);
        robot.delay(500);

        int menuKey = menuShortcutKey();
        robot.keyPress(menuKey);
        robot.keyPress(KeyEvent.VK_N);
        robot.keyRelease(KeyEvent.VK_N);
        robot.keyRelease(menuKey);

        waitFor("New document", DEFAULT_TIMEOUT_MS, new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return Application.getInstance().getDocumentCount() >= initialCount + 1;
            }
        });

        final File example = new File("examples/01 Basics/01 Shape/01 Primitives/01 Primitives.ndbx");
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                Application.getInstance().openExample(example);
            }
        });

        waitFor("Example open", DEFAULT_TIMEOUT_MS, new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                NodeBoxDocument doc = Application.getInstance().getCurrentDocument();
                return doc != null && doc.getDocumentFile() != null && sameFile(example, doc.getDocumentFile());
            }
        });
    }

    private static int menuShortcutKey() {
        int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        if ((mask & InputEvent.META_DOWN_MASK) != 0) {
            return KeyEvent.VK_META;
        }
        return KeyEvent.VK_CONTROL;
    }

    private static void waitFor(String label, long timeoutMs, Supplier<Boolean> condition) throws Exception {
        long start = System.currentTimeMillis();
        while ((System.currentTimeMillis() - start) < timeoutMs) {
            if (condition.get()) return;
            Thread.sleep(100);
        }
        throw new AssertionError("Timed out waiting for: " + label);
    }

    private static boolean sameFile(File left, File right) {
        try {
            return left.getCanonicalFile().equals(right.getCanonicalFile());
        } catch (Exception e) {
            return false;
        }
    }
}
