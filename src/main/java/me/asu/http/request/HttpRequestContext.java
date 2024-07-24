package me.asu.http.request;

import com.sun.net.httpserver.HttpExchange;
import lombok.Data;
import me.asu.http.common.Pair;

import java.util.List;

@Data
public class HttpRequestContext {
    private static ThreadLocal<HttpRequestContext> contexts = new ThreadLocal<>();

    HttpExchange exchange;
    Request request;
    List<Pair<String, String>> attachments;

    public static HttpRequestContext get() {
        return contexts.get();

    }

    public static void set(HttpRequestContext ctx) {
        contexts.set(ctx);

    }

    public static void remove() {
        contexts.remove();

    }

}
