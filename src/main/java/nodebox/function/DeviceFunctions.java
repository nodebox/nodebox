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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oscP5.OscMessage;


public class DeviceFunctions {

    public static final FunctionLibrary LIBRARY;

    static {
        LIBRARY = JavaLibrary.ofClass("realtime", DeviceFunctions.class, "mousePosition", "bufferPoints", "receiveOSC", "sendOSC");
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

    public static List<Map<String, Object>> receiveOSC(String oscAddressPrefix, NodeContext context) {
        Map<String, List<Object>> oscMessages = (Map<String, List<Object>>) context.getData().get("osc.messages");
        if (oscMessages == null) return ImmutableList.of();
        if (oscAddressPrefix.isEmpty()) return ImmutableList.of();

        Pattern userPattern = Pattern.compile("(<[a-z0-9-_]+>)+");
        Matcher upMatcher = userPattern.matcher(oscAddressPrefix);

        ImmutableList.Builder<String> builder = ImmutableList.builder();
        while (upMatcher.find()) {
            String s = upMatcher.group(0);
            if (s.startsWith("<") && s.endsWith(">"))
                s = s.substring(1, s.length() - 1);
            builder.add(s);
        }
        ImmutableList<String> messageData = builder.build();

        String convertedAddressPrefix = upMatcher.replaceAll("(XXXPLHXXX)");
        if (! convertedAddressPrefix.endsWith("*"))
            convertedAddressPrefix = convertedAddressPrefix + "*";
        convertedAddressPrefix = convertedAddressPrefix.replaceAll("\\*", ".*?");
        convertedAddressPrefix = "^" + convertedAddressPrefix.replaceAll("(XXXPLHXXX)", "[^\\/]*") + "$";

        Pattern lookupPattern = Pattern.compile(convertedAddressPrefix);
        ImmutableList.Builder<Map<String, Object>> b = ImmutableList.builder();

        for (Map.Entry<String, List<Object>> e : oscMessages.entrySet()) {
            Matcher lpMatcher = lookupPattern.matcher(e.getKey());

            if (lpMatcher.find()) {
                ImmutableMap.Builder<String, Object> mb = ImmutableMap.builder();
                mb.put("address", e.getKey());
                for (int i = 0; i < lpMatcher.groupCount(); i++)
                    mb.put(messageData.get(i), lpMatcher.group(i + 1));
                int i = 1;
                for (Object o : e.getValue())
                    mb.put("col " + i++, o);
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
}

