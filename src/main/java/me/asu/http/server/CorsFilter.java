package me.asu.http.server;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.*;
import me.asu.util.Strings;

/**
 * @author suk
 * @since 2018/11/22
 */
public class CorsFilter extends Filter {

    private final HttpServerConfig config;

    public CorsFilter(HttpServerConfig config) {
        this.config = config;
    }
    @Override
    public String description() {
        return "Cors support";
    }

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        String requestMethod = exchange.getRequestMethod();
        if ("options".equalsIgnoreCase(requestMethod)) {
            if (config.isEnableCors()) {
                Headers headers = exchange.getResponseHeaders();
                if (config.getCorsAccessControlAllowCredentials() != null) {
                    headers.set(" Access-Control-Allow-Credentials", config.getCorsAccessControlAllowCredentials().toString());
                }
                if (Strings.isNotEmpty(config.getCorsAccessControlAllowOrigin())) {
                    headers.set("Access-Control-Allow-Origin", config.getCorsAccessControlAllowOrigin());
                } else {
                    headers.set("Access-Control-Allow-Origin", "*");
                }
                if (Strings.isNotEmpty(config.getCorsAccessControlAllowMethods())) {
                    headers.set("Access-Control-Allow-Methods", config.getCorsAccessControlAllowMethods());
                } else {
                    headers.set("Access-Control-Allow-Methods", "OPTIONS, GET, POST, PUT, PATCH, DELETE, HEAD");
                }
                if (Strings.isNotEmpty(config.getCorsAccessControlExposeHeaders())) {
                    headers.set("Access-Control-Expose-Headers", config.getCorsAccessControlExposeHeaders());
                }
                if (config.getCorsAccessControlMaxAge() != null) {
                    headers.set("Access-Control-Max-Age", config.getCorsAccessControlMaxAge().toString());
                }
            }

            exchange.sendResponseHeaders(HttpURLConnection.HTTP_NO_CONTENT, -1);
            return;
        }
        chain.doFilter(exchange);
    }
}

