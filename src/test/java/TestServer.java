import java.io.IOException;
import me.asu.http.Action;
import me.asu.http.Application;
import me.asu.http.request.Request;

public class TestServer extends Application
{

    public TestServer() throws IOException {
        super();
    }

    public static void main(String[] args) throws IOException {
        System.setProperty("static.dir", "D:\\03_projects\\suk\\asu-http\\src\\test\\static");

        TestServer testServer = new TestServer();
        testServer.addAction("/", new IndexAction());
        testServer.run();
    }

    static class IndexAction implements Action
    {

        @Override
        public Object get(Request req)
        {
            return "This is a test";
        }

        @Override
        public Object attachment(Object attach)
        {
            return null;
        }

        @Override
        public Object attachment()
        {
            return null;
        }
    }
}
