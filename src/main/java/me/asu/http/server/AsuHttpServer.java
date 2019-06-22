package me.asu.http.server;


import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.asu.util.NamedThreadFactory;
import me.asu.util.Strings;

/**
 * @author suk
 * @since 2018/11/16
 */
@Getter
@Slf4j
public class AsuHttpServer {

    /**
     * 线程数，由于不会是很频繁的调用，无需很多
     */

    private transient HttpServer httpServer;

    private transient ThreadPoolExecutor executor;
    private transient Map<String, HttpContext> contextMap = new HashMap<>();
    private transient List<HttpHandler>        handlers   = new ArrayList<>();
    private List<String> staticPaths = new ArrayList<>();
    private Path templatePath = Paths.get("templates");
    private boolean debug = false;
    private HttpServerConfig config = new HttpServerConfig();



    public AsuHttpServer(HttpServerConfig config) throws IOException {
        if (config != null) {
            this.config = config;
            if (Strings.isEmpty(config.getHost())) {
                config.setHost(HttpServerConfig.DEFAULT_HOST);
            }
            if (config.port < 0 && config.port > 65535) {
                config.setPort(HttpServerConfig.DEFAULT_PORT);
            }

            if (config.threads < 1) {
                config.setThreads(HttpServerConfig.DEAFULT_THREADS);
            }
        }
        init();

        //            executor.setCorePoolSize(config.getThreads());
        //            executor.setMaximumPoolSize(config.getThreads());
    }

    public AsuHttpServer() throws IOException {
        init();
    }

    private void init() throws IOException {
        this.httpServer = HttpServer.create(new InetSocketAddress(config.getHost(), config.getPort()), 0);
        executor = new ThreadPoolExecutor(config.getThreads(), config.getThreads(), 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(), new NamedThreadFactory("Http-Worker-"));
        httpServer.setExecutor(executor);

        new AsuStaticHttpHandler(this, config);
        new AsuHttpHandler(this, config);
    }


    public AsuHttpServer addHandler(HttpHandler httpHandler) {
        Objects.requireNonNull(httpHandler);
        this.getHandlers().add(httpHandler);
        return this;
    }

    public HttpContext registerHandlerWithContextPath(String path, HttpHandler httpHandler) {
        HttpContext context = getHttpServer().createContext(path, httpHandler);
        getContextMap().put(path, context);
        addHandler(httpHandler);
        CorsFilter filter = new CorsFilter(config);
        context.getFilters().add(filter);
        return context;
    }
    /**
     * 启动服务
     *
     * @return 端口
     */
    public int run() {
        httpServer.start();
        InetSocketAddress address = httpServer.getAddress();
        int port = address.getPort();

        log.info("Server is start at: {}。", port);
        return port;
    }

    /**
     * 停止服务器
     */
    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
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
