package me.asu.http.server;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import me.asu.http.util.Strings;
import me.asu.lang.map.MultiValueMap;

@Slf4j
public class ParamMap {

    MultiValueMap<String, String> map = MultiValueMap.create();

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public String getParameter(String key) {
        Object o = map.getValue(key, 0);
        if (o instanceof String) {
            return (String) o;
        } else if (o == null){
            return null;
        } else {
            return o.toString();
        }
    }

    public List<String> getParameters(String key) {
        return map.getValues(key);
    }

    public void setParameter(String key, String value) {
        map.add(key, value);
    }

    public Boolean getBoolean(String key) {
        String val = getParameter(key);
        if (Strings.isBlank(val)) {
            return null;
        }
        return Boolean.valueOf(val);
    }


    public Integer getInt(String key) {
        try {
            return Integer.valueOf(getParameter(key));
        } catch (NumberFormatException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public Long getLong(String key) {
        try {
            return Long.valueOf(getParameter(key));
        } catch (NumberFormatException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public Double getDouble(String key) {
        try {
            return Double.valueOf(getParameter(key));
        } catch (NumberFormatException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public Date getDate(String key, String fmt) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(fmt);
            return sdf.parse(getParameter(key));
        } catch (ParseException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public boolean containsParameter(String key) {
        return map.containsKey(key);
    }

    public Set<String> keySet() {
        return map.keySet();
    }

}