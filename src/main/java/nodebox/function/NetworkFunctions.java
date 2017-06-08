package nodebox.function;

import com.sun.org.apache.xerces.internal.impl.dv.xs.BooleanDV;
import com.sun.org.apache.xpath.internal.operations.Bool;
import nodebox.graphics.Geometry;
import nodebox.graphics.IGeometry;
import nodebox.graphics.Path;
import nodebox.graphics.Point;
import nodebox.graphics.Rect;
import nodebox.graphics.Color;
import nodebox.network.WebSocketMessaging;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.jayway.jsonpath.JsonPath;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;

//import java.io.IOException;
//import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.io.*;
import java.net.*;

import nodebox.network.*;

import javax.json.*;
import javax.json.stream.*;
//import javax.json.JsonObject;
//import javax.json.JsonReader ;
import javax.json.JsonObjectBuilder;

public class NetworkFunctions {

    public static final Map<Integer, Response> responseCache = new HashMap<Integer, Response>();
    public static final FunctionLibrary LIBRARY;
    //private boolean isConnected = false;

    private static Socket socket = null;
    private static DataOutputStream outToServer;
    private static BufferedReader inFromServer;
    private static boolean isConnected = false;
    private static String strInFromServer = "";

   // private static SocketClient socketClient = null;
    //private static Thread socketThread;

    static {
        LIBRARY = JavaLibrary.ofClass("network", NetworkFunctions.class,
                "httpGet", "queryJSON", "encodeURL", "socketClientSendData", "geometrySuperToJson", "cookGraph");
    }

    public static synchronized Map<String, Object> httpGet(final String url, final String username, final String password, final long refreshTimeSeconds) {
        Integer cacheKey = Objects.hashCode(url, username, password);
        if (responseCache.containsKey(cacheKey)) {
            Response r = responseCache.get(cacheKey);
            long timeNow = nowSeconds();
            long timeFetched = r.timeFetched;
            if ((timeNow - timeFetched) <= refreshTimeSeconds) {
                return r.response;
            }
        }
        Map<String, Object> r = _httpGet(url, username, password);
        Response res = new Response(nowSeconds(), r);
        responseCache.put(cacheKey, res);
        return r;
    }

    public static Iterable<?> queryJSON(final Object json, final String query) {
        if (json instanceof Map) {
            Map<?, ?> requestMap = (Map<?, ?>) json;
            if (requestMap.containsKey("body")) {
                return queryJSON(requestMap.get("body"), query);
            } else {
                throw new IllegalArgumentException("Cannot parse JSON input.");
            }
        } else if (json instanceof String) {
            Object results = JsonPath.read((String) json, query);
            if (!(results instanceof Iterable)) {
                return ImmutableList.of(results);
            } else {
                return (Iterable<?>) results;
            }
        } else {
            throw new IllegalArgumentException("Cannot parse JSON input.");
        }
    }

