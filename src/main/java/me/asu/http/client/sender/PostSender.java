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

package me.asu.http.client.sender;

import java.io.*;

import me.asu.util.io.Streams;
import me.asu.http.client.HttpException;
import me.asu.http.client.Request;
import me.asu.http.client.Response;
import me.asu.http.client.Sender;

public class PostSender extends Sender {

    public PostSender(Request request) {
        super(request);
    }

    @Override
    public Response send() throws HttpException {
        try {
            openConnection();
            InputStream ins = request.getInputStream();
            setupRequestHeader();
            if (ins != null && request.getHeader() != null && ins instanceof ByteArrayInputStream
                    && this.request.getHeader().get("Content-Length") == null) {
                conn.addRequestProperty("Content-Length", "" + ins.available());
            }
            setupDoInputOutputFlag();
            if (null != ins) {
                OutputStream ops = conn.getOutputStream();
                Streams.write(ops, ins, 8192);
                Streams.safeClose(ins);
                Streams.safeFlush(ops);
                Streams.safeClose(ops);
            }
            return createResponse(getResponseHeader());
        } catch (Exception e) {
            throw new HttpException(request.getUrl().toString(), e);
        }
    }


}