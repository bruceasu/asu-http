package me.asu.http.request;

import com.sun.net.httpserver.HttpExchange;
import lombok.Data;
@Data
public class HttpRequestContext {
    private static ThreadLocal<HttpRequestContext> contexts = new ThreadLocal<>();

    HttpExchange exchange;
    Request request;

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
