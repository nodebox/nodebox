package nodebox.graphics;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import nodebox.util.FileUtils;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

public class CSVRenderer {

    static Set<String> keySet(Object o) {
        if (o instanceof Geometry) {
            return ImmutableSet.of("x", "y", "width", "height");
        } else if (o instanceof Path) {
            return ImmutableSet.of("d", "fill", "stroke", "stroke-width", "x", "y", "width", "height");
        } else if (o instanceof Point) {
            return ImmutableSet.of("x", "y");
        } else if (o instanceof Map) {
            Set<String> keys = new LinkedHashSet<>();
            for (Object key : ((Map) o).keySet()) {
                keys.add(key.toString());
            }
            return keys;
        } else {
            return ImmutableSet.of("value");
        }
    }

    @SuppressWarnings("unchecked")
    static Map<String, ?> objectAsMap(Object o) {
        if (o instanceof Geometry) {
            Geometry geo = (Geometry) o;
            Rect r = geo.getBounds();
            return ImmutableMap.of("x", r.x, "y", r.y, "width", r.width, "height", r.height);
        } else if (o instanceof Path) {
            Path path = (Path) o;
            Rect r = path.getBounds();
            String d = SVGRenderer.renderPathData(path);
            String fill = path.getFill() != null ? path.getFill().toCSS() : "none";
            String stroke = path.getStroke() != null ? path.getStroke().toCSS() : "none";
            String strokeWidth = String.valueOf(path.getStrokeWidth());
            return ImmutableMap.<String, Object>builder()
                    .put("d", d)
                    .put("fill", fill)
                    .put("stroke", stroke)
                    .put("stroke-width", strokeWidth)
                    .put("x", r.x)
                    .put("y", r.y)
                    .put("width", r.width)
                    .put("height", r.height)
                    .build();
        } else if (o instanceof Point) {
            Point point = (Point) o;
            return ImmutableMap.of("x", point.x, "y", point.y);
        } else if (o instanceof Map) {
            return (Map<String, ?>) o;
        } else {
            return ImmutableMap.of("value", o != null ? o : "null");
        }
    }

    private static String valueInQuotes(String s) {
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        int l = s.length();
        for (int i = 0; i < l; i++) {
            char c = s.charAt(i);
            if (c == '"') {
                sb.append('"');
            }
            sb.append(c);
        }
        sb.append('"');
        return sb.toString();
    }

    public static String renderToString(Iterable<?> objects, char delimiter, boolean quotes) {
        checkArgument(objects != null);

        Set<String> keySet = new LinkedHashSet<>();

        for (Object o : objects) {
            keySet.addAll(keySet(o));
        }

        StringBuilder sb = new StringBuilder();

        // Write header
        boolean first = true;
        for (String k : keySet) {
            if (first) {
                first = false;
            } else {
                sb.append(delimiter);
            }
            sb.append(quotes ? valueInQuotes(k) : k);
        }
        sb.append('\n');

        // Write table
        for (Object o : objects) {
            Map<String, ?> m = objectAsMap(o);
            first = true;
            for (String k : keySet) {
                if (first) {
                    first = false;
                } else {
                    sb.append(delimiter);
                }
                String v = m.get(k).toString();
                sb.append(quotes ? valueInQuotes(v) : v);
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    public static void renderToFile(Iterable<?> objects, File file, char delimiter, boolean quotes) {
        checkArgument(objects != null);
        FileUtils.writeFile(file, renderToString(objects, delimiter, quotes));
    }

}
