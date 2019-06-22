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

package me.asu.http.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class Cookie {

    protected Map<String, String> map;

    public Cookie() {
        map = new HashMap<String, String>();
    }

    public Cookie(String s) {
        this();
        parse(s);
    }

    public String get(String name) {
        return map.get(name);
    }

    public Cookie remove(String name) {
        map.remove(name);
        return this;
    }

    public Cookie set(String name, String value) {
        map.put(name, value);
        return this;
    }

    public void parse(String str) {
        String[] ss = str.split(";");
        for (String s : ss) {
            String[] p = s.split("=]");
            if (p.length == 1) {
                map.put(p[0], "");
            } else {
                map.put(p[0], p[1]);
            }
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Iterator<Map.Entry<String, String>> it = map.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, String> entry = it.next();
            sb.append(entry.getKey()).append('=').append(entry.getValue());
            if (it.hasNext())
                sb.append("; ");
        }
        return sb.toString();
    }

}
