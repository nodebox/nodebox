package nodebox.function;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import netP5.UdpClient;
import nodebox.graphics.Point;
import nodebox.node.NodeContext;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oscP5.OscMessage;

import ddf.minim.analysis.*;
import ddf.minim.*;

public class DeviceFunctions {

    public static final FunctionLibrary LIBRARY;

    static {
        LIBRARY = JavaLibrary.ofClass("device", DeviceFunctions.class, "mousePosition", "bufferPoints", "receiveOSC", "sendOSC",
                "audioAnalysis", "audioLogAvg", "audioWave", "beatDetect");
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

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> receiveOSC(String deviceName, String oscAddressPrefix, String arguments, NodeContext context) {
        Map<String, List<Object>> oscMessages = (Map<String, List<Object>>) context.getData().get(deviceName + ".messages");
        if (oscMessages == null) return ImmutableList.of();
        if (oscAddressPrefix.isEmpty()) return ImmutableList.of();
        Pattern userPattern = Pattern.compile("(<[a-z0-9-_]+?(?::[ifs]|:string|:int|:float)?>)+");
        Matcher upMatcher = userPattern.matcher(oscAddressPrefix);

        Map<String, String> itemTypeMap = new HashMap<String, String>();
        ImmutableList.Builder<String> builder = ImmutableList.builder();
        while (upMatcher.find()) {
            String s = upMatcher.group(0);
            if (s.startsWith("<") && s.endsWith(">"))
                s = s.substring(1, s.length() - 1);
            String[] tokens = s.split(":");
            if (tokens.length == 2) {
                s = tokens[0];
                itemTypeMap.put(s, tokens[1].substring(0, 1));
            } else
                itemTypeMap.put(s, "s");
            builder.add(s);
        }
        ImmutableList<String> messageData = builder.build();

        ArrayList<String> argumentNames = new ArrayList<String>();
        if (! arguments.isEmpty()) {
            for (String arg : arguments.split(","))
                argumentNames.add(arg.trim());
        }

        String convertedAddressPrefix = upMatcher.replaceAll("(XXXPLHXXX)");
        if (! convertedAddressPrefix.endsWith("*"))
            convertedAddressPrefix = convertedAddressPrefix + "*";
        convertedAddressPrefix = convertedAddressPrefix.replaceAll("\\*", ".*?");
        convertedAddressPrefix = "^" + convertedAddressPrefix.replaceAll("(XXXPLHXXX)", "[^\\/]*") + "$";

        Pattern lookupPattern = Pattern.compile(convertedAddressPrefix);
        ImmutableList.Builder<Map<String, Object>> b = ImmutableList.builder();

        int maxArgs = 0;

        for (Map.Entry<String, List<Object>> e : oscMessages.entrySet()) {
            Matcher lpMatcher = lookupPattern.matcher(e.getKey());
            if (lpMatcher.find())
                maxArgs = Math.max(maxArgs, e.getValue().size());
        }

        int argNamesSize = argumentNames.size();
        for (int i = 0 ; i < maxArgs - argNamesSize ; i++)
            argumentNames.add("Column");

        Map<String, Integer> argumentDuplicates = new HashMap<String, Integer>();
        for (String arg : argumentNames) {
            if (argumentDuplicates.containsKey(arg))
                argumentDuplicates.put(arg, 1);
            else
                argumentDuplicates.put(arg, 0);
        }

        ArrayList<String> newArgumentNames = new ArrayList<String>();
        for (String arg : argumentNames) {
            if (argumentDuplicates.get(arg) > 0) {
                newArgumentNames.add(arg + argumentDuplicates.get(arg));
                argumentDuplicates.put(arg, argumentDuplicates.get(arg) + 1);
            } else
                newArgumentNames.add(arg);
        }

        for (Map.Entry<String, List<Object>> e : oscMessages.entrySet()) {
            Matcher lpMatcher = lookupPattern.matcher(e.getKey());

            if (lpMatcher.find()) {
                ImmutableMap.Builder<String, Object> mb = ImmutableMap.builder();
                mb.put("address", e.getKey());
                for (int i = 0; i < lpMatcher.groupCount(); i++) {
                    String msg = messageData.get(i);
                    String msgData = lpMatcher.group(i + 1);
                    if (itemTypeMap.get(msg).equals("s")) {
                        mb.put(msg, msgData);
                    } else if (itemTypeMap.get(msg).equals("i")) {
                        try {
                            mb.put(msg, Integer.parseInt(msgData));
                        } catch (NumberFormatException nfe) {
                            mb.put(msg, 0);
                        }
                    } else if (itemTypeMap.get(msg).equals("f")) {
                        try {
                            mb.put(msg, Double.parseDouble(msgData));
                        } catch (NumberFormatException nfe) {
                            mb.put(msg, 0.0d);
                        }
                    }
                }
                int i = 0;
                for (Object o : e.getValue())  {
                    String arg = newArgumentNames.get(i);
                    mb.put(arg, o);
                    i++;
                }
                for ( ; i < newArgumentNames.size(); i++) {
                    mb.put(newArgumentNames.get(i), 0);
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

    public static List<Double> audioAnalysis(String deviceName, String channel, long averages, NodeContext context) {
        AudioSource source = (AudioSource) context.getData().get(deviceName + ".source");
        if (source == null) return ImmutableList.of();
        FFT fft = new FFT( source.bufferSize(), source.sampleRate() );
        fft.window(FFT.HANN);

        if (averages > 0)
            fft.linAverages((int) averages);

        if (channel.equals("left")) {
            fft.forward(source.left);
        } else if (channel.equals("right")) {
            fft.forward(source.right);
        } else {
            fft.forward(source.mix);
        }

        ImmutableList.Builder<Double> b = new ImmutableList.Builder<Double>();
        if (averages == 0) {
            for (int i = 0; i < fft.specSize(); i++)
                b.add((double) fft.getBand(i));
        } else {
           for(int i = 0; i < fft.avgSize(); i++)
               b.add((double) fft.getAvg(i));
        }
        return b.build();
    }

    public static List<Double> audioLogAvg(String deviceName, String channel, long baseFreq, long bandsPerOctave, NodeContext context) {
        AudioSource source = (AudioSource) context.getData().get(deviceName + ".source");
        if (source == null) return ImmutableList.of();
        FFT fft = new FFT( source.bufferSize(), source.sampleRate() );
        fft.window(FFT.HANN);

        fft.logAverages((int) baseFreq, (int) bandsPerOctave);

        if (channel.equals("left")) {
            fft.forward(source.left);
        } else if (channel.equals("right")) {
            fft.forward(source.right);
        } else {
            fft.forward(source.mix);
        }

        ImmutableList.Builder<Double> b = new ImmutableList.Builder<Double>();
        for(int i = 0; i < fft.avgSize(); i++)
            b.add((double) fft.getAvg(i));
        return b.build();
    }

    public static List<Map<String, Double>> audioWave(String deviceName, NodeContext context) {
        AudioSource source = (AudioSource) context.getData().get(deviceName + ".source");
        if (source == null) return ImmutableList.of();
        ImmutableList.Builder<Map<String, Double>> b = ImmutableList.builder();
        for (int i = 0; i < source.bufferSize(); i++) {
            ImmutableMap.Builder<String, Double> mb = ImmutableMap.builder();
            mb.put("left", (double) source.left.get(i));
            mb.put("right", (double) source.right.get(i));
            mb.put("mix", (double) source.mix.get(i));
            b.add(mb.build());
        }
        return b.build();
    }

    public static Map<String, Boolean> beatDetect(String deviceName, NodeContext context) {
        BeatDetect beat = (BeatDetect) context.getData().get(deviceName + ".beat");
        if (beat == null) return ImmutableMap.of();
        ImmutableMap.Builder<String, Boolean> mb = ImmutableMap.builder();
        mb.put("beat", beat.isOnset());
        mb.put("kick", beat.isKick());
        mb.put("snare", beat.isSnare());
        mb.put("hat", beat.isHat());
        return mb.build();
    }
}

