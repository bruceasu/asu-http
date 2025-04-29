package me.asu.http;


import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static me.asu.http.Strings.isEmpty;

/**
 * @author suk
 * @since 2018/11/16
 */
@Getter
@Slf4j
public class Application {
    private transient HTTPServer               httpServer;
    private transient ThreadPoolExecutor       executor;
    private           AppConfig                config      = new AppConfig();

    public Application() throws IOException {
        createHttpServer(config.getPort());
        addDefaultHandlers();
    }
    public Application(int port) throws IOException {
        createHttpServer(port);
        addDefaultHandlers();
    }

    public void addRoute(String path, ContextHandler handler, String... methods) {
        ensureServerCreated();
        Objects.requireNonNull(path);
        Objects.requireNonNull(handler);
        httpServer.addContext(path, handler, methods);
    }

    public void addRoutes(Object route) {
        Objects.requireNonNull(route);
        httpServer.addContexts(route);
    }

    void ensureServerCreated() {
        Objects.requireNonNull(httpServer, "httpServer is null");
    }

    public void setServerSocketFactory(ServerSocketFactory serverSocketFactory) {
        ensureServerCreated();
        httpServer.setServerSocketFactory(serverSocketFactory);
    }

    void addDefaultHandlers() {
        addRoute("/{*}", new DeathHandler());
    }

    void createExecutor() {
        executor = new ThreadPoolExecutor(config.getThreads(), config.getThreads(), 0L,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
                new NamedThreadFactory("Http-Worker-"));
    }

    void createHttpServer(int port) {
        httpServer = new HTTPServer(port);
        if (System.getProperty("javax.net.ssl.keyStore") != null) // enable SSL if configured
            httpServer.setServerSocketFactory(SSLServerSocketFactory.getDefault());
    }

    /**
     * 启动服务
     *
     * @return 端口
     */
    public int run(AppConfig config) throws IOException {
        if (config != null) {
            this.config = config;
            if (isEmpty(config.getHost())) {
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
        createExecutor();
        httpServer.setExecutor(executor);
        httpServer.start();
        log.info("Server is start at: {} ", config.getPort());
        return config.getPort();
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
            httpServer.stop();
            httpServer = null;
        }
        log.info("Server is shutdown.");
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        shutdown();
    }

    @Data
    public static class AppConfig {
        static final int                   DEFAULT_THREADS = Math.max(200, 32 * Runtime.getRuntime().availableProcessors());
        static final int                   DEFAULT_PORT    = 8000;
        static final String                DEFAULT_HOST    = "0.0.0.0";

        HTTPServer.GzipConfig gzipConfig   = new HTTPServer.GzipConfig();
        HTTPServer.CorsConfig corsConfig   = new HTTPServer.CorsConfig();
        int                   port         = DEFAULT_PORT;
        String                host         = DEFAULT_HOST;
        int                   threads      = DEFAULT_THREADS;
        Charset               bodyEncoding = StandardCharsets.UTF_8;
        Charset               uriEncoding  = StandardCharsets.UTF_8;
        boolean               enableGzip   = false;
        boolean               enableCors   = false;
    }
}
