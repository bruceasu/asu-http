package me.asu.http.handler;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import me.asu.http.AppConfig;
import me.asu.http.Application;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author Suk
 * @since 2018/10/25
 */
public abstract class BaseHttpHandler implements HttpHandler, Closeable, Serializable {

    private static final long serialVersionUID = 2920606245741371534L;
    private static Logger log = getLogger(BaseHttpHandler.class);
    protected Application app;
    protected List<Runnable> shutdownHooks = new ArrayList<>();
    protected HttpContext ctx;
    protected AppConfig appCfg;
    /**
     * 0: ready, 1: started, 2: shutting down, 3: shutdown
     */
    transient HandlerState state = HandlerState.READY;

    public AppConfig getAppCfg() {
        return appCfg;
    }

    public void setAppCfg(AppConfig appCfg) {
        this.appCfg = appCfg;
    }

    public HttpContext getCtx() {
        return ctx;
    }

    public void setCtx(HttpContext ctx) {
        this.ctx = ctx;
    }

    public Application getApp() {
        return app;
    }

    public void setApp(Application app) {
        this.app = app;
    }

    public BaseHttpHandler addShutdownHook(Runnable runnable) {
        this.shutdownHooks.add(runnable);
        return this;
    }

    public BaseHttpHandler removeShutdownHook(Runnable runnable) {
        this.shutdownHooks.remove(runnable);
        return this;
    }

    public BaseHttpHandler addFilter(Filter filter) {
        ctx.getFilters().add(filter);
        return this;
    }

    /**
     * 启动
     */
    public void run() {
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


}
