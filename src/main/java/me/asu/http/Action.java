package me.asu.http;

import me.asu.http.request.Request;
import me.asu.http.response.ResponseStatus;

public interface Action
{
    default Object get(Request req) {return ResponseStatus.Response405;}
    default Object post(Request req){return ResponseStatus.Response405;}
    default Object delete(Request req){return ResponseStatus.Response405;}
    default Object put(Request req){return ResponseStatus.Response405;}
    default Object patch(Request req){return ResponseStatus.Response405;}

    Object attachment(Object attach);
    Object attachment();
}
