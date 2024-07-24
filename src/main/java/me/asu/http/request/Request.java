package me.asu.http.request;

import com.sun.net.httpserver.HttpExchange;
import me.asu.http.common.CommonContentType;
import me.asu.http.common.HeaderKey;
import me.asu.http.util.map.MultiValueMap;

import java.io.IOException;
import java.net.URI;
import java.util.List;

public interface Request {

    static boolean isMultipartFormData( String contentType) {
        return (contentType != null && contentType.startsWith(CommonContentType.FORM_DATA.type()));
    }

    default boolean isForm() {
        String contentType = contentType();
        return (contentType != null && contentType.startsWith(CommonContentType.FORM.type()));
    }

    default boolean isJson() {
        String contentType = contentType();
        return (contentType != null && contentType.startsWith(CommonContentType.JSON.type()));
    }

    default boolean isXml() {
        String contentType = contentType();
        return (contentType != null && contentType.startsWith(CommonContentType.XML.type()));
    }

    String contentType();

//    Map<String, Object> getDataMap();

    static Request createRequest(HttpExchange httpExchange) {
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

    String getParameter(String param);

    ParamMap getParamMap();

    ParamMap getBodyMap();

    MultiValueMap<String, String> getHeadMap();

    String getMethod();

    URI getRequestURI();

    String getRequestBody();

    byte[] getRawBody();

    <T> T getJson(Class<T> clazz) throws IOException;

    void initBody();

    HttpExchange getHttpExchange();


}