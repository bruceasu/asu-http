package me.asu.http.server.filter;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import me.asu.http.server.HttpRequestContext;
import me.asu.http.server.HttpRequestContext;
import me.asu.http.server.Request;

/**
 * @author suk
 * @since 2018/11/22
 */
public class ContextFilter extends Filter {

    @Override
    public String description() {
        return "Add Http Request Context";
    }

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        try {
            HttpRequestContext context = new HttpRequestContext();
            Request request = Request.createReauest(exchange);
            context.setRequest(request);
            HttpRequestContext.set(context);
            chain.doFilter(exchange);
        } finally {
            HttpRequestContext.remove();
        }
    }
}

