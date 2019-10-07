package me.asu.http.server;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import me.asu.http.common.HeaderKey;
import me.asu.lang.map.MultiValueMap;
import me.asu.util.Bytes;
import me.asu.util.ParseXMLUtils;
import me.asu.util.io.Streams;
import org.nutz.json.Json;

@Slf4j
public class HttpRequest implements Request {

    /**
     * HttpExchange
     */
    private HttpExchange httpExchange;

    /**
     * query data
     */
    private ParamMap paramMap = new ParamMap();

    /**
     * form data
     */
    private ParamMap bodyMap = new ParamMap();

    /**
     * xml or json parse result
     */
    private Map<String, Object> dataMap = new HashMap<>();

    /**
     * header
     */
    private MultiValueMap<String, String> headMap = MultiValueMap.create();

    /**
     * raw body
     */
    private byte[] requestBody;

    public HttpRequest(HttpExchange httpExchange) {
        this.httpExchange = httpExchange;
        initRequestParam();
        initRequestHeader();

    }

    @Override
    public void initBody() {
        String contentType = getHeadMap().getValue(HeaderKey.CONTENT_TYPE);
        initRequestBody();
        if (Request.isForm(contentType)) {
            initFormParam();
        } else if (Request.isJson(contentType)) {
            dataMap = Json.fromJson(HashMap.class, getRequestBody());
        } else if (Request.isXml(contentType)) {
            dataMap = ParseXMLUtils.string2Map(getRequestBody());
        }

    }

    @Override
    public HttpExchange getHttpExchange() {
        return httpExchange;
    }

    @Override
    public ParamMap getParamMap() {
        return paramMap;
    }

    public MultiValueMap<String, String> getHeadMap() {
        return this.headMap;
    }

    @Override
    public Map<String, Object> getDataMap() {
        return dataMap;
    }

    @Override
    public String getParameter(String param) {
        return paramMap.getParameter(param);
    }

    @Override
    public String getMethod() {
        return httpExchange.getRequestMethod().trim().toUpperCase();
    }

    @Override
    public URI getReuestURI() {
        return httpExchange.getRequestURI();
    }

    @Override
    public String getRequestBody() {
        return Bytes.toString(requestBody);
    }

    private void initRequestParam() {
        // getQuery: 采用UTF-8编码解码%xx%xx%xx的参数信息。
        // getRawQuery: 直接返回原始数据
        String query = getReuestURI().getQuery();
        if (null != query) {
            String[] params = query.split("&");
            for (String param : params) {
                String[] paras = param.split("=");
                if (paras.length == 1) {
                    paramMap.setParameter(paras[0], "");
                } else {
                    paramMap.setParameter(paras[0], paras[1]);
                }
            }
        }

    }

    private void initRequestHeader() {
        for (String s : httpExchange.getRequestHeaders().keySet()) {
            headMap.add(s, httpExchange.getRequestHeaders().get(s));
        }
    }

    private void initRequestBody() {
        // 获得输入流
        InputStream in = httpExchange.getRequestBody();
        try {
            requestBody = Streams.readBytes(in);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void initFormParam() {
        String query = getRequestBody();
        if (null != query) {
            String[] params = query.split("&");
            for (String param : params) {
                String[] paras = param.split("=");
                if (paras.length == 1) {
                    String key = paras[0];
                    try {
                        key = URLDecoder.decode(key, "utf-8");
                    } catch (UnsupportedEncodingException e) {
                    }
                    bodyMap.setParameter(key, "");
                } else {
                    String key = paras[0];
                    try {
                        key = URLDecoder.decode(key, "utf-8");
                    } catch (UnsupportedEncodingException e) {
                    }
                    String val = paras[1];
                    try {
                        val = URLDecoder.decode(val, "utf-8");
                    } catch (UnsupportedEncodingException e) {
                    }
                    bodyMap.setParameter(key, val);
                }
            }
        }
    }
}