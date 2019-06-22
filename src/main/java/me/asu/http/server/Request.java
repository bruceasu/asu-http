package me.asu.http.server;

import com.sun.net.httpserver.HttpExchange;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface Request {

    public final static String GET = "GET";
    public final static String POST = "POST";

    public String getParamter(String param);

    public String getMethod();

    public URI getReuestURI();

    public void initRequestHeader();

    public void initRequestParam();

    public void initRequestBody();

    public String getRequestBody();

    public static class HttpRequest implements Request {
        private HttpExchange httpExchange;
        private Map<String, String> paramMap = new HashMap<String, String>();
        private Map<String, List<String>> headMap = new HashMap<String, List<String>>();
        private String requestBody = "";

        public HttpRequest(HttpExchange httpExchange) {
            this.httpExchange = httpExchange;
        }

        @Override
        public String getParamter(String param) {
            return paramMap.get(param);
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
        public void initRequestParam() {
            String query = getReuestURI().getQuery();
            String [] arrayStr = query.split("&");
            for(String str : arrayStr){
                paramMap.put(str.split("=")[0], str.split("=")[1]);
            }

        }

        @Override
        public void initRequestHeader() {
            for(String s : httpExchange.getRequestHeaders().keySet()){
                headMap.put(s, httpExchange.getRequestHeaders().get(s));
            }
        }

        @Override
        public void initRequestBody() {
            InputStream in = httpExchange.getRequestBody(); // 获得输入流
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String temp = null;
            try {
                while ((temp = reader.readLine()) != null) {
                    requestBody += temp;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public String getRequestBody() {
            return requestBody;
        }
    }
}