package me.asu.http.filter;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import lombok.extern.slf4j.Slf4j;
import me.asu.http.request.HttpRequestContext;
import me.asu.http.request.Request;

import java.io.IOException;

/**
 * @author suk
 * @since 2018/11/22
 */
@Slf4j
public class ContextFilter extends Filter {

    @Override
    public String description() {
        return "Add Http Request Context";
    }

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        try {
            HttpRequestContext context = new HttpRequestContext();
            Request request = Request.createRequest(exchange);
            context.setRequest(request);
            HttpRequestContext.set(context);
            chain.doFilter(exchange);
        } catch (Exception e) {
            log.error("", e);
        } finally {
            HttpRequestContext.remove();
        }
    }
}

