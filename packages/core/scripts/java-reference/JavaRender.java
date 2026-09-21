import nodebox.graphics.*;
import nodebox.node.*;
import java.io.File;
import java.util.*;

/** Render top-level children of an .ndbx with the original Java engine and print every node's results. */
public class JavaRender {
    public static void main(String[] args) throws Exception {
        File file = new File(args[0]);
        String pattern = args.length > 1 ? args[1] : "_$";
        boolean verbose = System.getenv("VERBOSE") != null;
        List<NodeLibrary> libraries = new ArrayList<>();
        for (String lib : new String[] {"math", "string", "color", "list", "data", "network", "device", "corevector"})
            libraries.add(NodeLibrary.loadSystemLibrary(lib));
        NodeRepository repository = NodeRepository.of(libraries.toArray(new NodeLibrary[0]));
        NodeLibrary library = NodeLibrary.load(file, repository);
        nodebox.function.FunctionRepository functions = nodebox.function.FunctionRepository.combine(repository.getFunctionRepository(), library.getFunctionRepository());
        for (Node child : library.getRoot().getChildren()) {
            if (!child.getName().matches(".*" + pattern)) continue;
            String name = child.getName();
            Map<String, Object> data = new HashMap<>();
            data.put("frame", 12.0);
            NodeContext context = new NodeContext(library, functions, data);
            try {
                List<?> result = context.renderNode("/" + name);
                System.out.println("OK    " + name + ": " + result.size() + " results " + summarize(result));
            } catch (Exception e) {
                Throwable t = e;
                while (t.getCause() != null) t = t.getCause();
                System.out.println("FAIL  " + name + ": " + e.getMessage() + " <- " + t);
                if (System.getenv("STACK") != null) t.printStackTrace(System.out);
            }
            if (verbose) {
                TreeMap<String, List<?>> sorted = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                sorted.putAll(results(context));
                for (Map.Entry<String, List<?>> e : sorted.entrySet())
                    System.out.println("  " + e.getKey() + " -> " + e.getValue().size() + ": " + summarize(e.getValue()));
            }
        }
    }

    @SuppressWarnings("unchecked")
    static Map<String, List<?>> results(NodeContext context) throws Exception {
        java.lang.reflect.Field f = NodeContext.class.getDeclaredField("renderResults");
        f.setAccessible(true);
        return (Map<String, List<?>>) f.get(context);
    }

    static String summarize(List<?> values) {
        StringBuilder b = new StringBuilder("[");
        int n = 0;
        for (Object v : values) {
            if (n++ > 0) b.append(", ");
            if (n > 5) { b.append("…").append(values.size()); break; }
            b.append(format(v));
        }
        return b.append("]").toString();
    }

    static String format(Object v) {
        if (v instanceof Path) return formatPoints("Path", ((Path) v).getPoints());
        if (v instanceof Geometry) return formatPoints("Geometry", ((Geometry) v).getPoints());
        if (v instanceof Point) { Point p = (Point) v; return "?LCD".charAt(p.type) + String.format(Locale.US, "%.2f,%.2f", p.x, p.y); }
        if (v instanceof Color) return "<Color>";
        if (v instanceof Double) return Double.toString((Double) v);
        if (v instanceof String) return "\"" + v + "\"";
        return String.valueOf(v);
    }

    static String formatPoints(String name, List<Point> points) {
        if (points.size() > 24) return "<" + name + " " + points.size() + " pts>";
        StringBuilder b = new StringBuilder("<" + name);
        for (Point p : points) b.append(" ").append("?LCD".charAt(p.type)).append(String.format(Locale.US, "%.2f,%.2f", p.x, p.y));
        return b.append(">").toString();
    }
}
