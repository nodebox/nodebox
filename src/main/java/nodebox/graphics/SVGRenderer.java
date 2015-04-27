package nodebox.graphics;

import nodebox.util.FileUtils;
import org.python.google.common.collect.ImmutableMap;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

public class SVGRenderer {

    public static String XML_DECLARATION = "<?xml version=\"1.0\"?>\n";

    public static String smartFloat(double v) {
        if ((long) v == v) {
            return String.valueOf((long) v);
        } else {
            return String.format(Locale.US, "%.2f", v);
        }
    }

    public static void appendFloat(StringBuilder sb, double v) {
        sb.append(smartFloat(v));
    }

    public static String renderPathData(Path path) {
        StringBuilder sb = new StringBuilder();
        for (Contour c : path.getContours()) {
            List<Point> points = c.getPoints();
            for (int i = 0; i < points.size(); i += 1) {
                Point pt = points.get(i);
                if (pt.getType() == Point.LINE_TO) {
                    if (i == 0) {
                        sb.append('M');
                        appendFloat(sb, pt.x);
                        sb.append(',');
                        appendFloat(sb, pt.y);
                    } else {
                        sb.append('L');
                        appendFloat(sb, pt.x);
                        sb.append(',');
                        appendFloat(sb, pt.y);
                    }
                } else if (pt.getType() == Point.CURVE_DATA) {
                    // We expect three points.
                    sb.append('C');
                    appendFloat(sb, pt.x);
                    sb.append(',');
                    appendFloat(sb, pt.y);

                    pt = points.get(++i);
                    checkState(pt.getType() == Point.CURVE_DATA);
                    sb.append(' ');
                    appendFloat(sb, pt.x);
                    sb.append(',');
                    appendFloat(sb, pt.y);

                    pt = points.get(++i);
                    checkState(pt.getType() == Point.CURVE_TO);
                    sb.append(' ');
                    appendFloat(sb, pt.x);
                    sb.append(',');
                    appendFloat(sb, pt.y);
                }
            }
            if (c.isClosed()) {
                sb.append('Z');
            }
        }
        return sb.toString();
    }

    public static Element renderPath(Path path) {
        String d = renderPathData(path);

        HashMap<String, String> attrs = new HashMap<String, String>();
        attrs.put("d", d);
        if (path.getFill() != null) {
            if (!path.getFill().equals(Color.BLACK)) {
                attrs.put("fill", path.getFill().toCSS());
            }
        } else {
            attrs.put("fill", "none");
        }
        if (path.getStroke() != null && path.getStroke().isVisible()) {
            attrs.put("stroke", path.getStroke().toCSS());
            if (path.getStrokeWidth() != 1) {
                attrs.put("stroke-width", smartFloat(path.getStrokeWidth()));
            }
        }

        return new Element("path", attrs, null);
    }

    public static Element renderGeometry(Geometry geo) {
        List<Element> elements = new LinkedList<Element>();

        for (Path path : geo.getPaths()) {
            elements.add(renderPath(path));
        }
        return new Element("g", null, elements);
    }

    public static Element renderSVG(Iterable<?> objects, Rectangle2D bounds) {
        LinkedList<Element> elements = new LinkedList<Element>();
        for (Object o : objects) {
            if (o instanceof Geometry) {
                elements.add(renderGeometry((Geometry) o));
            } else if (o instanceof Path) {
                elements.add(renderPath((Path) o));
            } else {
                throw new RuntimeException("Don't know how to render " + o.getClass().getName());
            }
        }

        StringBuilder viewBox = new StringBuilder();
        appendFloat(viewBox, bounds.getX());
        viewBox.append(' ');
        appendFloat(viewBox, bounds.getY());
        viewBox.append(' ');
        appendFloat(viewBox, bounds.getWidth());
        viewBox.append(' ');
        appendFloat(viewBox, bounds.getHeight());

        Map<String, String> attrs = ImmutableMap.of(
                "xmlns", "http://www.w3.org/2000/svg",
                "width", smartFloat(bounds.getWidth()),
                "height", smartFloat(bounds.getHeight()),
                "viewBox", viewBox.toString());
        return new Element("svg", attrs, elements);
    }

    public static String renderToString(Iterable<?> objects, Rectangle2D bounds) {
        checkArgument(objects != null);
        Element svg = renderSVG(objects, bounds);
        return XML_DECLARATION + svg.toString(4, 0);
    }

    public static void renderToFile(Iterable<?> objects, Rectangle2D bounds, File file) {
        checkArgument(objects != null);
        FileUtils.writeFile(file, renderToString(objects, bounds));
    }

    public static class Element {
        String tag;
        Map<String, String> attributes;
        List<Element> children;

        public Element(String tag, Map<String, String> attributes, List<Element> children) {
            this.tag = tag;
            this.attributes = attributes;
            this.children = children;
        }

        public boolean isSelfClosing() {
            return this.children == null;
        }

        public String toString() {
            return toString(4, 0);
        }

        public String toString(int indent, int start) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < start; i++) {
                sb.append(' ');
            }
            sb.append("<");
            sb.append(tag);
            if (attributes != null) {
                for (Map.Entry<String, String> entry : attributes.entrySet()) {
                    sb.append(" ");
                    sb.append(entry.getKey());
                    sb.append("=\"");
                    sb.append(entry.getValue());
                    sb.append("\"");
                }
            }
            if (isSelfClosing()) {
                sb.append("/>");
            } else {
                sb.append(">\n");
                for (Element child : children) {
                    sb.append(child.toString(indent, start + indent));
                    sb.append('\n');
                }
                for (int i = 0; i < start; i++) {
                    sb.append(' ');
                }
                sb.append("</");
                sb.append(tag);
                sb.append(">");
            }
            return sb.toString();
        }

    }

}
