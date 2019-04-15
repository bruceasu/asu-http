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

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

public abstract class Http {

    public static class multipart {
        public static String getBoundary(String contentType) {
            if (null == contentType)
                return null;
            for (String tmp : contentType.split(";")) {
                tmp = tmp.trim();
                if (tmp.startsWith("boundary=")) {
                    return tmp.substring("boundary=".length());
                }
            }
            return null;
        }

        public static String formatName(String name, String filename, String contentType) {
            StringBuilder sb = new StringBuilder();
            sb.append("Content-Disposition: form-data; name=\"");
            sb.append(name);
            sb.append("\"");
            if (null != filename)
                sb.append("; filename=\"").append(filename).append("\"");
            if (null != contentType)
                sb.append("\nContent-Type: ").append(contentType);
            sb.append('\n').append('\n');
            return sb.toString();
        }

        public static String formatName(String name) {
            return formatName(name, null, null);
        }
    }

    public static Response get(String url) {
        return Sender.create(Request.get(url)).send();
    }

    public static Response get(String url, int timeout) {
        return Sender.create(Request.get(url)).setTimeout(timeout).send();
    }

    public static String post(String url, Map<String, Object> params, int timeout) {
        return Sender.create(Request.create(url, Request.METHOD.POST, params, null))
                     .setTimeout(timeout)
                     .send()
                     .getContent();
    }

    public static Response post2(String url, Map<String, Object> params, int timeout) {
        return Sender.create(Request.create(url, Request.METHOD.POST, params, null))
                     .setTimeout(timeout)
                     .send();
    }

    public static String encode(Object s) {
        if (null == s)
            return "";
        try {
            // Fix issue 283, 按照“茶几”的意见再次修改
            return URLEncoder.encode(s.toString(), "utf-8");
        }
        catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String post(String url, Map<String, Object> params, String inenc, String reenc) {
        return Sender.create(Request.create(url, Request.METHOD.POST, params, null).setEnc(inenc))
                     .send()
                     .getContent(reenc);
    }

    protected static ProxySwitcher proxySwitcher;

    protected static boolean autoSwitch;

    public static void setAutoSwitch(boolean use) {
        autoSwitch = use;
    }

    public static void setHttpProxy(String host, int port) {
        final Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
        proxySwitcher = new ProxySwitcher() {
            @Override
            public Proxy getProxy(URL url) {
                return proxy;
            }

            @Override
            public Proxy getProxy(Request req) {
                req.getHeader().set("Connection", "close");
                return getProxy(req.getUrl());
            }
        };
    }

    public static void setSocktProxy(String host, int port) {
        final Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(host, port));
        proxySwitcher = new ProxySwitcher() {
            @Override
            public Proxy getProxy(URL url) {
                return proxy;
            }

            @Override
            public Proxy getProxy(Request req) {
                req.getHeader().set("Connection", "close");
                return getProxy(req.getUrl());
            }
        };
    }

    public static ProxySwitcher getProxySwitcher() {
        return proxySwitcher;
    }

    public static void setProxySwitcher(ProxySwitcher proxySwitcher) {
        Http.proxySwitcher = proxySwitcher;
    }
}
