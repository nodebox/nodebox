package nodebox.e2e;

import nodebox.client.Application;
import nodebox.client.NodeBoxDocument;
import nodebox.node.Connection;
import nodebox.node.Node;
import nodebox.node.Port;
import nodebox.ui.ExportFormat;
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
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
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
            captureSwingSnapshot(description);
            captureScreenshot(description);
        }
    };

    @BeforeClass
    public static void requireE2E() throws Exception {
        Assume.assumeTrue("E2E tests require NODEBOX_E2E=1", "1".equals(System.getenv(E2E_ENV)));
        Assume.assumeFalse("E2E tests require a graphics environment", GraphicsEnvironment.isHeadless());
        if (Application.getInstance() == null) {
            Application.main(new String[]{});
            waitFor("Application instance", DEFAULT_TIMEOUT_MS, new Supplier<Boolean>() {
                @Override
                public Boolean get() {
                    return Application.getInstance() != null;
                }
            });
        }
        waitFor("Initial document", DEFAULT_TIMEOUT_MS, new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                Application app = Application.getInstance();
                return app != null && app.getDocumentCount() > 0 && app.getCurrentDocument() != null;
            }
        });
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
        NodeBoxDocument current = focusCurrentDocument();
        assertNotNull(current);
        assertTrue(current.isVisible());

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

        final File example = exampleFile();
        openExampleAndWait(example);

        if ("1".equals(System.getenv(E2E_FAIL_ENV))) {
            throw new AssertionError("Intentional E2E failure after UI is visible.");
        }
    }

    @Test
    public void saveAndReloadDocument() throws Exception {
        final NodeBoxDocument doc = focusCurrentDocument();
        assertNotNull(doc);

        final File tempFile = new File("build/e2e-artifacts", "e2e-save.ndbx");
        File parentDir = tempFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                doc.setDocumentFile(tempFile);
                doc.setNeedsResave(false);
                doc.save();
            }
        });

        waitFor("Saved file", DEFAULT_TIMEOUT_MS, new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return tempFile.isFile() && tempFile.length() > 0;
            }
        });

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                Application.getInstance().openDocument(tempFile);
            }
        });

        waitFor("Reloaded document", DEFAULT_TIMEOUT_MS, new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                NodeBoxDocument current = Application.getInstance().getCurrentDocument();
                return current != null && current.getDocumentFile() != null && sameFile(tempFile, current.getDocumentFile());
            }
        });
    }

    @Test
    public void exportSingleFramePng() throws Exception {
        openExampleAndWait(exampleFile());
        final NodeBoxDocument doc = Application.getInstance().getCurrentDocument();
        assertNotNull(doc);

        final File exportDir = new File("build/e2e-artifacts", "exports");
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }
        final String prefix = "e2e-export";
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                doc.exportRange(prefix, exportDir, 1, 1, ExportFormat.PNG);
            }
        });

        final File exportFile = new File(exportDir, prefix + "-00001.png");
        waitFor("PNG export", DEFAULT_TIMEOUT_MS, new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return exportFile.isFile() && exportFile.length() > 0;
            }
        });
    }

    @Test
    public void undoRedoNodeCreation() throws Exception {
        openExampleAndWait(exampleFile());
        final NodeBoxDocument doc = Application.getInstance().getCurrentDocument();
        assertNotNull(doc);

        final AtomicInteger initialCount = new AtomicInteger();
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                initialCount.set(countRootNodes(doc));
                Node prototype = firstPrototypeNode(doc);
                doc.createNode(prototype, new nodebox.graphics.Point(100, 100));
            }
        });

        waitFor("Node created", DEFAULT_TIMEOUT_MS, new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return countRootNodes(doc) == initialCount.get() + 1;
            }
        });

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                doc.undo();
            }
        });

        waitFor("Undo", DEFAULT_TIMEOUT_MS, new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return countRootNodes(doc) == initialCount.get();
            }
        });

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                doc.redo();
            }
        });

        waitFor("Redo", DEFAULT_TIMEOUT_MS, new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return countRootNodes(doc) == initialCount.get() + 1;
            }
        });
    }

    @Test
    public void copyPasteSelectionIncreasesNodeCount() throws Exception {
        final NodeBoxDocument doc = focusCurrentDocument();
        assertNotNull(doc);
        ensureAtLeastOneNode(doc);

        final int beforeCount = countRootNodes(doc);
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                Node node = firstChildNode(doc);
                if (node == null) {
                    return;
                }
                doc.setActiveNode(node.getName());
                doc.copy();
                doc.paste();
            }
        });

        waitFor("Paste node", DEFAULT_TIMEOUT_MS, new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return countRootNodes(doc) >= beforeCount + 1;
            }
        });

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                doc.undo();
            }
        });

        waitFor("Undo paste", DEFAULT_TIMEOUT_MS, new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return countRootNodes(doc) == beforeCount;
            }
        });
    }

    @Test
    public void deleteSelectionRemovesNode() throws Exception {
        final NodeBoxDocument doc = focusCurrentDocument();
        assertNotNull(doc);

        final int beforeCount = countRootNodes(doc);
        final Node created = createRectNode(doc, new nodebox.graphics.Point(140, 140));

        waitFor("Node created", DEFAULT_TIMEOUT_MS, new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return countRootNodes(doc) == beforeCount + 1;
            }
        });

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                if (created != null) {
                    doc.setActiveNode(created.getName());
                }
                doc.deleteSelection();
            }
        });

        waitFor("Delete selection", DEFAULT_TIMEOUT_MS, new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return countRootNodes(doc) == beforeCount;
            }
        });
    }

    @Test
    public void setPortValueUpdatesNode() throws Exception {
        final NodeBoxDocument doc = focusCurrentDocument();
        assertNotNull(doc);

        final Node created = createRectNode(doc, new nodebox.graphics.Point(180, 180));
        assertNotNull(created);
        final String nodePath = Node.path(doc.getActiveNetworkPath(), created.getName());
        final double expected = 200.0;

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                doc.setActiveNode(created.getName());
                doc.setValue(nodePath, "width", expected);
            }
        });

        waitFor("Port value updated", DEFAULT_TIMEOUT_MS, new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                Object value = readPortValue(doc, nodePath, "width");
                return numberEquals(value, expected);
            }
        });
    }

    @Test
    public void renameNodeUpdatesActiveNetwork() throws Exception {
        final NodeBoxDocument doc = focusCurrentDocument();
        assertNotNull(doc);

        final Node created = createRectNode(doc, new nodebox.graphics.Point(220, 220));
        assertNotNull(created);
        final String newName = created.getName() + "_renamed";

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                doc.setNodeName(created, newName);
            }
        });

        waitFor("Node renamed", DEFAULT_TIMEOUT_MS, new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                Node active = doc.getActiveNetwork();
                return active != null && active.hasChild(newName);
            }
        });
    }

    @Test
    public void takeDocumentScreenshot() throws Exception {
        final NodeBoxDocument doc = focusCurrentDocument();
        assertNotNull(doc);
        final File screenshot = new File(artifactsDir(), "nodebox-document.png");

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                doc.takeScreenshot(screenshot);
            }
        });

        waitFor("Document screenshot", DEFAULT_TIMEOUT_MS, new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return screenshot.isFile() && screenshot.length() > 0;
            }
        });
    }

    @Test
    public void frameAdvancesByOne() throws Exception {
        final NodeBoxDocument doc = focusCurrentDocument();
        assertNotNull(doc);
        final double[] frame = new double[1];

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                doc.setFrame(1);
                doc.nextFrame();
                frame[0] = doc.getFrame();
            }
        });

        assertTrue("Frame should advance by one.", Math.abs(frame[0] - 2.0) < 0.001);
    }

    @Test
    public void connectNodesAddsConnection() throws Exception {
        openExampleAndWait(exampleFile());
        final NodeBoxDocument doc = Application.getInstance().getCurrentDocument();
        assertNotNull(doc);

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                doc.connect("colorize1", "combine1", "list4");
            }
        });

        waitFor("Connection added", DEFAULT_TIMEOUT_MS, new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return hasConnection(doc.getActiveNetwork(), "colorize1", "combine1", "list4");
            }
        });
    }

    @Test
    public void switchRenderedNodeUpdatesNetwork() throws Exception {
        openExampleAndWait(exampleFile());
        final NodeBoxDocument doc = Application.getInstance().getCurrentDocument();
        assertNotNull(doc);

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                Node target = doc.getActiveNetwork().getChild("polygon1");
                if (target != null) {
                    doc.setRenderedNode(target);
                }
            }
        });

        waitFor("Rendered node switched", DEFAULT_TIMEOUT_MS, new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return "polygon1".equals(doc.getActiveNetwork().getRenderedChildName());
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

    private static NodeBoxDocument focusCurrentDocument() throws Exception {
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
        return current[0];
    }

    private static File exampleFile() {
        return new File("examples/01 Basics/01 Shape/01 Primitives/01 Primitives.ndbx");
    }

    private static void openExampleAndWait(final File example) throws Exception {
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

    private static int countRootNodes(NodeBoxDocument doc) {
        Collection<Node> nodes = doc.getNodeLibrary().getRoot().getChildren();
        return nodes == null ? 0 : nodes.size();
    }

    private static Node firstChildNode(NodeBoxDocument doc) {
        Collection<Node> nodes = doc.getNodeLibrary().getRoot().getChildren();
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }
        return nodes.iterator().next();
    }

    private static void ensureAtLeastOneNode(NodeBoxDocument doc) throws Exception {
        if (countRootNodes(doc) > 0) {
            return;
        }
        createRectNode(doc, new nodebox.graphics.Point(100, 100));
        waitFor("Initial node", DEFAULT_TIMEOUT_MS, new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return countRootNodes(doc) > 0;
            }
        });
    }

    private static Node createRectNode(final NodeBoxDocument doc, final nodebox.graphics.Point point) throws Exception {
        final Node[] created = new Node[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                Node prototype = rectPrototype(doc);
                doc.createNode(prototype, point);
                created[0] = doc.getActiveNode();
            }
        });
        return created[0];
    }

    private static Node rectPrototype(NodeBoxDocument doc) {
        try {
            return doc.getNodeRepository().getNode("corevector.rect");
        } catch (RuntimeException ignored) {
            return firstPrototypeNode(doc);
        }
    }

    private static boolean hasConnection(Node network, String outputNode, String inputNode, String inputPort) {
        if (network == null) {
            return false;
        }
        for (Connection connection : network.getConnections()) {
            if (outputNode.equals(connection.getOutputNode())
                    && inputNode.equals(connection.getInputNode())
                    && inputPort.equals(connection.getInputPort())) {
                return true;
            }
        }
        return false;
    }

    private static Node firstPrototypeNode(NodeBoxDocument doc) {
        List<Node> nodes = doc.getNodeRepository().getNodes();
        if (nodes.isEmpty()) {
            throw new IllegalStateException("No prototype nodes available.");
        }
        return nodes.get(0);
    }

    private static Object readPortValue(final NodeBoxDocument doc, final String nodePath, final String portName) {
        final Object[] value = new Object[1];
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    Node node = doc.getNodeLibrary().getNodeForPath(nodePath);
                    if (node == null) return;
                    Port port = node.getInput(portName);
                    if (port == null) return;
                    value[0] = port.getValue();
                }
            });
        } catch (Exception ignored) {
        }
        return value[0];
    }

    private static boolean numberEquals(Object value, double expected) {
        if (!(value instanceof Number)) {
            return false;
        }
        return Math.abs(((Number) value).doubleValue() - expected) < 0.001;
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
            File out = new File(artifactsDir(), safeName(description) + "-screen.png");
            ImageIO.write(image, "png", out);
        } catch (Exception ignored) {
            // Best-effort for CI artifacts.
        }
    }

    private static void captureSwingSnapshot(Description description) {
        try {
            final Application app = Application.getInstance();
            if (app == null) return;
            final NodeBoxDocument doc = app.getCurrentDocument();
            if (doc == null) return;
            final BufferedImage[] imageHolder = new BufferedImage[1];
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    int width = Math.max(doc.getWidth(), doc.getPreferredSize().width);
                    int height = Math.max(doc.getHeight(), doc.getPreferredSize().height);
                    if (width <= 0) width = 1280;
                    if (height <= 0) height = 720;
                    java.awt.Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
                    if (screen.width > 0) {
                        width = Math.min(width, screen.width);
                    }
                    if (screen.height > 0) {
                        height = Math.min(height, screen.height);
                    }
                    doc.setSize(width, height);
                    doc.validate();
                    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g = image.createGraphics();
                    doc.printAll(g);
                    g.dispose();
                    imageHolder[0] = image;
                }
            });
            if (imageHolder[0] != null) {
                File out = new File(artifactsDir(), safeName(description) + "-swing.png");
                ImageIO.write(imageHolder[0], "png", out);
            }
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
