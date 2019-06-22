import java.io.IOException;
import me.asu.http.server.AsuHttpHandler;
import me.asu.http.server.AsuHttpServer;

public class TestServer extends AsuHttpServer {

    public TestServer() throws IOException {
        super();
    }

    public static void main(String[] args) throws IOException {
        System.setProperty("static.dir", "D:\\03_projects\\suk\\asu-http\\src\\test\\static");
        new TestServer().run();
    }
}
