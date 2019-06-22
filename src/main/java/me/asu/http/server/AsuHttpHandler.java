package me.asu.http.server;

import static org.slf4j.LoggerFactory.getLogger;

import com.sun.net.httpserver.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import me.asu.http.common.HandlerState;
import me.asu.http.server.ParameterFilter.ParamMap;
import org.slf4j.Logger;

/**
 * @author victor.
 * @since 2018/10/25
 */
public class AsuHttpHandler implements HttpHandler, Closeable, Serializable {

    private static final long serialVersionUID = 7052998575538369252L;
    private static Logger log = getLogger(AsuHttpHandler.class);

    private final AsuHttpServer httpServer;
    private final ParameterFilter filter = new ParameterFilter();
    /**
     * 0: ready, 1: started, 2: shutting down, 3: shutdown
     */
    private transient HandlerState state = HandlerState.READY;
    private List<Runnable> shutdownHooks = new ArrayList<>();
    private HttpContext context;

    public AsuHttpHandler(AsuHttpServer httpServer, HttpServerConfig config) {
        this.httpServer = httpServer;

        context = this.httpServer.getHttpServer()
                                             .createContext("/", this);
        this.httpServer.getContextMap().put("/", context);

        context.getFilters().add(filter);


        this.httpServer.getHandlers().add(this);
        run();
    }


    @Override
    public void handle(final HttpExchange exchange) {
        if (this.state != HandlerState.STARTED) {
            throw new IllegalStateException("handler 还没有启动。");
        }
        ParamMap map = (ParamMap) exchange.getAttribute("parameters");
        int statusCode = 200;
        try {
            String brokerId = map.getParameter("brokerId");
            String pid = map.getParameter("pid");
            String webPort = map.getParameter("webPort");
            log.debug("<<< 请求：brokerId:{}, pid: {}, webPort: {}", brokerId, pid, webPort);

            // exchange.
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            statusCode = 500;
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
            exchange.sendResponseHeaders(statusCode, 0);
        } catch (IOException e1) {
            log.error("send error message occurred error.", e1);
        } finally {
            exchange.close();
        }

    }

    public AsuHttpHandler addShutdownHook(Runnable runnable) {
        this.shutdownHooks.add(runnable);
        return this;
    }

    public AsuHttpHandler removeShutdownHook(Runnable runnable) {
        this.shutdownHooks.remove(runnable);
        return this;
    }

    public AsuHttpHandler addFilter(Filter filter) {
        context.getFilters().add(filter);
        return this;
    }

    /**
     * 启动
     */
    private void run() {
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
                this.shutdownHooks.forEach(r->{
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
}
