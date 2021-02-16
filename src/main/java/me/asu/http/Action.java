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
    /**
     * param attach 如果是通配路径，这里存放解析到匹配的值  List<Pair<String, String>>, 应该存储起来
     */
    default Object attachment(Object attach) {return this;};
    default Object attachment() {return null;}
}
