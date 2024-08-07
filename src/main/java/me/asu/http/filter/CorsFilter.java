package me.asu.http.filter;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import me.asu.http.AppConfig;
import me.asu.http.AppConfig.CorsConfig;
import me.asu.http.util.Strings;

import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * @author suk
 * @since 2018/11/22
 */
public class CorsFilter extends Filter {

    private final AppConfig config;

    public CorsFilter(AppConfig config) {
        this.config = config;
    }

    @Override
    public String description() {
        return "Cors support";
    }

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        if (config.isEnableCors()) {
            Headers headers = exchange.getResponseHeaders();
            CorsConfig corsConfig = config.getCorsConfig();
            if (corsConfig.getAccessControlAllowCredentials() != null) {
                headers.set(" Access-Control-Allow-Credentials",
                        corsConfig.getAccessControlAllowCredentials().toString());
            }
            if (Strings.isNotEmpty(corsConfig.getAccessControlAllowOrigin())) {
                headers.set("Access-Control-Allow-Origin",
                        corsConfig.getAccessControlAllowOrigin());
            } else {
                headers.set("Access-Control-Allow-Origin", "*");
            }
            if (Strings.isNotEmpty(corsConfig.getAccessControlAllowMethods())) {
                headers.set("Access-Control-Allow-Methods",
                        corsConfig.getAccessControlAllowMethods());
            } else {
                headers.set("Access-Control-Allow-Methods",
                        "OPTIONS, GET, POST, PUT, PATCH, DELETE, HEAD");
            }
            if (Strings.isNotEmpty(corsConfig.getAccessControlExposeHeaders())) {
                headers.set("Access-Control-Expose-Headers",
                        corsConfig.getAccessControlExposeHeaders());
            }
            if (corsConfig.getAccessControlMaxAge() != null) {
                headers.set("Access-Control-Max-Age",
                        corsConfig.getAccessControlMaxAge().toString());
            }
        }
        String requestMethod = exchange.getRequestMethod();
        if ("options".equalsIgnoreCase(requestMethod)) {
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_NO_CONTENT, -1);
            return;
        }
        chain.doFilter(exchange);
    }
}

