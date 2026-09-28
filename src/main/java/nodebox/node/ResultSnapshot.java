package nodebox.node;

import com.google.common.collect.ImmutableList;
import nodebox.graphics.Contour;
import nodebox.graphics.Geometry;
import nodebox.graphics.Image;
import nodebox.graphics.Path;
import nodebox.graphics.Text;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A value copy of a render result, for comparing two results by content.
 * <p/>
 * The geometry classes compare by identity and are mutable, so this converts them to plain lists of their
 * visible properties. Lists and maps are copied element by element; other values are kept as they are and
 * compared with their own equals.
 */
final class ResultSnapshot {

    private ResultSnapshot() {
    }

    static Object of(Object value) {
        if (value instanceof Geometry) {
            return ImmutableList.of("Geometry", of(((Geometry) value).getPaths()));
        } else if (value instanceof Path) {
            Path p = (Path) value;
            return ImmutableList.of("Path", nullable(p.getFillColor()), nullable(p.getStrokeColor()),
                    p.getStrokeWidth(), of(p.getContours()));
        } else if (value instanceof Contour) {
            Contour c = (Contour) value;
            return ImmutableList.of("Contour", c.isClosed(), of(c.getPoints()));
        } else if (value instanceof Text) {
            Text t = (Text) value;
            return ImmutableList.of("Text", t.getText(), t.getBaseLineX(), t.getBaseLineY(), t.getWidth(),
                    t.getHeight(), t.getFontName(), t.getFontSize(), t.getLineHeight(), t.getAlign(),
                    nullable(t.getFillColor()), nullable(t.getTransform()));
        } else if (value instanceof Image) {
            Image i = (Image) value;
            return ImmutableList.of("Image", i.getX(), i.getY(), i.getWidth(), i.getHeight(), i.getAlpha(),
                    nullable(i.getTransform()));
        } else if (value instanceof Map) {
            Map<Object, Object> copy = new LinkedHashMap<Object, Object>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                copy.put(entry.getKey(), of(entry.getValue()));
            }
            return copy;
        } else if (value instanceof Iterable) {
            List<Object> copy = new ArrayList<Object>();
            for (Object element : (Iterable<?>) value) {
                copy.add(of(element));
            }
            return copy;
        }
        return value;
    }

    // ImmutableList does not hold nulls.
    private static Object nullable(Object value) {
        return value == null ? "null" : value;
    }
}
