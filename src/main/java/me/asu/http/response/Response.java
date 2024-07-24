package me.asu.http.response;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;


/**
 * An Response is used to set output values, and to write those values
 * to the client.
 *
 * @author suk
 */
public class Response {
    // default to "200 - OK"
    private int code = 200;
    private byte[] body;
    private String mimeType = "text/html";

    private Map<String, String> headers = new HashMap<>();


    /**
     * Send a simple string message with an HTTP response code back to
     * the client. <p>
     * <p>
     * Can be used for sending all data back.
     *
     * @param code    An HTTP response code.
     * @param message The content of the server's response to the browser
     */
    public void message(int code, String message) {
        setCode(code);
        setBody(message);
        setMimeType("text/plain");
    }

    /*********************
     GETTERS AND SETTERS
     *********************/

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body.getBytes(StandardCharsets.UTF_8);
    }

    public void setBody(byte[] bytes) {
        body = bytes;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }


    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public void setHeader(String key, String value) {
        this.headers.put(key, value);
    }


}
