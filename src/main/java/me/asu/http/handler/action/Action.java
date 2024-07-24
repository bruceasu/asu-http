package me.asu.http.handler.action;

import me.asu.http.request.Request;
import me.asu.http.response.ResponseStatus;

@FunctionalInterface
public interface Action {
    Object execute(Request req);
}
