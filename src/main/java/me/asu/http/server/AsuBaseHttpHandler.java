package me.asu.http.server;

import static org.slf4j.LoggerFactory.getLogger;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import me.asu.http.common.HandlerState;
import org.slf4j.Logger;

/**
 * @author victor.
 * @since 2018/10/25
 */
public abstract class AsuBaseHttpHandler implements HttpHandler, Closeable, Serializable {

    private static final long serialVersionUID = 2920606245741371534L;
    private static Logger log = getLogger(AsuBaseHttpHandler.class);

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
