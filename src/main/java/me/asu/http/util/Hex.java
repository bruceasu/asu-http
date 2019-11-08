/*
 * Copyright (c) 2017 Suk Honzeon
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.asu.http.util;

/**
 * Hex Tool.
 * <p>2017 Suk All rights reserved.</p>
 *
 * @author Suk
 * @version 1.0.0
 * @since 2017-09-11 17:44
 */

import java.io.UnsupportedEncodingException;

public class Hex {

    public static final  String DEFAULT_CHARSET_NAME = "UTF-8";
    private static final char[] DIGITS_LOWER         = {'0', '1', '2', '3', '4', '5', '6', '7', '8',
            '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    private static final char[] DIGITS_UPPER         = {'0', '1', '2', '3', '4', '5', '6', '7', '8',
            '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    private final String charsetName;

    public Hex() {
        this.charsetName = "UTF-8";
    }

    public Hex(String csName) {
        this.charsetName = csName;
    }

    public byte[] decode(byte[] array) {
        try {
            return decodeHex(new String(array, getCharsetName()).toCharArray());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static byte[] decodeHex(char[] data) {
        int len = data.length;

        if ((len & 0x1) != 0) {
            throw new IllegalStateException("Odd number of characters.");
        }

        byte[] out = new byte[len >> 1];

        int i = 0;
        for (int j = 0; j < len; ++i) {
            int f = toDigit(data[j], j) << 4;
            ++j;
            f |= toDigit(data[j], j);
            ++j;
            out[i] = (byte) (f & 0xFF);
        }

        return out;
    }

    public String getCharsetName() {
        return this.charsetName;
    }

    protected static int toDigit(char ch, int index) {
        int digit = Character.digit(ch, 16);
        if (digit == -1) {
            throw new IllegalStateException(
                    String.format("Illegal hexadecimal charcter %s at index %d",
                            new Object[]{Character.valueOf(ch), Integer.valueOf(index)}));
        }
        return digit;
    }

    public Object decode(Object object) {
        try {
            char[] charArray = (object instanceof String) ? ((String) object).toCharArray()
                    : (char[]) object;
            return decodeHex(charArray);
        } catch (ClassCastException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public byte[] encode(byte[] array) {
        return getBytesUnchecked(encodeHexString(array), getCharsetName());
    }

    public byte[] getBytesUnchecked(String string, String charsetName) {
        if (string == null) {
            return null;
        }
        try {
            return string.getBytes(charsetName);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static String encodeHexString(byte[] data, int start, int end) {
        if (start < 0 || end > data.length) {
            throw new IndexOutOfBoundsException();
        }
        byte[] newData = new byte[end - start];
        System.arraycopy(data, start, newData, start, (end - start));
        return encodeHexString(newData);
    }

    public static String encodeHexString(byte[] data) {
        return new String(encodeHex(data));
    }

    public static char[] encodeHex(byte[] data) {
        return encodeHex(data, true);
    }

    public static char[] encodeHex(byte[] data, boolean toLowerCase) {
        return encodeHex(data, (toLowerCase) ? DIGITS_LOWER : DIGITS_UPPER);
    }

    protected static char[] encodeHex(byte[] data, char[] toDigits) {
        int l = data.length;
        char[] out = new char[l << 1];

        int i = 0;
        for (int j = 0; i < l; ++i) {
            out[(j++)] = toDigits[((0xF0 & data[i]) >>> 4)];
            out[(j++)] = toDigits[(0xF & data[i])];
        }
        return out;
    }

    public Object encode(Object object) {
        try {
            byte[] byteArray = (object instanceof String) ? ((String) object)
                    .getBytes(getCharsetName()) : (byte[]) (byte[]) object;
            return encodeHex(byteArray);
        } catch (ClassCastException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public String toString() {
        return super.toString() + "[charsetName=" + this.charsetName + "]";
    }
}
