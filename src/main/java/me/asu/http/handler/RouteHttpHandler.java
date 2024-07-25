package me.asu.http.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lombok.Getter;
import me.asu.http.AppConfig;
import me.asu.http.Application;
import me.asu.http.common.Pair;
import me.asu.http.request.HttpRequestContext;
import me.asu.http.request.Request;
import me.asu.http.response.Response;
import me.asu.http.response.ResponseStatus;
import me.asu.http.util.JsonUtil;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author Suk
 * @since 2018/10/25
 */
public class RouteHttpHandler extends BaseHttpHandler
        implements HttpHandler, Closeable, Serializable {


    private static final long serialVersionUID = 7052998575538369252L;
    private static final int SINGLE_CHUNK_SIZE = 4096;
    private static Logger log = getLogger(RouteHttpHandler.class);

    private HashMap<String, Route> routes = new HashMap<>();
    private HashMap<String, Route> wildCardRoutes = new HashMap<>();
    private ExecutorService es = Executors.newFixedThreadPool(500);

    {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            es.shutdown();
        }));
    }

    public RouteHttpHandler(Application app, AppConfig config) {

        setApp(app);
        setAppCfg(config);
        run();
    }

//        if (uri.matches(".*\\{.+\\}.*")) {
//        wildCardActions.put(uri, action);
//    } else {
//        actions.put(uri, action);
//    }

    @Override
    public void handle(final HttpExchange exchange) {
        if (this.state != HandlerState.STARTED) {
            throw new IllegalStateException("handler 还没有启动。");
        }
        CompletableFuture.runAsync(() -> {
            Object response = null;
            try {
                HttpRequestContext ctx = HttpRequestContext.get();
                Request request = ctx.getRequest();
                URI requestURI = request.getRequestURI();
                String path = requestURI.getPath();
                Route route = getRoute(path);
                if (route == null) {
                    response = ResponseStatus.Response404;
                } else {
                    String method = request.getMethod().toUpperCase();
                    response = route.execute(method, request);
                }
                if (!(response instanceof Response)) {
                    Response resp = new Response();
                    if (response instanceof String) {
                        resp.setCode(200);
                        resp.setBody((String) response);
                    } else if (response instanceof byte[]) {
                        resp.setCode(200);
                        resp.setBody((byte[]) response);
                    } else {
                        String s = JsonUtil.stringify(response);
                        resp.setCode(200);
                        resp.setBody(s);
                        resp.setMimeType("application/json; Charset=UTF-8");
                    }
                    response = resp;
                }
                //            exchange.
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                response = ResponseStatus.Response500;
            }
            try {
                // status code and content length, 0 will using chunk
                // 第二个参数，还有其他情况，
                // 例如，
                // 1. status code  [100,2 00) || 204 || 304 时，content length != -1 会写日志，并设置为-1
                // 2. 如果是头请求，content length >=  0 时，会写日志，最后统一设置为0
                // 3. 如果content length ==  0，此时，使用http1.0版本时，表示无限长度，否则使用 chunk
                // 4. content length == -1, 强制 置0
                // 5. content length > 0, 表示body entity 长度。
                Response resp = (Response) response;
                Map<String, String> headers = resp.getHeaders();
                if (headers != null) {
                    headers.forEach((k, v) -> {
                        exchange.getResponseHeaders().add(k, v);
                    });
                }
                int code = resp.getCode();
                if (code == HTTP_OK) {
                    byte[] body = resp.getBody();
                    if (body != null && body.length <= SINGLE_CHUNK_SIZE) {
                        exchange.sendResponseHeaders(code, body.length);
                    } else {
                        exchange.sendResponseHeaders(code, 0);
                    }
                    OutputStream responseBody = exchange.getResponseBody();

                    if (body != null && body.length > 0) {
                        responseBody.write(body);
                    }
                    responseBody.close();
                } else {
                    exchange.sendResponseHeaders(code, 0);
                }
            } catch (IOException e1) {
                log.error("send error message occurred error.", e1);
            } finally {
                exchange.close();
            }
        }, es);


    }

    public Route getRoute(String uri) {
        uri = Route.normalize(uri);
        Route route = routes.get(uri);
        if (route == null) {
            // try wildcard
            UriParser uriParser = new UriParser();
            for (Map.Entry<String, Route> entry : wildCardRoutes.entrySet()) {
                String pattern = entry.getKey();
                Route act = entry.getValue();
                uriParser.parse(uri, pattern);
                if (uriParser.isMatch()) {
                    List<Pair<String, String>> variables = Collections.unmodifiableList(uriParser.getList());
                    HttpRequestContext ctx = HttpRequestContext.get();
                    ctx.setAttachments(variables);
                    return act;
                }
            }
        }
        if (route == null) {
            route = new Route(uri);
            routes.put(uri, route);
        }
        return route;
    }


    @Getter
    static class UriParser {

        static Pattern PATH_VARIABLE_PATTERN = Pattern.compile("\\{(.+?)\\}");
        LinkedList<Pair<String, String>> list = new LinkedList<>();
        boolean match = false;

        void parse(final String subUri, final String pattern) {
            list.clear();
            match = false;

            Matcher matcher = PATH_VARIABLE_PATTERN.matcher(pattern);
            List<String> names = new ArrayList<>();
            StringBuffer buf = new StringBuffer("^");
            while (matcher.find()) {
                names.add(pattern.substring(matcher.start() + 1, matcher.end() - 1));
                matcher.appendReplacement(buf, "([^/]+?)");
            }
            matcher.appendTail(buf);
            buf.append("$");
            Pattern xPattern = Pattern.compile(buf.toString());
            Matcher matcher1 = xPattern.matcher(subUri);
            if (matcher1.matches()) {
                match = true;
                for (int i = 0; i < names.size(); i++) {
                    String group = matcher1.group(i + 1);
                    list.add(new Pair(names.get(i), group));
                }
            }
        }
    }

}
