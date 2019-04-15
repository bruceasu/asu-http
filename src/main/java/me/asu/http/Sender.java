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

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.net.ssl.*;
import me.asu.http.sender.*;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import lombok.extern.slf4j.Slf4j;

/**
 * @author zozoh(zozohtnt@gmail.com)
 * @author wendal(wendal1985@gmail.com)
 */
@Slf4j
public abstract class Sender {

    /**
     * 默认连接超时, 30秒
     */
    public static int Default_Conn_Timeout = 30 * 1000;
    /**
     * 默认读取超时, 10分钟
     */
    public static int Default_Read_Timeout = 10 * 60 * 1000;

    public static Sender create(String url) {
        return create(Request.get(url));
    }

    public static Sender create(String url, int timeout) {
        return create(Request.get(url)).setTimeout(timeout);
    }

    public static Sender create(Request request) {
        switch (request.getMethod()) {
            case GET:
                return new GetSender(request);
            case POST:
                return new PostSender(request);
            case PUT:
                return new PutSender(request);
            case DELETE:
                return new DeleteSender(request);
            case PATCH:
                return new PatchSender(request);
            default:
                // fallback
                return new GetSender(request);
        }
    }

    public static Sender create(Request request, int timeout) {
        Sender sender = null;
        switch (request.getMethod()) {
            case GET:
                sender = new GetSender(request);
                break;
            case POST:
                sender = new PostSender(request);
                break;
            case PUT:
                sender = new PutSender(request);
                break;
            case DELETE:
                sender = new DeleteSender(request);
                break;
            case PATCH:
                sender = new PatchSender(request);
                break;
            default:
                // fallback
                sender = new GetSender(request);
                break;
        }
        return sender.setTimeout(timeout);
    }

    protected Request request;

    protected int timeout;

    protected HttpURLConnection conn;

    protected Sender(Request request) {
        this.request = request;
    }

    public abstract Response send() throws HttpException;

    protected Response createResponse(Map<String, String> reHeaders) throws IOException {
        Response rep = null;
        if (reHeaders != null) {
            rep = new Response(conn, reHeaders);
            if (rep.isOK()) {
                InputStream is1 = conn.getInputStream();
                InputStream is2 = null;
                String encoding = conn.getContentEncoding();
                // 如果采用了压缩,则需要处理否则都是乱码
                if (encoding != null && encoding.contains("gzip")) {
                    is2 = new GZIPInputStream(is1);
                } else if (encoding != null && encoding.contains("deflate")) {
                    is2 = new InflaterInputStream(is1, new Inflater(true));
                } else {
                    is2 = is1;
                }

                BufferedInputStream is = new BufferedInputStream(is2);
                rep.setStream(is);
            } else {
                try {
                    rep.setStream(conn.getInputStream());
                } catch (IOException e) {
                    rep.setStream(new ByteArrayInputStream(new byte[0]));
                }
            }
        }
        return rep;
    }

    protected Map<String, String> getResponseHeader() throws IOException {
        if (conn.getResponseCode() < 0) {
            throw new IOException("Network error!! resp code < 0");
        }
        Map<String, String> reHeaders = new HashMap<String, String>();
        for (Entry<String, List<String>> en : conn.getHeaderFields().entrySet()) {
            List<String> val = en.getValue();
            if (null != val && val.size() > 0) {
                reHeaders.put(en.getKey(), en.getValue().get(0));
            }
        }
        return reHeaders;
    }

    protected void setupDoInputOutputFlag() {
        conn.setDoInput(true);
        conn.setDoOutput(true);
    }

    protected void openConnection() {
        ProxySwitcher proxySwitcher = Http.proxySwitcher;
        if (proxySwitcher != null) {
            try {
                Proxy proxy = proxySwitcher.getProxy(request);
                if (proxy != null) {
                    testProxyAvailable(proxy);

                    conn = (HttpURLConnection) request.getUrl().openConnection(proxy);
                    conn.setConnectTimeout(Default_Conn_Timeout);
                    if (timeout > 0) {
                        conn.setReadTimeout(timeout);
                    } else {
                        conn.setReadTimeout(Default_Read_Timeout);
                    }
                    initSsl();
                    return;
                }
            } catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
                if (!Http.autoSwitch) {
                    throw new HttpException(e);
                }
                log.info("Test proxy FAIl, fallback to direct connection", e);
            }

        }
        openWithoutProxy();
    }

    private void testProxyAvailable(Proxy proxy) throws IOException {
        if (Http.autoSwitch) {
            Socket socket = null;
            try {
                socket = new Socket();
                socket.connect(proxy.address(), 5 * 1000);
            } finally {
                if (socket != null) {
                    socket.close();
                }
            }
        }
    }

    private void openWithoutProxy() {
        try {
            conn = (HttpURLConnection) request.getUrl().openConnection();
            conn.setConnectTimeout(Default_Conn_Timeout);
            conn.setRequestMethod(request.getMethod().name());
            if (timeout > 0) {
                conn.setReadTimeout(timeout);
            } else {
                conn.setReadTimeout(Default_Read_Timeout);
            }
            initSsl();
        } catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            if (!Http.autoSwitch) {
                throw new HttpException(e);
            }
        }
    }

    private void initSsl() throws NoSuchAlgorithmException, KeyManagementException {
        if (conn instanceof HttpsURLConnection) {
            SSLContext ctx = SSLContext.getInstance("TLS");
            KeyManager[] kms = new KeyManager[0];
            TrustManager[] tms = new TrustManager[]{new DefaultTrustManager()};
            SecureRandom random = new SecureRandom();
            ctx.init(kms, tms, random);
            SSLContext.setDefault(ctx);
            HttpsURLConnection sslConn = (HttpsURLConnection) conn;
            sslConn.setHostnameVerifier((hostname, session) -> true);
        }
    }

    protected void setupRequestHeader() {
        URL url = request.getUrl();
        String host = url.getHost();
        if (url.getPort() > 0 && url.getPort() != 80) {
            host += ":" + url.getPort();
        }
        conn.setRequestProperty("Host", host);
        Header header = request.getHeader();
        if (null != header) {
            for (Entry<String, String> entry : header.getAll()) {
                conn.addRequestProperty(entry.getKey(), entry.getValue());
            }
        }
    }

    public Sender setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    public int getTimeout() {
        return timeout;
    }

}
