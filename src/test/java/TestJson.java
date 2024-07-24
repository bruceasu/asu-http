import com.fasterxml.jackson.core.JsonProcessingException;
import me.asu.http.util.JsonUtil;

public class TestJson {

    public static void main(String[] args) throws JsonProcessingException {
        AppResult<Boolean> ar = new AppResult();
        ar.result = false;

        String stringify = JsonUtil.stringify(ar);
        System.out.println("stringify = " + stringify);
    }
}
