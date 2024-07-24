package me.asu.http;


import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.asu.http.filter.ContextFilter;
import me.asu.http.filter.CorsFilter;
import me.asu.http.handler.Route;
import me.asu.http.handler.RouteHttpHandler;
import me.asu.http.handler.StaticHttpHandler;
import me.asu.http.handler.action.Action;
import me.asu.http.util.NamedThreadFactory;
import me.asu.http.util.Strings;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author suk
 * @since 2018/11/16
 */
@Getter
@Slf4j
public class Application {

    private transient HttpServer httpServer;
    private transient ThreadPoolExecutor executor;
    private transient Map<String, HttpContext> contextMap = new HashMap<>();
    private transient List<HttpHandler> handlers = new ArrayList<>();
    private List<String> staticPaths = new ArrayList<>();
    // support templates
    // private Path templatePath = Paths.get("templates");
    private boolean debug = false;
    private AppConfig config = new AppConfig();
    private RouteHttpHandler routeHttpHandler;
    private StaticHttpHandler staticHttpHandler;

    public Application() throws IOException {
        createExecutor();
        createHttpServer();
        addStaticHandler();
        addRouteHandler();
    }

    private void addRouteHandler() {
        routeHttpHandler = new RouteHttpHandler(this, config);
        String path = "/";
        HttpContext context = registerHandlerWithContextPath(path, routeHttpHandler);
        routeHttpHandler.setCtx(context);
    }

    private void addStaticHandler() {
        StaticHttpHandler staticHttpHandler = new StaticHttpHandler(this, config);
        String path = staticHttpHandler.getContextPath();
        HttpContext context = registerHandlerWithContextPath(path, staticHttpHandler);
        staticHttpHandler.setCtx(context);
    }

    private void createExecutor() {
        executor = new ThreadPoolExecutor(config.getThreads(), config.getThreads(), 0L,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
                new NamedThreadFactory("Http-Worker-"));
    }

    private void createHttpServer() throws IOException {
        InetSocketAddress address = new InetSocketAddress(config.getHost(), config.getPort());
        httpServer = HttpServer.create(address, 0);
        httpServer.setExecutor(executor);
    }

    public Route createRoute(String uri) {
        return routeHttpHandler.getRoute(uri);
    }

    public Route createRoute(String uri, Action action) {
        return routeHttpHandler.getRoute(uri).defaultMethod(action);
    }



    public HttpContext registerHandlerWithContextPath(String path, HttpHandler httpHandler) {
        HttpContext context = getHttpServer().createContext(path, httpHandler);
        getContextMap().put(path, context);
        getHandlers().add(httpHandler);

        ContextFilter cf = new ContextFilter();
        context.getFilters().add(cf);
        CorsFilter filter = new CorsFilter(config);
        context.getFilters().add(filter);
        return context;
    }

    /**
     * 启动服务
     *
     * @return 端口
     */
    public int run(AppConfig config) throws IOException {
        if (config != null) {
            this.config = config;
            if (Strings.isEmpty(config.getHost())) {
                config.setHost(AppConfig.DEFAULT_HOST);
            }
            if (config.getPort() < 0 && config.getPort() > 65535) {
                config.setPort(AppConfig.DEFAULT_PORT);
            }

            if (config.getThreads() < 1) {
                config.setThreads(AppConfig.DEFAULT_THREADS);
            }
        }
        return run();
    }

    /**
     * 启动服务
     *
     * @return 端口
     */
    public int run() throws IOException {
        httpServer.start();
        InetSocketAddress address = httpServer.getAddress();
        int port = address.getPort();

        log.info("Server is start at: {} ", port);
        return port;
    }

    /**
     * 停止服务器
     */
    public void shutdown() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        if (httpServer != null) {
            httpServer.stop(1);
            httpServer = null;
        }

        for (HttpHandler httpHandler : handlers) {
            if (httpHandler instanceof Closeable) {
                try {
                    ((Closeable) httpHandler).close();
                } catch (IOException e) {
                    // ignore.
                }
            }
        }

        log.info("Server is shutdown.");
    }


    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        shutdown();
    }


}