    public static String encodeURL(final String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return s;
        }
    }

    private static JsonObjectBuilder pointToJson(JsonBuilderFactory factory, Point p) {
        JsonObjectBuilder rootObj = factory.createObjectBuilder();
        rootObj.add(WSDefs.TAGS.CLASS, WSDefs.TAGS.POINT);
        rootObj.add(WSDefs.TAGS.X, p.getX());
        rootObj.add(WSDefs.TAGS.Y, p.getY());
        return(rootObj);
    }

    private static JsonArrayBuilder pointListToJson(JsonBuilderFactory factory, List<Point> ps) {
        JsonArrayBuilder rootObj = factory.createArrayBuilder();
        for (Point p : ps) {
            rootObj.add(pointToJson(factory,  p));
        }
        return(rootObj);
    }

    private static JsonObjectBuilder rectToJson(JsonBuilderFactory factory, Rect r) {
        JsonObjectBuilder rootObj = factory.createObjectBuilder();
        rootObj.add(WSDefs.TAGS.CLASS, WSDefs.TAGS.RECT);
        rootObj.add(WSDefs.TAGS.HEIGHT, r.getHeight());
        rootObj.add(WSDefs.TAGS.WIDTH, r.getWidth());
        rootObj.add(WSDefs.TAGS.POSITION, pointToJson(factory, r.getPosition()));
        return(rootObj);
    }

    private static JsonObjectBuilder colorToJson(JsonBuilderFactory factory, Color c) {
        if(c == null) return null;
        JsonObjectBuilder rootObj = factory.createObjectBuilder();
        rootObj.add(WSDefs.TAGS.CLASS, WSDefs.TAGS.COLOR);
        rootObj.add(WSDefs.TAGS.R, c.getRed());
        rootObj.add(WSDefs.TAGS.G, c.getGreen());
        rootObj.add(WSDefs.TAGS.B, c.getBlue());
        rootObj.add(WSDefs.TAGS.A, c.getAlpha());
        return(rootObj);
    }

    private static JsonObjectBuilder pathToJson(JsonBuilderFactory factory, Path p) {
        JsonObjectBuilder rootObj = factory.createObjectBuilder();
        rootObj.add(WSDefs.TAGS.CLASS, WSDefs.TAGS.PATH);
        rootObj.add(WSDefs.TAGS.POINTCOUNT, p.getPointCount());
        rootObj.add(WSDefs.TAGS.POINTS, pointListToJson(factory, p.getPoints()));
        rootObj.add(WSDefs.TAGS.BOUNDS, rectToJson(factory, p.getBounds()));
        Color fc = p.getFillColor();

        if(fc == null) rootObj.addNull(WSDefs.TAGS.FILLCOLOR);
        else rootObj.add(WSDefs.TAGS.FILLCOLOR, colorToJson(factory, p.getFillColor()));
        Color sc = p.getStrokeColor();
        if(sc == null) rootObj.addNull(WSDefs.TAGS.STROKECOLOR);
        else rootObj.add(WSDefs.TAGS.STROKECOLOR, colorToJson(factory, p.getStrokeColor()));

        rootObj.add(WSDefs.TAGS.STROKEWIDTH, p.getStrokeWidth());
        rootObj.add(WSDefs.TAGS.STROKECLOSED, p.isClosed());
        return(rootObj);
    }

    private static JsonArrayBuilder pathListToJson(JsonBuilderFactory factory, List<Path> ps) {
        JsonArrayBuilder rootObj = factory.createArrayBuilder();
        for (Path p : ps) {
            rootObj.add(pathToJson(factory,  p));
        }
        return(rootObj);
    }

    public static JsonObjectBuilder geometryToJson(JsonBuilderFactory factory, IGeometry shape) {
        JsonObjectBuilder rootObj = factory.createObjectBuilder();
        rootObj.add(WSDefs.TAGS.CLASS, WSDefs.TAGS.GEOMETRY);
        rootObj.add(WSDefs.TAGS.PATHS, pathListToJson(factory, ((Geometry)shape).getPaths()));
        return(rootObj);
    }


    public static String geometrySuperToJson(List<IGeometry> shapes) {
        if (shapes == null) return null;

        JsonBuilderFactory factory = Json.createBuilderFactory(null);
        JsonObjectBuilder rootObj = factory.createObjectBuilder();
        JsonArrayBuilder rootArray = factory.createArrayBuilder();

        for (IGeometry shape : shapes) {
            if (shape instanceof Path) {
                rootArray.add(pathToJson(factory, (Path)shape));
            }
            else if (shape instanceof Geometry) {
                JsonObjectBuilder curGeoRoot = factory.createObjectBuilder();
                rootArray.add(geometryToJson(factory, shape));
                rootArray.add(curGeoRoot);
            }
            else {
                System.out.println(shape.getClass().toString());
                throw new RuntimeException("Unable to generate json for " + shape + ": I can only process paths or geometry objects.");
            }
        }

        rootObj.add("geometry", rootArray);
        JsonObject finalVal =rootObj.build();
        return finalVal.toString();
    }

    private static JsonObject JsonObjectFromString(String str) {
        JsonReader jsonReader = Json.createReader(new StringReader(str));
        JsonObject object = jsonReader.readObject();
        //jsonReader.close();
        return object;
    }

    public static String socketClientSendData(String jsonData, final long timeOut) {
        if(jsonData == null) return null;
        if(jsonData.isEmpty()) return null;

        return socketClientSendDataJson(jsonData, timeOut);
    }

    private static String socketClientSendDataJson(String jsonData, final long timeOut) {

        JsonObject object = JsonObjectFromString(jsonData);
        UUID id = WebSocketMessaging.sendData(object);
        if(id == null) return null;

        // Since we're running a loop we can do poor persons timeout
        JsonObject retMsg = null;
        long curTime = nowMiliSeconds();
        while(retMsg == null && (nowMiliSeconds() - curTime) < timeOut)
        {
            retMsg = WebSocketMessaging.getMessage(id);
        }

        if(retMsg == null) return null;
        return retMsg.toString();
    }

    public static List<IGeometry> cookGraph(List<IGeometry> shapes, Object cookObj) {
        return shapes;
    }

    /*
    private static Object socketClientSendData(Iterable<String> keys, Iterable<Object> values, final long timeOut, boolean retJson) {
        if (keys == null || values == null ) return ImmutableMap.of();
        if (Iterables.size(keys) != Iterables.size(values)) throw new IllegalArgumentException("Key and Value Lists must be the same size.");

        ImmutableMap.Builder<String, Object> b = ImmutableMap.builder();

        Iterator<String> keyIterator = keys.iterator();
        Iterator<Object> valueIterator = values.iterator();

        // Converting to json
        JsonObjectBuilder builder = Json.createObjectBuilder();
        while (keyIterator.hasNext() && valueIterator.hasNext()) {
            String key = keyIterator.next().toString();
            Object value = valueIterator.next();

            if(value instanceof Number) {
                builder.add(key, (float)value);
            } else if(value instanceof String) {
                builder.add(key, value.toString());
            } else if(value instanceof Boolean) {
                builder.add(key, (boolean)value);
            }
        }

        JsonObject object = builder.build();

        UUID id = WebSocketMessaging.sendData(object);
        if(id == null) return(ImmutableMap.of());

        // Since we're running a loop we can do poor persons timeout
        JsonObject retMsg = null;
        long curTime = nowMiliSeconds();
        while(retMsg == null && (nowMiliSeconds() - curTime) < timeOut)
        {
            retMsg = WebSocketMessaging.getMessage(id);
        }

        if(retMsg == null) return(ImmutableMap.of());

        if(retJson) return retMsg.toString();

        for (Map.Entry<String, JsonValue> entry : retMsg.entrySet())
        {
            String key = entry.getKey();
            JsonValue value = entry.getValue();
            b.put(key, value);
        }
        b.put("testNumber", 5);
        b.put("testBool", true);
        return b.build();
    }
    */

    private static Map<String, Object> _httpGet(final String url, final String username, final String password) {
        HttpGet request = new HttpGet(url);

        if (username != null && !username.trim().isEmpty()) {
            UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);
            BasicScheme scheme = new BasicScheme();
            Header authorizationHeader;
            try {
                authorizationHeader = scheme.authenticate(credentials, request, new BasicHttpContext());
            } catch (AuthenticationException e) {
                throw new RuntimeException(e);
            }
            request.addHeader(authorizationHeader);
        }

        try {
            DefaultHttpClient client = new DefaultHttpClient();
            HttpResponse response = client.execute(request);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                String body = EntityUtils.toString(entity);
                HashMap<String, String> m = new HashMap<String, String>();
                for (Header h : response.getAllHeaders()) {
                    m.put(h.getName(), h.getValue());
                }

                Map<String, String> headers = ImmutableMap.copyOf(m);
                return ImmutableMap.of(
                        "body", body,
                        "statusCode", response.getStatusLine().getStatusCode(),
                        "headers", headers);
            } else {
                // 204 No Content
                return emptyResponse(204);
            }
        } catch (IllegalStateException e) {
            if (e.getMessage().startsWith("Target host must not be null")) {
                throw new RuntimeException("URL should start with \"http://\" or \"https://\".");
            } else {
                throw e;
            }
        } catch (IOException e) {
            // We return status code 408 (Request Timeout) here since we always want to return a valid response.
            // However, the exception signifies an IO error, so maybe the network connection is down.
            // This has no valid HTTP response (since there is NO response).
            return emptyResponse(408);
        }
    }

    private static Map<String, Object> emptyResponse(int statusCode) {
        return ImmutableMap.<String, Object>of(
                "body", "",
                "statusCode", statusCode
        );
    }

    private static long nowSeconds() {
        return System.currentTimeMillis() / 1000;
    }
    private static long nowMiliSeconds() { return System.currentTimeMillis();    }

    private static class Response {
        private final long timeFetched;
        private final Map<String, Object> response;

        private Response(long timeFetched, Map<String, Object> response) {
            this.timeFetched = timeFetched;
            this.response = response;
        }
    }

}
