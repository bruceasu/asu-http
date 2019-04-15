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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class Header {

    private Header() {
        items = new HashMap<String, String>();
    }

    private Map<String, String> items;

    public Collection<String> keys() {
        return items.keySet();
    }

    public String get(String key) {
        return items.get(key);
    }

    public Header set(String key, String value) {
        if (null != key) {
            items.put(key, value);
        }
        return this;
    }

    public Header remove(String key) {
        items.remove(key);
        return this;
    }

    public Header clear() {
        items.clear();
        return this;
    }

    public Set<Entry<String, String>> getAll() {
        return items.entrySet();
    }

    public Header addAll(Map<String, String> map) {
        if (null != map) {
            items.putAll(map);
        }
        return this;
    }



    public static Header create(Map<String, String> properties) {
        return new Header().addAll(properties);
    }


    public static Header create() {
        Header header = new Header();
        header.set("User-Agent", "Nutz.Robot");
        header.set("Accept-Encoding", "gzip,deflate");
        header.set("Accept", "text/xml,application/xml,application/xhtml+xml,text/html;"
                + "q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
        header.set("Accept-Language", "en-US,en,zh,zh-CN");
        header.set("Accept-Charset", "ISO-8859-1,*,utf-8");
        header.set("Connection", "keep-alive");
        header.set("Cache-Control", "max-age=0");
        return header;
    }

    public String get(String key, String defaultValue) {
        String value = get(key);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    public int getInt(String key, int defaultValue) {
        String value = get(key);
        if (value == null) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }
}
