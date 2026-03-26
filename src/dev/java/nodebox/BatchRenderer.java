package nodebox;

import nodebox.function.FunctionRepository;
import nodebox.graphics.Rect;
import nodebox.graphics.SVGRenderer;
import nodebox.node.*;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.List;

/**
 * Headless batch renderer: loads .ndbx files and exports to SVG.
 * Used for golden master testing (Java vs TypeScript comparison).
 *
 * Registers Inter font and remaps Verdana → Inter so text examples
 * render with a freely redistributable font.
 *
 * Usage: java -cp ... nodebox.BatchRenderer <input.ndbx> <output.svg>
 *        java -cp ... nodebox.BatchRenderer --all <outputDir>
 */
public class BatchRenderer {

    private static String interFontFamily = null;

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java -cp ... nodebox.BatchRenderer <input.ndbx> <output.svg>");
            System.out.println("       java -cp ... nodebox.BatchRenderer --all <outputDir>");
            System.exit(-1);
        }

        // Register Inter font for text rendering
        registerInterFont();

        if (args[0].equals("--all")) {
            renderAll(new File(args[1]));
        } else {
            renderOne(new File(args[0]), new File(args[1]));
        }
    }

    private static void registerInterFont() {
        File fontFile = new File("packages/nodebox-core/fonts/Inter.ttf");
        if (!fontFile.exists()) {
            System.err.println("WARNING: Inter.ttf not found at " + fontFile.getAbsolutePath());
            System.err.println("Text examples will use system Verdana instead.");
            return;
        }
        try {
            Font inter = Font.createFont(Font.TRUETYPE_FONT, fontFile);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(inter);
            interFontFamily = inter.getFamily();
            System.out.println("Registered font: " + interFontFamily + " (" + inter.getName() + ")");
        } catch (Exception e) {
            System.err.println("WARNING: Could not register Inter font: " + e.getMessage());
        }
    }

    private static NodeRepository loadSystemRepository() {
        return NodeBox.getSystemRepository("libraries");
    }

    private static void renderOne(File inFile, File outFile) {
        NodeRepository systemRepository = loadSystemRepository();
        try {
            NodeLibrary library = loadLibrary(inFile, systemRepository);

            // Remap all font references to Inter for reproducible golden masters
            if (interFontFamily != null) {
                library = remapAllFonts(library, interFontFamily);
            }

            FunctionRepository functionRepository = FunctionRepository.combine(
                    systemRepository.getFunctionRepository(),
                    library.getFunctionRepository()
            );
            NodeContext ctx = new NodeContext(library, functionRepository);
            List<?> result = ctx.renderNode("/");

            Rect bounds = library.getBounds();
            Rectangle2D rect = new Rectangle2D.Double(
                    bounds.getX(), bounds.getY(),
                    bounds.getWidth(), bounds.getHeight()
            );

            outFile.getParentFile().mkdirs();
            SVGRenderer.renderToFile(result, rect, outFile);
            System.out.println("OK " + inFile.getName());
        } catch (Exception e) {
            System.err.println("FAIL " + inFile.getName() + ": " + e.getMessage());
        }
    }

    private static NodeLibrary loadLibrary(File inFile, NodeRepository systemRepository) {
        try {
            return NodeLibrary.load(inFile, systemRepository);
        } catch (OutdatedLibraryException e) {
            UpgradeResult result = NodeLibraryUpgrades.upgrade(inFile);
            return result.getLibrary(inFile, systemRepository);
        }
    }

    /**
     * Walk the node tree and replace ALL font name values in font_name ports.
     * This ensures golden masters use a single bundled font (Inter) regardless
     * of what fonts the examples originally specified.
     */
    private static NodeLibrary remapAllFonts(NodeLibrary library, String to) {
        Node root = remapAllFontsInNode(library.getRoot(), to);
        return library.withRoot(root);
    }

    private static Node remapAllFontsInNode(Node node, String to) {
        Node result = node;
        for (nodebox.node.Port port : node.getInputs()) {
            if (port.getName().equals("font_name") && !port.stringValue().isEmpty()) {
                result = result.withInputValue(port.getName(), to);
            }
        }
        for (Node child : node.getChildren()) {
            Node remapped = remapAllFontsInNode(child, to);
            if (remapped != child) {
                result = result.withChildReplaced(child.getName(), remapped);
            }
        }
        return result;
    }

    private static void renderAll(File outputDir) {
        File examplesDir = new File("examples");
        if (!examplesDir.exists()) {
            System.err.println("Cannot find examples/ directory. Run from the project root.");
            System.exit(-1);
        }
        outputDir.mkdirs();
        renderDirectory(examplesDir, examplesDir, outputDir);
    }

    private static void renderDirectory(File dir, File baseDir, File outputDir) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                renderDirectory(f, baseDir, outputDir);
            } else if (f.getName().endsWith(".ndbx")) {
                String relativePath = baseDir.toPath().relativize(f.toPath()).toString();
                String svgName = relativePath.replaceAll("\\.ndbx$", ".svg");
                File outFile = new File(outputDir, svgName);
                renderOne(f, outFile);
            }
        }
    }
}
