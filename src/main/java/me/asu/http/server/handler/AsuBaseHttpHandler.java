package me.asu.http.server.handler;

import static org.slf4j.LoggerFactory.getLogger;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import me.asu.http.common.HandlerState;
import me.asu.http.server.AsuHttpServer;
import me.asu.http.server.HttpServerConfig;
import org.slf4j.Logger;

/**
 * @author victor.
 * @since 2018/10/25
 */
public abstract class AsuBaseHttpHandler implements HttpHandler, Closeable, Serializable {

    private static final long serialVersionUID = 2920606245741371534L;
    private static Logger log = getLogger(AsuBaseHttpHandler.class);
    protected static final String[] methods = {
            "GET", "POST", "HEAD", "OPTIONS", "PUT", "DELETE", "TRACE"
    };
    protected AsuHttpServer httpServer;

    /**
     * 0: ready, 1: started, 2: shutting down, 3: shutdown
     */
    protected transient HandlerState state = HandlerState.READY;
    protected List<Runnable> shutdownHooks = new ArrayList<>();
    protected HttpContext context;
    HttpServerConfig config;

    public HttpServerConfig getConfig() {
        return config;
    }

    public void setConfig(HttpServerConfig config) {
        this.config = config;
    }

    public HttpContext getContext() {
        return context;
    }

    public void setContext(HttpContext context) {
        this.context = context;
    }

    public AsuHttpServer getHttpServer() {
        return httpServer;
    }

    public void setHttpServer(AsuHttpServer httpServer) {
        this.httpServer = httpServer;
    }

    public AsuBaseHttpHandler addShutdownHook(Runnable runnable) {
        this.shutdownHooks.add(runnable);
        return this;
    }

    public AsuBaseHttpHandler removeShutdownHook(Runnable runnable) {
        this.shutdownHooks.remove(runnable);
        return this;
    }

    public AsuBaseHttpHandler addFilter(Filter filter) {
        context.getFilters().add(filter);
        return this;
    }

    /**
     * 启动
     */
    protected void run() {
        if (this.state == HandlerState.READY) {
            this.state = HandlerState.STARTED;
        } else {
            throw new IllegalStateException("handler 不能多次启动。");
        }
    }

    /**
     * 关闭
     */
    @Override
    public void close() throws IOException {
        if (this.state == HandlerState.READY || this.state == HandlerState.STARTED) {
            this.state = HandlerState.SHUTTING_DOWN;
            // cleaning
            if (!this.shutdownHooks.isEmpty()) {
                this.shutdownHooks.forEach(r -> {
                    try {
                        r.run();
                    } catch (Throwable e) {
                        log.error(e.getMessage(), e);
                    }
                });
            }
            this.state = HandlerState.SHUTDOWN;
        }
    }

    protected void ok200(HttpExchange exchange) throws IOException {
        // status code and content length, 0 will using chunk
        // 第二个参数，还有其他情况，
        // 例如，
        // 1. status code  [100,2 00) || 204 || 304 时，content length != -1 会写日志，并设置为-1
        // 2. 如果是头请求，content length >=  0 时，会写日志，最后统一设置为0
        // 3. 如果content length ==  0，此时，使用http1.0版本时，表示无限长度，否则使用 chunk
        // 4. content length == -1, 强制 置0
        // 5. content length > 0, 表示body entity 长度。
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
    }

    protected void notModified304(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_MODIFIED, -1);
    }

    protected void notFound404(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, -1);
    }

    protected void noContent204(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_NO_CONTENT, -1);
    }

    protected void badRequest400(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_REQUEST, 0);
    }

    protected void error500(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, 0);
    }
}
