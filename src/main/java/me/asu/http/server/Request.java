package me.asu.http.server;

import com.sun.net.httpserver.HttpExchange;
import java.net.URI;
import java.util.List;
import java.util.Map;
import me.asu.http.common.CommonContentType;
import me.asu.http.common.HeaderKey;
import me.asu.lang.map.MultiValueMap;

public interface Request {

    public final static String GET = "GET";
    public final static String POST = "POST";

    String getParameter(String param);

    ParamMap getParamMap();

    MultiValueMap<String, String> getHeadMap();

    Map<String, Object> getDataMap();

    String getMethod();

    URI getReuestURI();

    String getRequestBody();

    void initBody();

    HttpExchange getHttpExchange();

    public static boolean isForm(String contentType) {
        return (contentType.startsWith(CommonContentType.FORM.type()));
    }

    public static boolean isMultipartFormData(String contentType) {
        return (contentType.startsWith(CommonContentType.FORM_DATA.type()));
    }

    public static boolean isJson(String contentType) {
        return (contentType.startsWith(CommonContentType.JSON.type()));
    }

    public static boolean isXml(String contentType) {
        return (contentType.startsWith(CommonContentType.XML.type()));
    }

    public static Request createReauest(HttpExchange httpExchange) {
        List<String> strings = httpExchange.getRequestHeaders().get(HeaderKey.CONTENT_TYPE);
        if (strings == null || strings.isEmpty()) {
            return httpRequest(httpExchange);
        }
        String s = strings.get(0);
        if (isMultipartFormData(s)) {
            return multipartRequest(httpExchange);
        } else {
            return httpRequest(httpExchange);
        }
    }

    static Request multipartRequest(HttpExchange httpExchange) {
        MultipartRequest multipartRequest = new MultipartRequest(httpExchange);
        multipartRequest.initBody();
        return multipartRequest;
    }

    static Request httpRequest(HttpExchange httpExchange) {
        HttpRequest httpRequest = new HttpRequest(httpExchange);
        httpRequest.initBody();
        return httpRequest;
    }


}