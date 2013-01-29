package nodebox.function;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class NetworkFunctions {

    public static final Cache<String, String> pages = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build();
    public static final FunctionLibrary LIBRARY;

    static {
        LIBRARY = JavaLibrary.ofClass("network", NetworkFunctions.class,
                "httpGet");
    }

    public static String httpGet(final String url) {
        synchronized (url) {
            try {
                return pages.get(url, new Callable<String>() {
                    @Override
                    public String call() throws Exception {
                        return _httpGet(url);
                    }
                });
            } catch (ExecutionException e) {
                return "";
            }
        }
    }

    private static String _httpGet(String url) {
        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet(url);
        try {
            HttpResponse response = client.execute(request);
            return EntityUtils.toString(response.getEntity());
        } catch (IOException e) {
            return "";
        }
    }

}
