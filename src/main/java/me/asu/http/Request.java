/******************************************************************************
 * Copyright (c) <2014 - 2015>, Suk Honzeon <sukhonzeon@gmail.com>            *
 * All rights reserved.                                                       *
 * Redistribution and use in source and binary forms, with or without         *
 * modification, are permitted provided that the following conditions are met:*
 * Redistributions of source code must retain the above copyright notice,     *
 * this list of conditions and the following disclaimer.                      *
 * Redistributions in binary form must reproduce the above copyright notice,  *
 * this list of conditions and the following disclaimer in the documentation  *
 * and/or other materials provided with the distribution.                     *
 * Neither the name of the ASU nor the names of its contributors              *
 * may be used to endorse or promote products derived from this software      *
 * without specific prior written permission.                                 *
 *                                                                            *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS        *
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT          *
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR      *
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT       *
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,      *
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED   *
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,        *
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY     *
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT               *
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE      *
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.       *
 ******************************************************************************/

package me.asu.http;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import me.asu.util.Bytes;

public class Request {

    public static enum METHOD {
        GET, POST, OPTIONS, PUT, DELETE, TRACE, CONNECT,PATCH
    }

    public static Request get(String url) {
        return create(url, METHOD.GET, new HashMap<String, Object>());
    }

    public static Request get(String url, Header header) {
        return Request.create(url, METHOD.GET, new HashMap<String, Object>(), header);
    }

    public static Request post(String url) {
        return create(url, METHOD.POST, new HashMap<String, Object>());
    }

    public static Request post(String url, Header header) {
        return Request.create(url, METHOD.POST, new HashMap<String, Object>(), header);
    }

    public static Request create(String url, METHOD method) {
        return create(url, method, new HashMap<String, Object>());
    }


    public static Request create(String url, METHOD method, Map<String, Object> params) {
        return create(url, method, params, Header.create());
    }

    public static Request create(String url,
                                 METHOD method,
                                 Map<String, Object> params,
                                 Header header) {
        return new Request().setMethod(method).setParams(params).setUrl(url).setHeader(header);
    }

    private Request() {
    }

    private String              url;
    private METHOD              method;
    private Header              header;
    private Map<String, Object> params;
    private byte[]              data;
    private URL                 cacheUrl;
    private InputStream         inputStream;
    private String              enc;

    public URL getUrl() {
        if (cacheUrl != null) {
            return cacheUrl;
        }

        StringBuilder sb = new StringBuilder(url);
        try {
            if (this.isGet() && null != params && params.size() > 0) {
                sb.append(url.indexOf('?') > 0 ? '&' : '?');
                sb.append(getURLEncodedParams());
            }
            cacheUrl = new URL(sb.toString());
            return cacheUrl;
        } catch (Exception e) {
            throw new HttpException(sb.toString(), e);
        }
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public String getURLEncodedParams() {
        StringBuilder sb = new StringBuilder();
        if (params != null) {
            for (Iterator<Map.Entry<String, Object>> it = params.entrySet().iterator(); it
                    .hasNext(); ) {
                Map.Entry<String, Object> e = it.next();
                sb.append(Http.encode(e.getKey())).append('=').append(Http.encode(e.getValue()));
                if (it.hasNext()) {
                    sb.append('&');
                }
            }
        }
        return sb.toString();
    }

    public InputStream getInputStream() {
        if (inputStream != null) {
            return inputStream;
        } else {
            if (null == data) {
                if (enc != null) {
                    try {
                        return new ByteArrayInputStream(getURLEncodedParams().getBytes(enc));
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
                }
                return new ByteArrayInputStream(Bytes.toBytes(getURLEncodedParams()));
            }
            return new ByteArrayInputStream(data);
        }
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public void setData(String data) {
        try {
            this.data = data.getBytes("utf-8");
        } catch (UnsupportedEncodingException e) {
            // 不可能
        }
    }

    private Request setParams(Map<String, Object> params) {
        this.params = params;
        return this;
    }

    public Request setUrl(String url) {
        if (url != null && url.indexOf("://") < 0)
        // 默认采用http协议
        {
            this.url = "http://" + url;
        } else {
            this.url = url;
        }
        return this;
    }

    public METHOD getMethod() {
        return method;
    }

    public boolean isGet() {
        return METHOD.GET == method;
    }

    public boolean isPost() {
        return METHOD.POST == method;
    }

    public boolean isDelete() {
        return METHOD.DELETE == method;
    }

    public boolean isPut() {
        return METHOD.PUT == method;
    }

    public Request setMethod(METHOD method) {
        this.method = method;
        return this;
    }

    public Header getHeader() {
        return header;
    }

    public Request setHeader(Header header) {
        this.header = header;
        return this;
    }

    public Request setCookie(Cookie cookie) {
        header.set("Cookie", cookie.toString());
        return this;
    }

    public Cookie getCookie() {
        String s = header.get("Cookie");
        if (null == s) {
            return new Cookie();
        }
        return new Cookie(s);
    }

    /**
     * 设置发送内容的编码,仅对String或者Map<String,Object>类型的data有效
     */
    public Request setEnc(String reqEnc) {
        this.enc = reqEnc;
        return this;
    }
}
