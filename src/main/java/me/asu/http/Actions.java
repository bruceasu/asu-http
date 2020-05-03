package me.asu.http;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Data;
import lombok.Getter;

public class Actions
{
    private HashMap<String, Action> actions = new HashMap<>();
    private HashMap<String, Action> wildCardActions = new HashMap<>();

    private static class Holder
    {
        public static final Actions INSTANCE = new Actions();
    }

    public static Actions getInstance()
    {
        return Holder.INSTANCE;
    }

    private Actions(){}

    public void addActions(Map<String, Action> a)
    {
        if (a != null) {
           a.forEach(this::addAction);
        }

    }

    public void addAction(String uri, Action action) {
        Objects.requireNonNull(uri);
        Objects.requireNonNull(action);
        uri = normalize(uri);

        if (uri.matches(".*\\{.+\\}.*")) {
            wildCardActions.put(uri, action);
        } else {
            actions.put(uri, action);
        }


    }

    public String normalize(String uri)
    {
        if (!uri.startsWith("/")) {
            uri = "/" + uri;
        }
        if (uri.length() > 1 && uri.endsWith("/")) {
            uri = uri.substring(0, uri.length() - 1);
        }
        return uri;
    }

    public Action removeAction(String uri) {
        return actions.remove(uri);
    }

    public Action getAction(String uri) {
        uri = normalize(uri);
        Action action = actions.get(uri);
        if (action == null) {
            // try wildcard
            UriParser uriParser = new UriParser();
            for (Map.Entry<String, Action> entry : wildCardActions.entrySet()) {
                String pattern = entry.getKey();
                Action act = entry.getValue();
                uriParser.parse(uri, pattern);
                if (uriParser.isMatch()) {
                    List<Pair<String, String>> variables = Collections.unmodifiableList(uriParser.getList());
                    act.attachment(variables);
                    return act;
                }
            }
        }
        return action;
    }
    /*
        String value = extractApiEntryValue(m);

     */
    @Getter
    static class UriParser {

        static Pattern PATH_VARIABLE_PATTERN = Pattern.compile("\\{(.+?)\\}");
        LinkedList<Pair<String, String>> list  = new LinkedList<>();
        boolean                          match = false;

        void parse(final String subUri, final String pattern) {
            list.clear();
            match = false;

            Matcher matcher = PATH_VARIABLE_PATTERN.matcher(pattern);
            List<String> names = new ArrayList<>();
            StringBuffer buf = new StringBuffer("^");
            while (matcher.find()) {
                names.add(pattern.substring(matcher.start() + 1, matcher.end() - 1));
                matcher.appendReplacement(buf, "([^/]+?)");
            }
            matcher.appendTail(buf);
            buf.append("$");
            Pattern xPattern = Pattern.compile(buf.toString());
            Matcher matcher1 = xPattern.matcher(subUri);
            if (matcher1.matches()) {
                match = true;
                for (int i = 0; i < names.size(); i++) {
                    String group = matcher1.group(i + 1);
                    list.add(new Pair(names.get(i), group));
                }
            }
        }
    }

    @Data
    static class Pair<T, E> {

        T key;
        E value;

        public Pair(T k, E v) {
            key = k;
            value = v;
        }
    }
}
