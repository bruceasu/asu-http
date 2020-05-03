package me.asu.http.handler;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.slf4j.LoggerFactory.getLogger;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.Map;
import me.asu.http.Action;
import me.asu.http.Actions;
import me.asu.http.AppConfig;
import me.asu.http.Application;
import me.asu.http.request.HttpRequestContext;
import me.asu.http.request.MethodConstants;
import me.asu.http.request.Request;
import me.asu.http.response.Response;
import me.asu.http.response.ResponseStatus;
import org.slf4j.Logger;
import xyz.calvinwilliams.okjson.OKJSON;

/**
 * @author Suk
 * @since 2018/10/25
 */
public class RouteHttpHandler extends BaseHttpHandler
        implements HttpHandler, Closeable, Serializable
{

    private static final long   serialVersionUID = 7052998575538369252L;
    private static       Logger log              = getLogger(RouteHttpHandler.class);
    private static final int SINGLE_CHUNK_SIZE = 4096;
    public RouteHttpHandler(Application app, AppConfig config)
    {
        setApp(app);
        setAppCfg(config);
        run();
    }


    @Override
    public void handle(final HttpExchange exchange)
    {
        if (this.state != HandlerState.STARTED) {
            throw new IllegalStateException("handler 还没有启动。");
        }
        Object response = null;
        try {
            HttpRequestContext ctx = HttpRequestContext.get();
            Request request = ctx.getRequest();
            URI requestURI = request.getRequestURI();
            String path = requestURI.getPath();
            Action action = Actions.getInstance().getAction(path);
            if (action == null) {
                response = ResponseStatus.Response404;
            } else {
                String method = request.getMethod();
                switch (method.toUpperCase()) {
                case MethodConstants.GET:
                    response = action.get(request);
                    break;
                case MethodConstants.PUT:
                    response = action.put(request);
                    break;
                case MethodConstants.PATCH:
                    response = action.patch(request);
                    break;
                case MethodConstants.POST:
                    response = action.post(request);
                    break;
                case MethodConstants.DELETE:
                    response = action.delete(request);
                    break;
                default:
                    response = ResponseStatus.Response405;
                }
            }
            if (!(response instanceof Response)) {
                Response resp = new Response();
                if (response instanceof String) {
                    resp.setCode(200);
                    resp.setBody((String)response);
                } else if (response instanceof byte[]) {
                    resp.setCode(200);
                    resp.setBody((byte[])response);
                } else {
                    String s = OKJSON.objectToString(response);
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

    }


}
