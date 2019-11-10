package me.asu.http.server.handler;

import static org.slf4j.LoggerFactory.getLogger;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;
import me.asu.http.common.HandlerState;
import me.asu.http.server.AsuHttpServer;
import me.asu.http.server.HttpServerConfig;
import me.asu.http.server.HttpServerConfig.GzipConfig;
import me.asu.http.util.MimeTypeDetector;
import me.asu.http.util.NamedThreadFactory;
import me.asu.http.util.Streams;
import me.asu.http.util.Strings;
import org.slf4j.Logger;

/**
 * @author victor.
 * @since 2018/10/25
 */
public class AsuStaticHttpHandler extends AsuBaseHttpHandler {

    private static final long serialVersionUID = 3817147817164555854L;
    public static final int DEFAULT_THREADS = Math.max(16, 8 * Runtime.getRuntime().availableProcessors());
    private static Logger log = getLogger(AsuStaticHttpHandler.class);
    private String contextPath = "/static";
    private String resourcePath;

    private transient ThreadPoolExecutor executor = new ThreadPoolExecutor(DEFAULT_THREADS,
            DEFAULT_THREADS, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
            new NamedThreadFactory("Http-Worker-"));

    public AsuStaticHttpHandler(AsuHttpServer httpServer, HttpServerConfig config) {
        setHttpServer(httpServer);
        setConfig(config);
        if (Strings.isNotEmpty(config.getStaticContextPath())) {
            contextPath = config.getStaticContextPath();
        }
        HttpContext context = httpServer.registerHandlerWithContextPath(contextPath, this);
        setContext(context);

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

    class SendFile implements Runnable {

        private final HttpExchange exchange;
        private final Path path;

        SendFile(final HttpExchange exchange, final Path path) {
            this.exchange = exchange;
            this.path = path;
        }
        @Override
        public void run() {
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

                if ("get".equalsIgnoreCase(requestMethod)) {
                    File file = path.toFile();
                    List<String> ifModifiedSince = requestHeaders
                            .get("If-Modified-Since");
                    String lastModified = file.lastModified() + "";
                    if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
                        if (lastModified.equals(ifModifiedSince.get(0))) {
                            AsuStaticHttpHandler.this.notModified304(exchange);
                            return;
                        }

                    }
                    // response
                    InputStream inputStream = Streams.fileIn(file);
                    Headers headers = exchange.getResponseHeaders();
                    headers.set("Content-Type", mime);
                    headers.set("Last-Modified", lastModified);
                    AsuStaticHttpHandler.this.ok200(exchange);
                    OutputStream response = exchange.getResponseBody();
                    List<String> strings = requestHeaders.get("Accept-Encoding");
                    boolean acceptGzip = false;
                    if (strings != null && !strings.isEmpty()) {
                        acceptGzip =
                                strings.contains("gzip") || strings.contains("Gzip")
                                        || strings.contains("GZIP");
                    }
                    GzipConfig gzipConfig = AsuStaticHttpHandler.this.getConfig()
                                                                     .getGzipConfig();
                    if (acceptGzip && AsuStaticHttpHandler.this.getConfig()
                                                               .isEnableGzip()
                            && file.length() > gzipConfig.getMinLengthUsingGzip()
                            && gzipConfig.getGzipMime().contains(mime)) {
                        headers.set("Content-Encoding", "gzip");
                        response = new GZIPOutputStream(response);
                    }
                    Streams.writeAndClose(response, inputStream);
                } else if ("head".equalsIgnoreCase(requestMethod)) {
                    AsuStaticHttpHandler.this.noContent204(exchange);
                } else {
                    AsuStaticHttpHandler.this.badRequest400(exchange);
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                try {
                    AsuStaticHttpHandler.this.error500(exchange);
                } catch (IOException ex) {
                    log.error(e.getMessage(), e);
                }
            } finally {
                exchange.close();
            }
        }
    }



}
