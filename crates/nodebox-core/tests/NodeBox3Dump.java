import nodebox.NodeBox;
import nodebox.function.FunctionRepository;
import nodebox.graphics.*;
import nodebox.node.*;

import java.io.File;
import java.util.List;

/**
 * Prints what NodeBox 3 computes for nodes in the example documents, for
 * nodebox3_examples.txt. Each argument is "<path under examples/>::/<node>"
 * (a node inside a subnetwork is "/<network>/<node>"). The node is rendered
 * in its document, as if it were the rendered node.
 *
 * Build NodeBox 3 from the master branch, then run from the root of that
 * checkout (it needs libraries/ and examples/), with a display (xvfb-run on
 * Linux; Jython initialization fails headless):
 *
 *   mvn dependency:copy-dependencies -DoutputDirectory=deps
 *   javac -d classes -cp "deps/*" $(find src/main/java -name '*.java')
 *   cp -r src/main/resources/* classes/
 *   javac -d dump -cp "classes:deps/*" NodeBox3Dump.java
 *   tr '\n' '\0' < nodebox3_examples.cases | \
 *     xargs -0 xvfb-run -a java -cp "dump:classes:deps/*" NodeBox3Dump > nodebox3_examples.txt
 */
public class NodeBox3Dump {
    public static void main(String[] args) {
        NodeRepository system = NodeBox.getSystemRepository("libraries");
        for (String arg : args) {
            String file = arg.substring(0, arg.indexOf("::"));
            String nodePath = arg.substring(arg.indexOf("::") + 2);
            System.out.println("# " + arg);
            try {
                File f = new File("examples/" + file);
                NodeLibrary library;
                try {
                    library = NodeLibrary.load(f, system);
                } catch (OutdatedLibraryException e) {
                    library = NodeLibraryUpgrades.upgrade(f).getLibrary(f, system);
                }
                FunctionRepository functions = FunctionRepository.combine(
                        system.getFunctionRepository(), library.getFunctionRepository());
                library = library.withRoot(renderedAt(library.getRoot(), nodePath.substring(1).split("/"), 0));
                for (Object o : new NodeContext(library, functions).renderNode("/")) {
                    dump(o);
                }
            } catch (Exception e) {
                Throwable cause = e;
                while (cause.getCause() != null) cause = cause.getCause();
                System.out.println("ERROR " + cause);
            }
        }
    }

    /** Makes each network along the path render the next node on it. */
    private static Node renderedAt(Node network, String[] names, int i) {
        Node n = network.withRenderedChildName(names[i]);
        if (i + 1 < names.length) {
            n = n.withChildReplaced(names[i], renderedAt(network.getChild(names[i]), names, i + 1));
        }
        return n;
    }

    /** One line per value; a path is a "path" line and one line per contour. */
    private static void dump(Object o) {
        if (o instanceof Geometry) {
            for (Path p : ((Geometry) o).getPaths()) dump(p);
        } else if (o instanceof Path) {
            System.out.println("path");
            for (Contour c : ((Path) o).getContours()) {
                StringBuilder sb = new StringBuilder(c.isClosed() ? "contour closed" : "contour open");
                for (Point pt : c.getPoints()) {
                    sb.append(' ').append(pt.getType()).append(':').append(pt.getX()).append(',').append(pt.getY());
                }
                System.out.println(sb);
            }
        } else if (o instanceof List) {
            for (Object x : (List<?>) o) dump(x);
        } else if (o instanceof Double || o instanceof Float) {
            System.out.println("float " + o);
        } else if (o instanceof Number) {
            System.out.println("int " + o);
        } else if (o instanceof String) {
            System.out.println("string " + o);
        } else if (o instanceof Point) {
            System.out.println("point " + ((Point) o).getX() + "," + ((Point) o).getY());
        } else {
            System.out.println("value " + o.getClass().getSimpleName() + " " + o);
        }
    }
}
