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

import me.asu.util.io.Streams;
import me.asu.util.Strings;

import java.io.*;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.util.Map;


public class Response {

    private static final String DEF_PROTOCAL_VERSION = "HTTP/1.1";

    public Response(HttpURLConnection conn, Map<String, String> reHeader) throws IOException {
        status = conn.getResponseCode();
        detail = conn.getResponseMessage();
        this.header = Header.create(reHeader);
        String s = header.get("Set-Cookie");
        if (null != s) {
            this.cookie = new Cookie(s);
        }
    }

    private Header header;
    private InputStream stream;
    private Cookie cookie;
    private String protocal = DEF_PROTOCAL_VERSION;
    private int status;
    private String detail;
    private String content;

    public String getProtocal() {
        return protocal;
    }

    public int getStatus() {
        return status;
    }

    public String getDetail() {
        return detail;
    }

    public boolean isOK() {
        return status == 200;
    }

    public boolean isServerError() {
        return status >= 500 && status < 600;
    }

    public boolean isClientError() {
        return status >= 400 && status < 500;
    }

    void setStream(InputStream stream) {
        this.stream = stream;
    }

    public Header getHeader() {
        return header;
    }

    /**
     * 根据Http头的Content-Type获取网页的编码类型，如果没有设的话则返回null
     */
    public String getEncodeType() {
        String contextType = header.get("Content-Type");
        if (null != contextType) {
            for (String tmp : contextType.split(";")) {
                if (tmp == null) {
                    continue;
                }
                tmp = tmp.trim();
                if (tmp.startsWith("charset=")) {
                    return tmp.substring(8).trim();
                }
            }
        }
        return null;
    }

    public InputStream getStream() {
        return new BufferedInputStream(stream);
    }

    public Reader getReader() {
        String encoding = this.getEncodeType();
        if (null == encoding) {
            return getReader("utf-8");
        } else {
            return getReader(encoding);
        }
    }

    public Reader getReader(String charsetName) {
        return new InputStreamReader(getStream(), Charset.forName(charsetName));
    }

    public Cookie getCookie() {
        return cookie;
    }

    public void printHeader(Writer writer) {
        try {
            writer.write(header.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void print(Writer writer) {
        print(writer, null);
    }

    public void print(Writer writer, String charsetName) {
        Reader reader = null;
        try {
            if (null == charsetName) {
                reader = getReader();
            } else {
                reader = this.getReader(charsetName);
            }
            int c;
            char[] buf = new char[8192];
            while (-1 != (c = reader.read(buf))) {
                writer.write(buf, 0, c);
            }
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getContent() {
        if (Strings.isBlank(content)) {
            content = getContent(null);
        }
        return content;
    }

    public String getContent(String charsetName) {
        if (charsetName == null) {
            return Streams.readAndClose(getReader());
        }
        return Streams.readAndClose(getReader(charsetName));
    }


}
