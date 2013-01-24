package nodebox.function;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import netP5.UdpClient;
import nodebox.graphics.Point;
import nodebox.node.NodeContext;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import netP5.NetAddress;
import oscP5.OscMessage;


public class RealTimeFunctions {

    public static final FunctionLibrary LIBRARY;

    static {
        LIBRARY = JavaLibrary.ofClass("realTime", RealTimeFunctions.class, "mousePosition", "bufferPoints", "receiveOSC", "sendOSC");
    }

    public static Point mousePosition(NodeContext context) {
        Point p = (Point) context.getData().get("mouse.position");
        if (p != null) {
            return p;
        } else {
            return Point.ZERO;
        }
    }

    public static List<Point> bufferPoints(Point point, long size, final List<Point> previousPoints) {
        ImmutableList.Builder<Point> newPoints = ImmutableList.builder();
        if (previousPoints.size() == size) {
            newPoints.addAll(Iterables.skip(previousPoints, 1));
        } else {
            newPoints.addAll(previousPoints);
        }
        newPoints.add(point);
        return newPoints.build();
    }

    public static List<Object> receiveOSC(String address, NodeContext context) {
        Map<String, List<Object>> oscMessages = (Map<String, List<Object>>) context.getData().get("osc.messages");
        if (oscMessages != null) {
            for (Map.Entry<String, List<Object>> e : oscMessages.entrySet()) {
                if (e.getKey().equals(address)) {
                    return e.getValue();
                }
            }
            return ImmutableList.of();
        } else {
            return ImmutableList.of();
        }
    }

    public static void sendOSC(String address, long port, String route, Iterable<Double> iterable) {

        OscMessage message = new OscMessage(route);

        Iterator iterator = iterable.iterator();
        while (iterator.hasNext()) {
            message.add(((Double) iterator.next()).floatValue());
        }

        UdpClient c = new UdpClient(address,(int) port);
        c.send(message.getBytes());

    }

}

