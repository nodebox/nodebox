package nodebox;

import com.google.common.collect.ImmutableMap;
import nodebox.function.FunctionRepository;
import nodebox.graphics.Rect;
import nodebox.graphics.SVGRenderer;
import nodebox.node.*;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.*;

/**
 * Headless batch renderer for golden master testing: loads .ndbx files and exports frame 1 to SVG.
 * <p>
 * Each document is rendered without a cache, which is the SVG that is written. It is then rendered twice
 * through one RenderCache, and the second, cached render must give the same SVG; otherwise the file is
 * reported as CACHE-DIFF.
 * <p>
 * Usage:
 * java -cp ... nodebox.BatchRenderer input.ndbx output.svg
 * java -cp ... nodebox.BatchRenderer --dir inputDir outputDir   (all .ndbx files, same relative paths)
 * java -cp ... nodebox.BatchRenderer --all outputDir            (the examples/ folder)
 * <p>
 * It needs a display (NodeBox's Python setup touches AWT); use xvfb-run on Linux.
 * <p>
 * Each document gets 60 seconds (-Dbatch.timeout=seconds to change it).
 */
public class BatchRenderer {

    private static final long TIMEOUT_SECONDS = Long.getLong("batch.timeout", 60);
    private static NodeRepository systemRepository;
    private static int ok, failed, timedOut, cacheDiffs;

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: BatchRenderer <input.ndbx> <output.svg>");
            System.out.println("       BatchRenderer --dir <inputDir> <outputDir>");
            System.out.println("       BatchRenderer --all <outputDir>");
            System.exit(-1);
        }
        systemRepository = NodeBox.getSystemRepository("libraries");
        if (args[0].equals("--all")) {
            renderDirectory(new File("examples"), new File("examples"), new File(args[1]));
        } else if (args[0].equals("--dir")) {
            renderDirectory(new File(args[1]), new File(args[1]), new File(args[2]));
        } else {
            renderOne(new File(args[0]), new File(args[1]));
        }
        System.out.println("DONE ok=" + ok + " failed=" + failed + " timeout=" + timedOut + " cache-diff=" + cacheDiffs);
        System.exit(0);
    }

    private static void renderDirectory(File dir, File baseDir, File outputDir) {
        File[] files = dir.listFiles();
        if (files == null) return;
        java.util.Arrays.sort(files);
        for (File f : files) {
            if (f.isDirectory()) {
                renderDirectory(f, baseDir, outputDir);
            } else if (f.getName().endsWith(".ndbx")) {
                String relativePath = baseDir.toPath().relativize(f.toPath()).toString();
                renderOne(f, new File(outputDir, relativePath.replaceAll("\\.ndbx$", ".svg")));
            }
        }
    }

    private static void renderOne(final File inFile, final File outFile) {
        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "render " + inFile.getName());
            t.setDaemon(true);
            return t;
        });
        Future<String> future = executor.submit(() -> render(inFile, outFile));
        try {
            String status = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (status.equals("CACHE-DIFF")) cacheDiffs++;
            ok++;
            System.out.println(status + " " + inFile.getPath());
        } catch (TimeoutException e) {
            future.cancel(true);
            timedOut++;
            System.out.println("TIMEOUT " + inFile.getPath());
        } catch (ExecutionException e) {
            failed++;
            Throwable cause = e.getCause();
            System.out.println("FAIL " + inFile.getPath() + ": " + cause.getClass().getSimpleName() + ": " + cause.getMessage());
            if (Boolean.getBoolean("batch.trace")) cause.printStackTrace(System.out);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdownNow();
        }
    }

    private static String render(File inFile, File outFile) throws Exception {
        NodeLibrary library = loadLibrary(inFile);
        FunctionRepository functionRepository = FunctionRepository.combine(
                systemRepository.getFunctionRepository(),
                library.getFunctionRepository());
        Rect bounds = library.getBounds();
        Rectangle2D rect = new Rectangle2D.Double(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());

        List<?> uncached = new NodeContext(library, functionRepository).renderNode("/");
        outFile.getParentFile().mkdirs();
        SVGRenderer.renderToFile(uncached, rect, outFile);

        RenderCache cache = new RenderCache();
        cache.useFunctions(functionRepository);
        ImmutableMap<String, Object> data = ImmutableMap.of("frame", 1.0);
        new NodeContext(library, functionRepository, data, ImmutableMap.<String, Object>of(), cache).renderNode("/");
        List<?> cached = new NodeContext(library, functionRepository, data, ImmutableMap.<String, Object>of(), cache).renderNode("/");
        File cachedFile = new File(outFile.getPath() + ".cached");
        SVGRenderer.renderToFile(cached, rect, cachedFile);
        boolean same = new String(Files.readAllBytes(outFile.toPath()), StandardCharsets.UTF_8)
                .equals(new String(Files.readAllBytes(cachedFile.toPath()), StandardCharsets.UTF_8));
        if (same) cachedFile.delete();
        return same ? "OK" : "CACHE-DIFF";
    }

    private static NodeLibrary loadLibrary(File inFile) {
        try {
            return NodeLibrary.load(inFile, systemRepository);
        } catch (OutdatedLibraryException e) {
            UpgradeResult result = NodeLibraryUpgrades.upgrade(inFile);
            return result.getLibrary(inFile, systemRepository);
        }
    }
}
