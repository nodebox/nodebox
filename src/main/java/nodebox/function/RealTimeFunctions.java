package nodebox.function;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import netP5.UdpClient;
import nodebox.graphics.Point;
import nodebox.node.NodeContext;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import oscP5.OscMessage;


public class RealTimeFunctions {

    public static final FunctionLibrary LIBRARY;

    static {
        LIBRARY = JavaLibrary.ofClass("realtime", RealTimeFunctions.class, "mousePosition", "bufferPoints", "receiveOSC", "receiveMultiOSC", "sendOSC", "cacheOSC");
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

    public static List<Object> receiveOSC(String oscAddress, Object defaultValue, NodeContext context) {
        Map<String, List<Object>> oscMessages = (Map<String, List<Object>>) context.getData().get("osc.messages");
        if (oscMessages != null) {
            for (Map.Entry<String, List<Object>> e : oscMessages.entrySet()) {
                if (e.getKey().equals(oscAddress)) {
                    return e.getValue();
                }
            }
        }
        if (defaultValue != null)
            return ImmutableList.of(defaultValue);
        return ImmutableList.of();
    }

    public static List<Map<String, Object>> receiveMultiOSC(String oscAddressPrefix, NodeContext context) {
        Map<String, List<Object>> oscMessages = (Map<String, List<Object>>) context.getData().get("osc.messages");
        if (oscMessages == null) return ImmutableList.of();
        if (oscAddressPrefix.isEmpty()) return ImmutableList.of();
        ImmutableList.Builder<Map<String, Object>> b = ImmutableList.builder();
        for (Map.Entry<String, List<Object>> e : oscMessages.entrySet()) {
            if (e.getKey().startsWith(oscAddressPrefix)) {
                ImmutableMap.Builder<String, Object> mb = ImmutableMap.builder();
                mb.put("address", e.getKey());
                List<Object> values = e.getValue();
                int i = 1;
                for (Object o : e.getValue()) {
                    mb.put("col " + i++, o);
                }
                b.add(mb.build());
            }
        }
        return b.build();
    }

    public static void sendOSC(String ipAddress, long port, String oscAddress, Iterable<Double> oscArguments) {
        OscMessage message = new OscMessage(oscAddress);

        Iterator iterator = oscArguments.iterator();
        while (iterator.hasNext()) {
            message.add(((Double) iterator.next()).floatValue());
        }

        UdpClient c = new UdpClient(ipAddress, (int) port);
        c.send(message.getBytes());
    }

    public static void cacheOSC(String oscAddressPrefix, NodeContext context) {
        Map<String, List<Object>> oscMessages = (Map<String, List<Object>>) context.getData().get("osc.messages");
        Set<String> oscCache = (Set<String>) context.getData().get("osc.cache");
        if (oscMessages == null || oscCache == null) return;
        if (oscAddressPrefix.isEmpty()) return;
        for (Map.Entry<String, List<Object>> e : oscMessages.entrySet()) {
            if (e.getKey().startsWith(oscAddressPrefix)) {
                oscCache.add(e.getKey());
            }
        }
    }
}

