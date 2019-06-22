package me.asu.http.server;

import static org.slf4j.LoggerFactory.getLogger;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
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
import me.asu.http.common.HandlerState;
import me.asu.http.util.MimeTypeDetector;
import me.asu.util.NamedThreadFactory;
import me.asu.util.Strings;
import me.asu.util.io.Streams;
import org.slf4j.Logger;

/**
 * @author victor.
 * @since 2018/10/25
 */
public class AsuStaticHttpHandler extends AsuBaseHttpHandler {

    private static final long serialVersionUID = 3817147817164555854L;
    public static final int DEAFULT_THREADS = Math
            .max(16, 8 * Runtime.getRuntime().availableProcessors());
    private static Logger log = getLogger(AsuStaticHttpHandler.class);
    private String contextPath = "/static";
    String resourcePath;

    private transient ThreadPoolExecutor executor = new ThreadPoolExecutor(DEAFULT_THREADS,
            DEAFULT_THREADS, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
            new NamedThreadFactory("Http-Worker-"));
    ;

    public AsuStaticHttpHandler(AsuHttpServer httpServer, HttpServerConfig config) {
        setHttpServer(httpServer);
        setConfig(config);
        HttpContext context = httpServer.registerHandlerWithContextPath(contextPath, this);
        setContext(context);
        String startDir = System.getProperty("user.dir");
        resourcePath = System.getProperty("static.dir", startDir + File.separator + "static");
        run();
    }


    @Override
    public void handle(final HttpExchange exchange) {
        if (this.state != HandlerState.STARTED) {
            throw new IllegalStateException("handler 还没有启动。");
        }
        int statusCode = HttpURLConnection.HTTP_OK;
        try {
            URI requestURI = exchange.getRequestURI();
            String path = requestURI.getPath();
            Path p = Paths.get(resourcePath, path.substring(contextPath.length()));
            if (Files.exists(p)) {
                // 使用独立的线程处理。
                executor.execute(() -> {
                    String mime = MimeTypeDetector.detect(path);
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
                            File file = p.toFile();
                            InputStream inputStream = Streams.fileIn(file);
                            Headers headers = exchange.getResponseHeaders();
                            headers.set("Content-Type", mime);
                            exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
                            OutputStream response = exchange.getResponseBody();
                            List<String> strings = requestHeaders.get("Accept-Encoding");
                            boolean acceptGzip = false;
                            if (strings != null && !strings.isEmpty()) {
                                acceptGzip = strings.contains("gzip") || strings.contains("Gzip")
                                        || strings.contains("GZIP");
                            }
                            if (acceptGzip && getConfig().isEnableGzip()
                                    && file.length() > getConfig().getMinLengthUsingGzip()
                                    && config.getGzipMime().contains(mime)) {
                                headers.set("Content-Encoding", "gzip");
                                response = new GZIPOutputStream(response);
                            }
                            Streams.writeAndClose(response, inputStream);
                        } else if ("head".equalsIgnoreCase(requestMethod)) {
                            exchange.sendResponseHeaders(HttpURLConnection.HTTP_NO_CONTENT, -1);
                        } else {
                            exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_REQUEST, 0);
                        }
                    } catch (IOException e) {
                        log.error(e.getMessage(), e);
                        try {
                            exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, 0);
                        } catch (IOException ex) {
                            log.error(e.getMessage(), e);
                        }
                    } finally {
                        exchange.close();
                    }
                });
                return;
            } else {
                statusCode = HttpURLConnection.HTTP_NOT_FOUND;
            }
            //
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            statusCode = HttpURLConnection.HTTP_INTERNAL_ERROR;
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

}
