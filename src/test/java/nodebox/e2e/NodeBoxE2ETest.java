package nodebox.e2e;

import nodebox.client.Application;
import nodebox.client.NodeBoxDocument;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import javax.swing.SwingUtilities;
import javax.imageio.ImageIO;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class NodeBoxE2ETest {

    private static final String E2E_ENV = "NODEBOX_E2E";
    private static final String E2E_FAIL_ENV = "NODEBOX_E2E_FORCE_FAIL";
    private static final String ARTIFACTS_ENV = "NODEBOX_E2E_ARTIFACTS";
    private static final long DEFAULT_TIMEOUT_MS = 20000;

    @Rule
    public final TestWatcher watcher = new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {
            writeFailure(description, e);
            captureScreenshot(description);
        }
    };

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

    @Test
    public void canFailForPipelineVerification() {
        Assume.assumeTrue("Failure flag is not enabled", "1".equals(System.getenv(E2E_FAIL_ENV)));
        throw new AssertionError("Intentional E2E failure to verify CI artifacts.");
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

    private static File artifactsDir() {
        String dir = System.getenv(ARTIFACTS_ENV);
        if (dir == null || dir.trim().isEmpty()) {
            dir = "build/e2e-artifacts";
        }
        File target = new File(dir);
        if (!target.exists()) {
            target.mkdirs();
        }
        return target;
    }

    private static void captureScreenshot(Description description) {
        try {
            Robot robot = new Robot();
            Rectangle bounds = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            BufferedImage image = robot.createScreenCapture(bounds);
            File out = new File(artifactsDir(), safeName(description) + ".png");
            ImageIO.write(image, "png", out);
        } catch (Exception ignored) {
            // Best-effort for CI artifacts.
        }
    }

    private static void writeFailure(Description description, Throwable error) {
        File out = new File(artifactsDir(), safeName(description) + ".txt");
        try (PrintWriter writer = new PrintWriter(new FileWriter(out))) {
            writer.println("Test: " + description.getDisplayName());
            writer.println("Thread: " + Thread.currentThread().getName());
            writer.println();
            error.printStackTrace(writer);
        } catch (Exception ignored) {
            // Best-effort for CI artifacts.
        }
    }

    private static String safeName(Description description) {
        String raw = description.getClassName() + "-" + description.getMethodName();
        return raw.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
