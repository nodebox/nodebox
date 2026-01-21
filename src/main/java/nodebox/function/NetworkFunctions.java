package nodebox.function;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.jayway.jsonpath.JsonPath;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class NetworkFunctions {

    public static final Map<Integer, Response> responseCache = new HashMap<Integer, Response>();
    public static final FunctionLibrary LIBRARY;

    static {
        LIBRARY = JavaLibrary.ofClass("network", NetworkFunctions.class,
                "httpGet", "queryJSON", "encodeURL");
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

    private static Map<String, Object> _httpGet(final String url, final String username, final String password) {
        HttpGet request = new HttpGet(url);

        if (username != null && !username.trim().isEmpty()) {
            String safePassword = password == null ? "" : password;
            String credentials = username + ":" + safePassword;
            String encodedCredentials = Base64.getEncoder()
                    .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            request.addHeader("Authorization", "Basic " + encodedCredentials);
        }

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpClientResponseHandler<Map<String, Object>> handler = new HttpClientResponseHandler<Map<String, Object>>() {
                @Override
                public Map<String, Object> handleResponse(ClassicHttpResponse response) throws IOException {
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        String body;
                        try {
                            body = EntityUtils.toString(entity);
                        } catch (ParseException e) {
                            throw new IOException(e);
                        }
                        HashMap<String, String> m = new HashMap<String, String>();
                        for (Header h : response.getHeaders()) {
                            m.put(h.getName(), h.getValue());
                        }

                        Map<String, String> headers = ImmutableMap.copyOf(m);
                        return ImmutableMap.of(
                                "body", body,
                                "statusCode", response.getCode(),
                                "headers", headers);
                    } else {
                        // 204 No Content
                        return emptyResponse(204);
                    }
                }
            };
            return client.execute(request, handler);
        } catch (IllegalArgumentException e) {
            String message = e.getMessage();
            if (message != null && message.contains("URI with undefined scheme")) {
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

    private static class Response {
        private final long timeFetched;
        private final Map<String, Object> response;

        private Response(long timeFetched, Map<String, Object> response) {
            this.timeFetched = timeFetched;
            this.response = response;
        }
    }

}
