package me.asu.http.response;

import static java.net.HttpURLConnection.*;

public class ResponseStatus {

    public static final Response Response100 = new Response() {{
        setCode(100);
    }};
    public static final Response Response101 = new Response() {{
        setCode(101);
    }};

    public static final Response Response201 = new Response() {{
        setCode(HTTP_CREATED);
    }};
    public static final Response Response202 = new Response() {{
        setCode(HTTP_ACCEPTED);
    }};
    public static final Response Response203 = new Response() {{
        setCode(HTTP_NOT_AUTHORITATIVE);
    }};
    public static final Response Response204 = new Response() {{
        setCode(HTTP_NO_CONTENT);
    }};
    public static final Response Response205 = new Response() {{
        setCode(HTTP_RESET);
    }};
    public static final Response Response206 = new Response() {{
        setCode(HTTP_PARTIAL);
    }};

    public static final Response Response300 = new Response() {{
        setCode(HTTP_MULT_CHOICE);
    }};
    public static final Response Response301 = new Response() {{
        setCode(HTTP_MOVED_PERM);
    }};
    public static final Response Response302 = new Response() {{
        setCode(HTTP_MOVED_TEMP);
    }};
    public static final Response Response303 = new Response() {{
        setCode(HTTP_SEE_OTHER);
    }};
    public static final Response Response304 = new Response() {{
        setCode(HTTP_NOT_MODIFIED);
    }};
    public static final Response Response305 = new Response() {{
        setCode(HTTP_USE_PROXY);
    }};
    public static final Response Response307 = new Response() {{
        setCode(307);
    }};


    public static final Response Response400 = new Response() {{
        setCode(HTTP_BAD_REQUEST);
    }};
    public static final Response Response401 = new Response() {{
        setCode(HTTP_UNAUTHORIZED);
    }};
    public static final Response Response402 = new Response() {{
        setCode(HTTP_PAYMENT_REQUIRED);
    }};
    public static final Response Response403 = new Response() {{
        setCode(HTTP_FORBIDDEN);
    }};
    public static final Response Response404 = new Response() {{
        setCode(HTTP_NOT_FOUND);
    }};
    public static final Response Response405 = new Response() {{
        setCode(HTTP_BAD_METHOD);
    }};
    public static final Response Response406 = new Response() {{
        setCode(HTTP_NOT_ACCEPTABLE);
    }};
    public static final Response Response407 = new Response() {{
        setCode(HTTP_PROXY_AUTH);
    }};
    public static final Response Response408 = new Response() {{
        setCode(HTTP_CLIENT_TIMEOUT);
    }};
    public static final Response Response409 = new Response() {{
        setCode(HTTP_CONFLICT);
    }};
    public static final Response Response410 = new Response() {{
        setCode(HTTP_GONE);
    }};
    public static final Response Response411 = new Response() {{
        setCode(HTTP_LENGTH_REQUIRED);
    }};
    public static final Response Response412 = new Response() {{
        setCode(HTTP_PRECON_FAILED);
    }};
    public static final Response Response413 = new Response() {{
        setCode(HTTP_ENTITY_TOO_LARGE);
    }};
    public static final Response Response414 = new Response() {{
        setCode(HTTP_REQ_TOO_LONG);
    }};
    public static final Response Response415 = new Response() {{
        setCode(HTTP_UNSUPPORTED_TYPE);
    }};
    public static final Response Response416 = new Response() {{
        setCode(416);
    }};
    public static final Response Response417 = new Response() {{
        setCode(417);
    }};
    public static final Response Response418 = new Response() {{
        setCode(418);
    }};
    public static final Response Response420 = new Response() {{
        setCode(420);
    }};


    public static final Response Response500 = new Response() {{
        setCode(HTTP_INTERNAL_ERROR);
    }};
    public static final Response Response501 = new Response() {{
        setCode(HTTP_NOT_IMPLEMENTED);
    }};
    public static final Response Response502 = new Response() {{
        setCode(HTTP_BAD_GATEWAY);
    }};
    public static final Response Response503 = new Response() {{
        setCode(HTTP_UNAVAILABLE);
    }};
    public static final Response Response504 = new Response() {{
        setCode(HTTP_GATEWAY_TIMEOUT);
    }};
    public static final Response Response505 = new Response() {{
        setCode(HTTP_VERSION);
    }};

}
