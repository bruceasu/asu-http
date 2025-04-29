import me.asu.http.Application;
import me.asu.http.FileContextHandler;

import java.io.IOException;

public class TestServer extends Application  {
    public TestServer() throws IOException {
        super(8000);
        addRoute("/static/{*}",
                new FileContextHandler("D:\\03_projects\\suk\\asu-http\\src\\test\\static"));
        addRoute("/test", (req, rsp) -> {
            rsp.send(200, "This is a test");
            return 0;
        });
    }

    public static void main(String[] args) throws IOException {
        TestServer testServer = new TestServer();
        testServer.run();
    }


}
