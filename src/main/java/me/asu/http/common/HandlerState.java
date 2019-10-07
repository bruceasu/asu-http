package me.asu.http.common;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.*;
import me.asu.http.server.HttpServerConfig;
import me.asu.util.Strings;

/**
 * Created by suk on 2019/6/2.
 */
public enum HandlerState {
    READY(0),
    STARTED(1),
    SHUTTING_DOWN(2),
    SHUTDOWN(3),
    ;
    int value = -1;
    HandlerState(int val) {
        this.value = val;
    }

}
