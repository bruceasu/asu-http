package me.asu.http;

import lombok.Data;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Data
public class AppConfig {
    public static final int DEFAULT_THREADS = Math.max(200, 32 * Runtime.getRuntime().availableProcessors());
    public static final int DEFAULT_PORT = 8000;
    public static final String DEFAULT_HOST = "0.0.0.0";
    final GzipConfig gzipConfig = new GzipConfig();
    final CorsConfig corsConfig = new CorsConfig();
    int port = DEFAULT_PORT;
    String host = DEFAULT_HOST;
    int threads = DEFAULT_THREADS;
    Charset bodyEncoding = StandardCharsets.UTF_8;
    Charset uriEncoding = StandardCharsets.UTF_8;
    String staticPath;
    String staticContextPath = "/static";
    boolean enableGzip = false;
    boolean enableCors = false;

    @Data
    public class GzipConfig {
        final List<String> gzipMime = new ArrayList<>();
        long minLengthUsingGzip = 2048000;

        {
            gzipMime.add("text/css");
            gzipMime.add("text/html");
            gzipMime.add("text/htm");
            gzipMime.add("text/plain");
            gzipMime.add("text/js");
            gzipMime.add("text/javascript");
            gzipMime.add("application/x-javascript");
            gzipMime.add("application/x-json");
            gzipMime.add("application/json");
        }
    }

    @Data
    public class CorsConfig {
        /**
         * 该字段必填。它的值要么是请求时Origin字段的具体值，要么是一个*，表示接受任意域名的请求。
         */
        String accessControlAllowOrigin = "*";
        /**
         * 该字段必填。它的值是逗号分隔的一个具体的字符串或者*，表明服务器支持的所有跨域请求的方法。注意，返回的是所有支持的方法，而不单是浏览器请求的那个方法。这是为了避免多次"预检"请求。
         */
        String accessControlAllowMethods = "*";
        /**
         * 该字段可选。CORS请求时，XMLHttpRequest对象的getResponseHeader()方法只能拿到6个基本字段：Cache-Control、Content-Language、Content-Type、Expires、Last-Modified、Pragma。如果想拿到其他字段，就必须在Access-Control-Expose-Headers里面指定。
         */
        String accessControlExposeHeaders;
        /**
         * 该字段可选。它的值是一个布尔值，表示是否允许发送Cookie.默认情况下，不发生Cookie，即：false。对服务器有特殊要求的请求，比如请求方法是PUT或DELETE，或者Content-Type字段的类型是application/json，这个值只能设为true。如果服务器不要浏览器发送Cookie，删除该字段即可。
         */
        Boolean accessControlAllowCredentials;
        /**
         * 该字段可选，用来指定本次预检请求的有效期，单位为秒。在有效期间，不用发出另一条预检请求。
         */
        Long accessControlMaxAge;
    }


}