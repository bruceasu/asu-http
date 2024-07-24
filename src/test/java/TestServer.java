import me.asu.http.handler.action.Action;
import me.asu.http.Application;
import me.asu.http.request.Request;

import java.io.IOException;

public class TestServer extends Application {

    public TestServer() throws IOException {
        super();
    }

    public static void main(String[] args) throws IOException {
        System.setProperty("static.dir", "D:\\03_projects\\suk\\asu-http\\src\\test\\static");

        TestServer testServer = new TestServer();
        testServer.createRoute("/", new IndexAction());
        testServer.run();
    }

    static class IndexAction implements Action {

        @Override
        public Object execute(Request req) {
            return "This is a test";
        }

    }
}
