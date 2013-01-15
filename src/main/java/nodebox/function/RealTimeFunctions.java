package nodebox.function;

import nodebox.graphics.Point;
import nodebox.node.NodeContext;

import java.util.ArrayList;
import java.util.List;

public class RealTimeFunctions {

    public static final FunctionLibrary LIBRARY;

    public static final ThreadLocal<List<Point>> pointBuffer = new ThreadLocal<List<Point>>();

    static {
        LIBRARY = JavaLibrary.ofClass("realTime", RealTimeFunctions.class, "mousePosition", "bufferPoints");
    }

    public static Point mousePosition(NodeContext context) {
        Point p = (Point) context.getData().get("mouse.position");
        if (p != null) {
            return p;
        } else {
            return Point.ZERO;
        }
    }

    public static List<Point> bufferPoints(Point point, long size) {
        List<Point> buffer = pointBuffer.get();
        if (buffer == null) {
            buffer = new ArrayList<Point>(100);
        }
        buffer.add(point);
        if (buffer.size() > size) {
            buffer.remove(0);
        }
        pointBuffer.set(buffer);
        return buffer;
    }

}

