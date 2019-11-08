package me.asu.http.util;

import static me.asu.http.util.Strings.unicodeDecode;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

/**
 * @author suk.
 * @since 2017/10/22
 */
public class ApplicationConf implements Map<String, String>, Serializable {

    protected Map<String, String> maps;

    public ApplicationConf(Reader reader) throws IOException {
        this();
        load(reader);
    }

    public ApplicationConf() {
        maps = new LinkedHashMap<String, String>();
    }


    private static boolean isBlank(CharSequence cs) {
        int strLen;
        if (cs != null && (strLen = cs.length()) != 0) {
            for (int i = 0; i < strLen; ++i) {
                if (!Character.isWhitespace(cs.charAt(i))) {
                    return false;
                }
            }

            return true;
        } else {
            return true;
        }
    }

    /**
     * <b>载入并销毁之前的记录</b>
     */
    public synchronized void load(Reader reader) throws IOException {
        load(reader, false);
    }

    public synchronized void load(Reader reader, boolean clear) throws IOException {
        if (clear) {
            this.clear();
        }
        BufferedReader tr = null;
        if (reader instanceof BufferedReader) {
            tr = (BufferedReader) reader;
        } else {
            tr = new BufferedReader(reader);
        }
        String s;
        while (null != (s = tr.readLine())) {
            if (isBlank(s)) {
                continue;
            }
            // 只要第一个非空白字符是#,就认为是注释
            if (s.length() > 0 && s.trim().charAt(0) == '#') {
                continue;
            }
            int pos;
            char c = '0';
            for (pos = 0; pos < s.length(); pos++) {
                c = s.charAt(pos);
                if (c == '=' || c == ':') {
                    break;
                }
            }
            if (c == '=') {
                String name = s.substring(0, pos);
                String value = s.substring(pos + 1);
                if (value.endsWith("\\") && !value.endsWith("\\\\")) {
                    StringBuilder sb = new StringBuilder(value.substring(0, value.length() - 1));
                    while (null != (s = tr.readLine())) {
                        if (isBlank(s)) {
                            break;
                        }
                        if (s.endsWith("\\") && !s.endsWith("\\\\")) {
                            sb.append(s.substring(0, s.length() - 1));
                        } else {
                            sb.append(s);
                            break;
                        }
                    }
                    value = sb.toString();
                }
                // 对value里面的\\uXXXX进行转义?
                if (value.contains("\\u")) {
                    value = unicodeDecode(value);
                }
                value = value.replace("\\:", ":").replace("\\=", "=");
                maps.put(Strings.trim(name), value);
            } else if (c == ':') {
                String name = s.substring(0, pos);
                StringBuffer sb = new StringBuffer();
                sb.append(s.substring(pos + 1));
                String ss;
                while (null != (ss = tr.readLine())) {
                    if (ss.length() > 0 && ss.charAt(0) == '#') {
                        break;
                    }
                    sb.append("\r\n" + ss);
                }
                maps.put(trim(name), sb.toString());
                if (null == ss) {
                    return;
                }
            } else {
                maps.put(trim(s), null);
            }
        }
    }

    @Override
    public synchronized void clear() {
        maps.clear();
    }

    @Override
    public boolean containsKey(Object key) {
        return maps.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return maps.containsValue(value);
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        return maps.entrySet();
    }

    @Override
    public boolean equals(Object o) {
        return maps.equals(o);
    }

    @Override
    public int hashCode() {
        return maps.hashCode();
    }

    @Override
    public boolean isEmpty() {
        return maps.isEmpty();
    }

    @Override
    public Set<String> keySet() {
        return maps.keySet();
    }

    public List<String> keys() {
        return new ArrayList<String>(maps.keySet());
    }

    @Override
    public synchronized String put(String key, String value) {
        return maps.put(key, value);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public synchronized void putAll(Map t) {
        maps.putAll(t);
    }

    @Override
    public synchronized String remove(Object key) {
        return maps.remove(key);
    }

    @Override
    public int size() {
        return maps.size();
    }

    @Override
    public Collection<String> values() {
        return maps.values();
    }

    @Override
    public String get(Object key) {
        return maps.get(key);
    }

    public void print(OutputStream out) throws IOException {
        print(new OutputStreamWriter(out, Charset.forName("utf-8")));
    }

    public void print(Writer writer) throws IOException {
        String NL = System.getProperty("line.separator");
        for (Entry<String, String> en : entrySet()) {
            writer.write(en.getKey());
            String val = en.getValue();
            if (val == null) {
                writer.write("=");
                continue;
            }
            if (val.contains("\n")) {
                writer.write(":=");
                writer.write(val);
                writer.write(NL);
                writer.write("#End " + en.getKey());
            } else {
                writer.write('=');
                writer.write(val);
            }
            writer.write(NL);
        }
        writer.flush();
    }

    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    public boolean getBoolean(String key, boolean dfval) {
        String val = get(key);
        if (isBlank(val)) {
            return dfval;
        }
        return Boolean.parseBoolean(val);
    }

    public String get(String key, String defaultValue) {
        String s = get(key);
        if (isBlank(s)) {
            return defaultValue;
        } else {
            return s;
        }
    }

    public List<String> getList(String key) {
        return getList(key, "\n");
    }

    public List<String> getList(String key, String separatorChar) {
        List<String> re = new ArrayList<String>();
        String keyVal = get(key);
        if (!isBlank(keyVal)) {
            String[] vlist = keyVal.split(separatorChar);
            for (String v : vlist) {
                re.add(v);
            }
        }
        return re;
    }

    public String trim(String key) {
        return Strings.trim(get(key));
    }

    public String trim(String key, String defaultValue) {
        return Strings.trim(get(key, defaultValue));
    }

    public int getInt(String key) {
        return getInt(key, -1);
    }

    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(getTrim(key));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public long getLong(String key) {
        return getLong(key, -1);
    }

    public long getLong(String key, long dfval) {
        try {
            return Long.parseLong(getTrim(key));
        } catch (NumberFormatException e) {
            return dfval;
        }
    }

    public String getTrim(String key) {
        return Strings.trim(get(key));
    }

    public String getTrim(String key, String defaultValue) {
        return Strings.trim(get(key, defaultValue));
    }

    public List<String> getKeys() {
        return keys();
    }

    public Collection<String> getValues() {
        return values();
    }

    public Properties toProperties() {
        Properties p = new Properties();
        p.putAll(this);
        return p;
    }

}
