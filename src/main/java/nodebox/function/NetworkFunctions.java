package nodebox.function;

import com.google.common.collect.ImmutableMap;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class NetworkFunctions {

    public static final Map<String, Response> responseCache = new HashMap<String, Response>();
    public static final FunctionLibrary LIBRARY;

    static {
        LIBRARY = JavaLibrary.ofClass("network", NetworkFunctions.class,
                "httpGet");
    }

    public static Map<String, Object> httpGet(final String url, final long timeoutSeconds) {
        synchronized (url) {
            if (responseCache.containsKey(url)) {
                Response r = responseCache.get(url);
                long timeNow = nowSeconds();
                long timeFetched = r.timeFetched;
                if ((timeNow - timeFetched) <= timeoutSeconds) {
                    return r.response;
                }
            }
            Map<String, Object> r = _httpGet(url);
            Response res = new Response(nowSeconds(), r);
            responseCache.put(url, res);
            return r;
        }
    }

    private static Map<String, Object> _httpGet(String url) {
        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet(url);
        try {
            HttpResponse response = client.execute(request);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                String text = EntityUtils.toString(entity);
                ImmutableMap.Builder<String, String> b = ImmutableMap.builder();
                for (Header h : response.getAllHeaders()) {
                    b.put(h.getName(), h.getValue());
                }

                Map<String, String> headers = b.build();
                return ImmutableMap.of(
                        "text", text,
                        "statusCode", response.getStatusLine().getStatusCode(),
                        "headers", headers);
            } else {
                // 204 No Content
                return emptyResponse(204);
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
                "text", "",
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
