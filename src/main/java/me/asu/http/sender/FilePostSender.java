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

package me.asu.http.sender;

import java.io.*;
import java.util.Map;
import java.util.Map.Entry;

import me.asu.util.io.Streams;
import me.asu.http.HttpException;
import me.asu.http.Request;
import me.asu.http.Response;

public class FilePostSender extends PostSender {

    public static final String SEPARATOR = "\r\n";

    public FilePostSender(Request request) {
        super(request);
    }

    @Override
    public Response send() throws HttpException {
        try {
            String boundary = "---------------------------[Nutz]7d91571440efc";
            openConnection();
            setupRequestHeader();
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
            setupDoInputOutputFlag();
            Map<String, Object> params = request.getParams();
            if (null != params && params.size() > 0) {
                DataOutputStream outs = new DataOutputStream(conn.getOutputStream());
                for (Entry<String, ?> entry : params.entrySet()) {
                    outs.writeBytes("--" + boundary + SEPARATOR);
                    String key = entry.getKey();
                    File f = null;
                    if (entry.getValue() instanceof File) {
                        f = (File) entry.getValue();
                    } else if (entry.getValue() instanceof String) {
                        f = new File(entry.getValue().toString());
                    }
                    if (f != null && f.exists()) {
                        outs.writeBytes("Content-Disposition:    form-data;    name=\"" + key
                                + "\";    filename=\"" + entry.getValue() + "\"\r\n");
                        outs.writeBytes("Content-Type:   application/octet-stream\r\n\r\n");
                        if (f.length() == 0) {
                            continue;
                        }
                        InputStream is = new FileInputStream(f);
                        byte[] buffer = new byte[8192];
                        while (true) {
                            int amountRead = is.read(buffer);
                            if (amountRead == -1) {
                                break;
                            }
                            outs.write(buffer, 0, amountRead);
                        }
                        outs.writeBytes("\r\n");
                        is.close();
                    } else {
                        outs.writeBytes("Content-Disposition:    form-data;    name=\"" + key
                                + "\"\r\n\r\n");
                        outs.writeBytes(entry.getValue() + "\r\n");
                    }
                }
                outs.writeBytes("--" + boundary + "--" + SEPARATOR);
                Streams.safeFlush(outs);
                Streams.safeClose(outs);
            }

            return createResponse(getResponseHeader());

        } catch (IOException e) {
            throw new HttpException(request.getUrl().toString(), e);
        }
    }
}
