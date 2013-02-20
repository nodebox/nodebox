package nodebox.function;

import blobDetection.Blob;
import blobDetection.BlobDetection;
import blobDetection.EdgeVertex;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import netP5.UdpClient;
import nodebox.graphics.*;
import nodebox.graphics.Color;
import nodebox.graphics.Point;
import nodebox.node.NodeContext;

import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oscP5.OscMessage;

import ddf.minim.analysis.*;
import ddf.minim.*;
import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;

import SimpleOpenNI.*;

public class DeviceFunctions {

    public static final FunctionLibrary LIBRARY;

    private static int kinectWidth = 640;
    private static int kinectHeight = 480;
    private static BlobDetection theBlobDetection = new BlobDetection(kinectWidth / 3, kinectHeight / 3);

    static {
        LIBRARY = JavaLibrary.ofClass("device", DeviceFunctions.class, "mousePosition", "bufferPoints", "receiveOSC", "sendOSC", "kinectSkeleton", "kinectData", "kinectBlobs", "audioAnalysis");
        theBlobDetection.setThreshold(0.2f);
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
        if (!arguments.isEmpty()) {
            for (String arg : arguments.split(","))
                argumentNames.add(arg.trim());
        }

        String convertedAddressPrefix = upMatcher.replaceAll("(XXXPLHXXX)");
        if (!convertedAddressPrefix.endsWith("*"))
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
        for (int i = 0; i < maxArgs - argNamesSize; i++)
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
                for (Object o : e.getValue()) {
                    String arg = newArgumentNames.get(i);
                    mb.put(arg, o);
                    i++;
                }
                for (; i < newArgumentNames.size(); i++) {
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

    public static List<Double> audioAnalysis(String deviceName, long averages, NodeContext context) {
        AudioPlayer player = (AudioPlayer) context.getData().get(deviceName + ".player");
        if (player == null) return ImmutableList.of();
        FFT fft = new FFT(player.bufferSize(), player.sampleRate());
        if (averages > 0)
            fft.linAverages((int) averages);
        fft.forward(player.mix);
        ImmutableList.Builder<Double> b = new ImmutableList.Builder<Double>();
        if (averages == 0) {
            for (int i = 0; i < fft.specSize(); i++)
                b.add((double) fft.getBand(i));
        } else {
            for (int i = 0; i < fft.avgSize(); i++)
                b.add((double) fft.getAvg(i));
        }
        return b.build();
    }

    public static List<Map<String, Object>> kinectSkeleton(NodeContext context) {
        Map<Integer, Map<String, List<Float>>> data = (Map<Integer, Map<String, List<Float>>>) context.getData().get("kinect.skeletondata");
        if (data == null) return ImmutableList.of();
        if (data.isEmpty()) return ImmutableList.of();

        ImmutableList.Builder<Map<String, Object>> b = ImmutableList.builder();
        for (int userId : data.keySet()) {
            for (Map.Entry<String, List<Float>> entry : data.get(userId).entrySet()) {
                ImmutableMap.Builder<String, Object> mb = ImmutableMap.builder();
                mb.put("userid", userId);
                List<Float> xyz = entry.getValue();
                mb.put("x", xyz.get(0));
                mb.put("y", xyz.get(1));
                mb.put("z", xyz.get(2));
                b.add(mb.build());
            }
        }
        return b.build();
    }

    public static List<Map<String, Object>> kinectData(long steps, long minimumZ, long maximumZ, NodeContext context) {
        SimpleOpenNI ctx = (SimpleOpenNI) context.getData().get("kinect.context");
        if (ctx == null) return ImmutableList.of();
        int[] depthMap = ctx.depthMap();
        int index;
        PVector realWorldPoint;

        PVector[] realWorldMap = ctx.depthMapRealWorld();
        ImmutableList.Builder<Map<String, Object>> b = ImmutableList.builder();
        PVector tempVec1 = new PVector();
        PImage rgbImage = ctx.rgbImage();

        for (int y = 0; y < ctx.depthHeight(); y += steps) {
            for (int x = 0; x < ctx.depthWidth(); x += steps) {
                index = x + y * ctx.depthWidth();
                if (depthMap[index] > 0) {
                    realWorldPoint = realWorldMap[index];
                    if (realWorldPoint.z >= minimumZ && realWorldPoint.z <= maximumZ) {
                        ImmutableMap.Builder<String, Object> mb = ImmutableMap.builder();
                        ctx.convertRealWorldToProjective(realWorldPoint, tempVec1);

                        mb.put("x", realWorldPoint.x);
                        mb.put("y", realWorldPoint.y);
                        mb.put("z", realWorldPoint.z);
                        mb.put("px", tempVec1.x);
                        mb.put("py", tempVec1.y);

                        if (rgbImage != null) {
                            int c = rgbImage.pixels[index];
                            int alpha = (c >> 24) & 0xFF;
                            int red = (c >> 16) & 0xFF;
                            int green = (c >> 8) & 0xFF;
                            int blue = c & 0xFF;

                            mb.put("color", new Color(red / 255.0, green / 255.0, blue / 255.0));
                        }

                        b.add(mb.build());
                    }
                }
            }
        }
        return b.build();
    }

    public static Geometry kinectBlobs(NodeContext context) {
        SimpleOpenNI ctx = (SimpleOpenNI) context.getData().get("kinect.context");
        if (ctx == null) return null;

        PImage cam = ctx.sceneImage();
        PImage blobs = new PImage(kinectWidth / 3, kinectHeight / 3, PApplet.RGB);
        blobs.copy(cam, 0, 0, cam.width, cam.height, 0, 0, blobs.width, blobs.height);

        int[] pixels = blobs.pixels;
        for (int i = 0; i < pixels.length; i++) {
            int c = pixels[i];
            int red = (c >> 16) & 0xFF;
            int green = (c >> 8) & 0xFF;
            int blue = c & 0xFF;
            if (red == green && green == blue)
                pixels[i] = 0;
        }
        blobs.updatePixels();
        fastblur(blobs, 2);
        theBlobDetection.computeBlobs(pixels);

        Geometry g = new Geometry();
        Blob b;
        EdgeVertex eA, eB;
        for (int n = 0; n < theBlobDetection.getBlobNb(); n++) {
            b = theBlobDetection.getBlob(n);
            if (b != null) {
                Path path = new Path();
                for (int m = 0; m < b.getEdgeNb(); m++) {
                    eA = b.getEdgeVertexA(m);
                    eB = b.getEdgeVertexB(m);
                    if (eA != null && eB != null) {
                        path.addPoint(new Point(eA.x * kinectWidth, eA.y * kinectHeight));
                    }
//                            line(
//                                    eA.x*width, eA.y*height,
//                                    eB.x*width, eB.y*height
//                            );
                }
                path.close();
                g.add(path);
            }
        }
        return g;
    }

    // ==================================================
    // Super Fast Blur v1.1
    // by Mario Klingemann
    // <http://incubator.quasimondo.com>
    // ==================================================
    private static void fastblur(PImage img, int radius) {
        if (radius < 1) {
            return;
        }
        int w = img.width;
        int h = img.height;
        int wm = w - 1;
        int hm = h - 1;
        int wh = w * h;
        int div = radius + radius + 1;
        int r[] = new int[wh];
        int g[] = new int[wh];
        int b[] = new int[wh];
        int rsum, gsum, bsum, x, y, i, p, p1, p2, yp, yi, yw;
        int vmin[] = new int[Math.max(w, h)];
        int vmax[] = new int[Math.max(w, h)];
        int[] pix = img.pixels;
        int dv[] = new int[256 * div];
        for (i = 0; i < 256 * div; i++) {
            dv[i] = (i / div);
        }

        yw = yi = 0;

        for (y = 0; y < h; y++) {
            rsum = gsum = bsum = 0;
            for (i = -radius; i <= radius; i++) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))];
                rsum += (p & 0xff0000) >> 16;
                gsum += (p & 0x00ff00) >> 8;
                bsum += p & 0x0000ff;
            }
            for (x = 0; x < w; x++) {

                r[yi] = dv[rsum];
                g[yi] = dv[gsum];
                b[yi] = dv[bsum];

                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm);
                    vmax[x] = Math.max(x - radius, 0);
                }
                p1 = pix[yw + vmin[x]];
                p2 = pix[yw + vmax[x]];

                rsum += ((p1 & 0xff0000) - (p2 & 0xff0000)) >> 16;
                gsum += ((p1 & 0x00ff00) - (p2 & 0x00ff00)) >> 8;
                bsum += (p1 & 0x0000ff) - (p2 & 0x0000ff);
                yi++;
            }
            yw += w;
        }

        for (x = 0; x < w; x++) {
            rsum = gsum = bsum = 0;
            yp = -radius * w;
            for (i = -radius; i <= radius; i++) {
                yi = Math.max(0, yp) + x;
                rsum += r[yi];
                gsum += g[yi];
                bsum += b[yi];
                yp += w;
            }
            yi = x;
            for (y = 0; y < h; y++) {
                pix[yi] = 0xff000000 | (dv[rsum] << 16) | (dv[gsum] << 8) | dv[bsum];
                if (x == 0) {
                    vmin[y] = Math.min(y + radius + 1, hm) * w;
                    vmax[y] = Math.max(y - radius, 0) * w;
                }
                p1 = x + vmin[y];
                p2 = x + vmax[y];

                rsum += r[p1] - r[p2];
                gsum += g[p1] - g[p2];
                bsum += b[p1] - b[p2];

                yi += w;
            }
        }

    }
}

