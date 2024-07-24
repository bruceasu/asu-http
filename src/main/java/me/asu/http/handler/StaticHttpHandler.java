package me.asu.http.handler;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import me.asu.http.AppConfig;
import me.asu.http.AppConfig.GzipConfig;
import me.asu.http.Application;
import me.asu.http.request.MethodConstants;
import me.asu.http.util.MimeTypeDetector;
import me.asu.http.util.NamedThreadFactory;
import me.asu.http.util.Streams;
import me.asu.http.util.Strings;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author Suk
 * @since 2018/10/25
 */
public class StaticHttpHandler extends BaseHttpHandler {

    public static final int DEFAULT_THREADS = Math.max(16, 8 * Runtime.getRuntime().availableProcessors());
    private static final long serialVersionUID = 3817147817164555854L;
    private static Logger log = getLogger(StaticHttpHandler.class);
    private String contextPath = "/static";
    private String resourcePath;

    private transient ThreadPoolExecutor executor = new ThreadPoolExecutor(DEFAULT_THREADS,
            DEFAULT_THREADS, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
            new NamedThreadFactory("Http-Worker-"));

    public StaticHttpHandler(Application app, AppConfig config) {
        setApp(app);
        setAppCfg(config);
        if (Strings.isNotEmpty(config.getStaticContextPath())) {
            contextPath = config.getStaticContextPath();
        }
        if (Strings.isNotEmpty(config.getStaticPath())) {
            resourcePath = config.getStaticPath();
        } else {
            String startDir = System.getProperty("user.dir");
            resourcePath = System.getProperty("static.dir", startDir + File.separator + "static");
        }

        run();
    }


    @Override
    public void handle(final HttpExchange exchange) {
        if (this.state != HandlerState.STARTED) {
            throw new IllegalStateException("handler 还没有启动。");
        }
        try {
            URI requestURI = exchange.getRequestURI();
            String path = requestURI.getPath();
            String resource = path.substring(contextPath.length());
            Path p = Paths.get(resourcePath, resource);
            if (Files.exists(p)) {
                // 使用独立的线程处理。
                executor.execute(new SendFile(exchange, p));
            } else {
                notFound404(exchange);
                exchange.close();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            try {
                error500(exchange);
            } catch (IOException e1) {
                log.error("send error message occurred error.", e1);
            } finally {
                exchange.close();
            }
        } finally {

        }


    }

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
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

    class SendFile implements Runnable {

        private final HttpExchange exchange;
        private final Path path;

        SendFile(final HttpExchange exchange, final Path path) {
            this.exchange = exchange;
            this.path = path;
        }

        @Override
        public void run() {
            if (!Files.isRegularFile(path)) {
                try {
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, -1);
                } catch (IOException e) {
                    log.error("", e);
                }
                return;
            }
            String mime = MimeTypeDetector.detect(path.toString());
            if (Strings.isEmpty(mime)) {
                mime = "application/octet-stream";
            }
            try {
                String requestMethod = exchange.getRequestMethod();
                Headers requestHeaders = exchange.getRequestHeaders();
                InputStream requestBody = exchange.getRequestBody();
                int b;
                while ((b = requestBody.read()) != -1) {
                    // ignore the input.
                    ;
                }

                if (MethodConstants.GET.equalsIgnoreCase(requestMethod)) {
                    File file = path.toFile();

                    List<String> ifModifiedSince = requestHeaders
                            .get("If-Modified-Since");
                    String lastModified = file.lastModified() + "";
                    if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
                        if (lastModified.equals(ifModifiedSince.get(0))) {
                            StaticHttpHandler.this.notModified304(exchange);
                            return;
                        }

                    }
                    // response
                    InputStream inputStream = Streams.fileIn(file);
                    Headers headers = exchange.getResponseHeaders();
                    headers.set("Content-Type", mime);
                    headers.set("Last-Modified", lastModified);
                    StaticHttpHandler.this.ok200(exchange);
                    OutputStream response = exchange.getResponseBody();
                    List<String> strings = requestHeaders.get("Accept-Encoding");
                    boolean acceptGzip = false;
                    if (strings != null && !strings.isEmpty()) {
                        acceptGzip =
                                strings.contains("gzip") || strings.contains("Gzip")
                                        || strings.contains("GZIP");
                    }
                    GzipConfig gzipConfig = StaticHttpHandler.this.getAppCfg()
                            .getGzipConfig();
                    if (acceptGzip && StaticHttpHandler.this.getAppCfg()
                            .isEnableGzip()
                            && file.length() > gzipConfig.getMinLengthUsingGzip()
                            && gzipConfig.getGzipMime().contains(mime)) {
                        headers.set("Content-Encoding", "gzip");
                        response = new GZIPOutputStream(response);
                    }
                    Streams.writeAndClose(response, inputStream);
                } else if ("head".equalsIgnoreCase(requestMethod)) {
                    StaticHttpHandler.this.noContent204(exchange);
                } else {
                    StaticHttpHandler.this.badRequest400(exchange);
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                try {
                    StaticHttpHandler.this.error500(exchange);
                } catch (IOException ex) {
                    log.error(e.getMessage(), e);
                }
            } finally {
                exchange.close();
            }
        }
    }

}
