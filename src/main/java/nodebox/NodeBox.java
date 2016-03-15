package nodebox;

import nodebox.client.FileUtils;
import nodebox.client.visualizer.GrobVisualizer;
import nodebox.function.FunctionRepository;
import nodebox.graphics.Rect;
import nodebox.node.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NodeBox {

    private NodeBox() {}

    public static NodeRepository getSystemRepository(String systemLibraryDir) {
        List<NodeLibrary> libraries = new ArrayList<>();
        libraries.add(NodeLibrary.loadSystemLibrary("math"));
        libraries.add(NodeLibrary.loadSystemLibrary("string"));
        libraries.add(NodeLibrary.loadSystemLibrary("color"));
        libraries.add(NodeLibrary.loadSystemLibrary("list"));
        libraries.add(NodeLibrary.loadSystemLibrary("data"));
        libraries.add(NodeLibrary.loadSystemLibrary("corevector"));
        libraries.add(NodeLibrary.loadSystemLibrary("network"));
        return NodeRepository.of(libraries.toArray(new NodeLibrary[]{}));
    }

    public static void printUsage() {
        System.out.println("Usage: java -jar nodebox.jar [options] <inputFile.ndbx>");
        System.out.println("Options:");
        System.out.println(" -o FILE Specify the output file. Only PNG is supported for now. (Default: inputFile.png)");
        System.out.println(" -l DIR Location of the NodeBox system libraries directory. (Default: current directory)");
    }

    public static void main(String[] args) {
        String inputFile = null;
        String outputFile = null;
        String systemLibraryDir = null;
        File inFile = null;
        File outFile;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-o")) {
                outputFile = args[i + 1];
                i += 1;
            } else if (arg.equals("-l")) {
                systemLibraryDir = args[i + 1];
                i += 1;
            } else {
                inputFile = args[i];
            }
        }
        if (inputFile == null) {
            printUsage();
            System.exit(-1);
        } else {
            inFile = new File(inputFile);
        }
        if (outputFile == null) {
            outFile = new File(FileUtils.getBaseName(inFile.getAbsolutePath()) + ".png");
        } else {
            outFile = new File(outputFile);
        }
        if (systemLibraryDir == null) {
            systemLibraryDir = "libraries";
        }
        NodeRepository systemRepository = getSystemRepository(systemLibraryDir);
        NodeLibrary library;
        try {
            library = NodeLibrary.load(inFile, systemRepository);
        } catch (OutdatedLibraryException e) {
            UpgradeResult result = NodeLibraryUpgrades.upgrade(inFile);
            // The file is used here as the base name for finding relative libraries.
            library = result.getLibrary(inFile, systemRepository);
        }
        FunctionRepository functionRepository = FunctionRepository.combine(systemRepository.getFunctionRepository(), library.getFunctionRepository());
        library.getRoot();
        NodeContext ctx = new NodeContext(library, functionRepository);
        List<?> result = ctx.renderNode("/");
        Rect bounds = library.getBounds();
        BufferedImage img = new BufferedImage(
                (int) Math.ceil(bounds.getWidth()),
                (int) Math.ceil(bounds.getHeight()),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.translate(-bounds.getX(), -bounds.getY());
        GrobVisualizer.INSTANCE.draw(g, result);
        img.flush();
        try {
            ImageIO.write(img, "png", outFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
