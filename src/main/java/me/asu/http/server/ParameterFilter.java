package me.asu.http.server;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.net.URI;
import java.util.*;

/**
 * @author suk
 * @since 2018/11/22
 */
public class ParameterFilter extends Filter {

    @Override
    public String description() {
        return "Parse query string to map.";
    }

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        ParamMap map = new ParamMap();
        URI uri = exchange.getRequestURI();
        // getQuery: 采用UTF-8编码解码%xx%xx%xx的参数信息。
        // getRawQuery: 直接返回原始数据
        String rawQuery = uri.getQuery();
        if (null != rawQuery) {

            String[] params = rawQuery.split("&");
            for (String param : params) {
                String[] paras = param.split("\\s*=");
                if (paras.length == 1) {
                    map.setParameter(paras[0], "");
                } else {
                    map.setParameter(paras[0], paras[1]);
                }
            }
        }
        exchange.setAttribute("parameters", map);
        chain.doFilter(exchange);
    }

    public static class ParamMap {

        HashMap<String, Object> map = new HashMap<>();

        public int size() {
            return map.size();
        }

        public boolean isEmpty() {
            return map.isEmpty();
        }

        public String getParameter(Object key) {
            Object o = map.get(key);
            if (o instanceof String) {
                return (String) o;
            } else if (o instanceof List) {
                return (String) ((List) o).get(0);
            } else {
                return null;
            }
        }

        public List<String> getParameters(String key) {
            Object o = map.get(key);
            if (o instanceof String) {
                return Arrays.asList((String) o);
            } else if (o instanceof List) {
                return (List<String>) o;
            } else {
                return null;
            }
        }

        public Object setParameter(String key, String value) {
            if (map.containsKey(key)) {
                Object o = map.get(key);
                if (o instanceof List) {
                    ((List) o).add(value);
                    return o;
                } else {
                    ArrayList<String> arrayList = new ArrayList<>();
                    arrayList.add((String) o);
                    arrayList.add(value);
                    return map.put(key, arrayList);
                }
            } else {
                return map.put(key, value);
            }
        }

        public boolean containsParameter(String key) {
            return map.containsKey(key);
        }

        public Set<String> keySet() {
            return map.keySet();
        }

    }
}

