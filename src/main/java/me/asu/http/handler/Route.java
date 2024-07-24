package me.asu.http.handler;

import me.asu.http.handler.action.Action;
import me.asu.http.request.MethodConstants;
import me.asu.http.request.Request;
import me.asu.http.response.ResponseStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Route {
    String uri;
    Map<String, Action> actions = new HashMap<>();

    public Route(String uri) {
        this.uri = normalize(uri);
        addAction("default", req -> ResponseStatus.Response405);
    }


    public Route get(Action a) {
        if (a != null) {
            addAction(MethodConstants.GET, a);
        }
        return this;
    }

    public Route post(Action a) {
        if (a != null) {
            addAction(MethodConstants.POST, a);
        }
        return this;
    }

    public Route patch(Action a) {
        if (a != null) {
            addAction(MethodConstants.PATCH, a);
        }
        return this;
    }


    public Route put(Action a) {
        if (a != null) {
            addAction(MethodConstants.PUT, a);
        }
        return this;
    }


    public Route delete(Action a) {
        if (a != null) {
            addAction(MethodConstants.DELETE, a);
        }
        return this;
    }

    public Route head(Action a) {
        if (a != null) {
            addAction(MethodConstants.HEAD, a);
        }
        return this;
    }

    public Route defaultMethod(Action a) {
        if (a != null) {
            addAction("default", a);
        }
        return this;
    }

    public void addAction(String method, Action action) {
        Objects.requireNonNull(method);
        Objects.requireNonNull(action);
        actions.put(method, action);
    }

    public static String normalize(String uri) {
        if (!uri.startsWith("/")) {
            uri = "/" + uri;
        }
        if (uri.length() > 1 && uri.endsWith("/")) {
            uri = uri.substring(0, uri.length() - 1);
        }
        return uri;
    }

    public Action removeAction(String method) {
        return actions.remove(method);
    }

    public Object execute(String method, Request request) {
        Action action = actions.get(method);
        if (action == null) {
            if (!"default".equals(method)) {
                // try default
                action = actions.get("default");
            }
        }
        if (action == null) return ResponseStatus.Response404;

        return action.execute(request);
    }


}
