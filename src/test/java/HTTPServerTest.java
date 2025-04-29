import me.asu.http.*;

import javax.net.ssl.SSLServerSocketFactory;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import static me.asu.http.HeaderKey.CONTENT_TYPE;
import static me.asu.http.Strings.parseULong;

public class HTTPServerTest {
    /**
     * 启动一个独立的HTTP服务器，从磁盘提供文件服务。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        int port = 8080;
        String d = System.getProperty("user.dir");

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-p")) {
                if (i + 1 < args.length){
                    port =  (int) parseULong(args[++i], 10);
                } else {
                    usage();
                    return;
                }
            } else if (args[i].equals("-d")) {
                if (i + 1 < args.length){
                    d =  args[++i];
                } else {
                    usage();
                    return;
                }
            } else {
                usage();
                return;
            }
        }

        try {
            File dir = new File(d);
            if (!dir.canRead())
                throw new FileNotFoundException(dir.getAbsolutePath());
            // set up server
            HTTPServer server = new HTTPServer(port);
            if (System.getProperty("javax.net.ssl.keyStore") != null) // enable SSL if configured
                server.setServerSocketFactory(SSLServerSocketFactory.getDefault());
            server.addContext("/{*}", new FileContextHandler(dir));
            server.addContext("/api/time", new ContextHandler() {
                @Override
                public int serve(Request req, Response resp) throws IOException {
                    long now = System.currentTimeMillis();
                    resp.getHeaders().add(CONTENT_TYPE, "text/plain");
                    resp.send(200, String.format("%tF %<tT", now));
                    return 0;
                }
            });
            server.start();
            System.out.println("HTTPServer is listening on port " + port);
        } catch (Exception e) {
            System.err.println("error: " + e);
        }
    }

    private static void usage() {
        System.err.printf("Usage: java [-options] %s  [port] [directory] %n" +
                "To enable SSL: specify options -Djavax.net.ssl.keyStore, " +
                "-Djavax.net.ssl.keyStorePassword, etc.%n", HTTPServer.class.getName());
    }

}
