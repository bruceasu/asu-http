package me.asu.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

public class Bytes {
    /** A convenience array containing the carriage-return and line feed chars. */
    public static final    byte[]              CRLF         = { 0x0d, 0x0a };

    public Bytes() {
    }

    public static void main(String[] args) {
        int x = 16909060;
        byte[] bytes = toBytes(x);
        String ssBefore = Arrays.toString(bytes);
        System.out.println("ssBefore = " + ssBefore);
        ByteBuffer bb = ByteBuffer.wrap(new byte[4]);
        bb.asIntBuffer().put(x);
        String ssBefore2 = Arrays.toString(bb.array());
        System.out.println("ssBefore2 = " + ssBefore2);
        ByteBuffer allocate = ByteBuffer.allocate(4);
        allocate.put(bytes).clear();
        IntBuffer intBuffer = allocate.asIntBuffer();
        int i = intBuffer.get();
        System.out.println("String.valueOf(i, 16) = " + Integer.toHexString(i));
    }

    /**
     * Converts strings to bytes by casting the chars to bytes.
     * This is a fast way to encode a string as ISO-8859-1/US-ASCII bytes.
     * If multiple strings are provided, their bytes are concatenated.
     *
     * @param strings the strings to convert (containing only ISO-8859-1 chars)
     * @return the byte array
     */
    public static byte[] getBytes(String... strings) {
        int n = 0;
        for (String s : strings)
            n += s.length();
        byte[] b = new byte[n];
        n = 0;
        for (String s : strings)
            for (int i = 0, len = s.length(); i < len; i++)
                b[n++] = (byte)s.charAt(i);
        return b;
    }

    public static byte[] toBytes(int n) {
        byte[] b = new byte[]{(byte) (n >> 24 & 255), (byte) (n >> 16 & 255), (byte) (n >> 8 & 255),
                (byte) (n & 255)};
        return b;
    }

    public static byte[] toBytes(String str) {
        return toBytes(str, "utf-8");
    }

    public static byte[] toBytes(String str, String charset) {
        if (isEmpty(str)) {
            return new byte[0];
        } else {
            try {
                return str.getBytes(charset);
            } catch (Exception var3) {
                var3.printStackTrace();
                return new byte[0];
            }
        }
    }

    public static byte[] toBytes(String str, Charset charset) {
        if (isEmpty(str)) {
            return new byte[0];
        } else {
            try {
                return str.getBytes(charset);
            } catch (Exception var3) {
                var3.printStackTrace();
                return new byte[0];
            }
        }
    }

    /**
     * 快速判断是否是空串
     *
     * @param str 文本
     * @return true or false
     */
    public static boolean isEmpty(Object str) {
        return (str == null || "".equals(str.toString().trim()));
    }

    public static String toString(byte[] bytes) {
        if (bytes == null) {
            return "";
        } else {
            try {
                return new String(bytes, "utf-8");
            } catch (UnsupportedEncodingException var2) {
                var2.printStackTrace();
                return "";
            }
        }
    }

    public static byte[] toBytes(long n) {
        byte[] b = new byte[]{(byte) ((int) (n >> 56 & 255L)), (byte) ((int) (n >> 48 & 255L)),
                (byte) ((int) (n >> 40 & 255L)), (byte) ((int) (n >> 32 & 255L)),
                (byte) ((int) (n >> 24 & 255L)), (byte) ((int) (n >> 16 & 255L)),
                (byte) ((int) (n >> 8 & 255L)), (byte) ((int) (n & 255L))};
        return b;
    }

    public static void toBytes(long n, byte[] array, int offset) {
        array[7 + offset] = (byte) ((int) (n & 255L));
        array[6 + offset] = (byte) ((int) (n >> 8 & 255L));
        array[5 + offset] = (byte) ((int) (n >> 16 & 255L));
        array[4 + offset] = (byte) ((int) (n >> 24 & 255L));
        array[3 + offset] = (byte) ((int) (n >> 32 & 255L));
        array[2 + offset] = (byte) ((int) (n >> 40 & 255L));
        array[1 + offset] = (byte) ((int) (n >> 48 & 255L));
        array[0 + offset] = (byte) ((int) (n >> 56 & 255L));
    }

    public static long toLong(byte[] array) {
        return ((long) array[0] & 255L) << 56 | ((long) array[1] & 255L) << 48
                | ((long) array[2] & 255L) << 40 | ((long) array[3] & 255L) << 32
                | ((long) array[4] & 255L) << 24 | ((long) array[5] & 255L) << 16
                | ((long) array[6] & 255L) << 8 | ((long) array[7] & 255L) << 0;
    }

    public static long toLong(byte[] array, int offset) {
        return ((long) array[offset + 0] & 255L) << 56 | ((long) array[offset + 1] & 255L) << 48
                | ((long) array[offset + 2] & 255L) << 40 | ((long) array[offset + 3] & 255L) << 32
                | ((long) array[offset + 4] & 255L) << 24 | ((long) array[offset + 5] & 255L) << 16
                | ((long) array[offset + 6] & 255L) << 8 | ((long) array[offset + 7] & 255L) << 0;
    }

    public static void toBytes(int n, byte[] array, int offset) {
        array[3 + offset] = (byte) (n & 255);
        array[2 + offset] = (byte) (n >> 8 & 255);
        array[1 + offset] = (byte) (n >> 16 & 255);
        array[offset] = (byte) (n >> 24 & 255);
    }

    public static int toInt(byte[] b) {
        return b[3] & 255 | (b[2] & 255) << 8 | (b[1] & 255) << 16 | (b[0] & 255) << 24;
    }

    public static int toInt(byte[] b, int offset) {
        return b[offset + 3] & 255 | (b[offset + 2] & 255) << 8 | (b[offset + 1] & 255) << 16
                | (b[offset] & 255) << 24;
    }

    public static byte[] toBytes(short n) {
        byte[] b = new byte[]{(byte) (n >> 8 & 255), (byte) (n & 255)};
        return b;
    }

    public static void toBytes(short n, byte[] array, int offset) {
        array[offset + 1] = (byte) (n & 255);
        array[offset] = (byte) (n >> 8 & 255);
    }

    public static short toShort(byte[] b) {
        return (short) (b[1] & 255 | (b[0] & 255) << 8);
    }

    public static short toShort(byte[] b, int offset) {
        return (short) (b[offset + 1] & 255 | (b[offset] & 255) << 8);
    }

    public static byte[] uintToBytes(long n) {
        byte[] b = new byte[]{(byte) ((int) (n >> 24 & 255L)), (byte) ((int) (n >> 16 & 255L)),
                (byte) ((int) (n >> 8 & 255L)), (byte) ((int) (n & 255L))};
        return b;
    }

    public static void uintToBytes(long n, byte[] array, int offset) {
        array[3 + offset] = (byte) ((int) n);
        array[2 + offset] = (byte) ((int) (n >> 8 & 255L));
        array[1 + offset] = (byte) ((int) (n >> 16 & 255L));
        array[offset] = (byte) ((int) (n >> 24 & 255L));
    }

    public static long bytesToUint(byte[] array) {
        return (long) (array[3] & 255) | (long) (array[2] & 255) << 8
                | (long) (array[1] & 255) << 16 | (long) (array[0] & 255) << 24;
    }

    public static long bytesToUint(byte[] array, int offset) {
        return (long) (array[offset + 3] & 255) | (long) (array[offset + 2] & 255) << 8
                | (long) (array[offset + 1] & 255) << 16 | (long) (array[offset] & 255) << 24;
    }

    public static byte[] ushortToBytes(int n) {
        byte[] b = new byte[]{(byte) (n >> 8 & 255), (byte) (n & 255)};
        return b;
    }

    public static void ushortToBytes(int n, byte[] array, int offset) {
        array[offset + 1] = (byte) (n & 255);
        array[offset] = (byte) (n >> 8 & 255);
    }

    public static int bytesToUshort(byte[] b) {
        return b[1] & 255 | (b[0] & 255) << 8;
    }

    public static int bytesToUshort(byte[] b, int offset) {
        return b[offset + 1] & 255 | (b[offset] & 255) << 8;
    }

    public static byte[] ubyteToBytes(int n) {
        byte[] b = new byte[]{(byte) (n & 255)};
        return b;
    }

    public static void ubyteToBytes(int n, byte[] array, int offset) {
        array[0] = (byte) (n & 255);
    }

    public static int bytesToUbyte(byte[] array) {
        return array[0] & 255;
    }

    public static int bytesToUbyte(byte[] array, int offset) {
        return array[offset] & 255;
    }

    public static byte[] toByteArray(InputStream input) throws IOException {
        if (input != null && input.available() != 0) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            boolean n = false;

            int n1;
            while (-1 != (n1 = input.read(buffer))) {
                baos.write(buffer, 0, n1);
            }

            return baos.toByteArray();
        } else {
            return null;
        }
    }

    /**
     * 比较数组<code>a</code>和<code>b</code>中前<code>count</code>个字节。
     *
     * @param a     要比较的第一个数组。
     * @param b     要比较的第二个数组。
     * @param count 要比较的字节数。
     * @return 如果数组<code>a</code>和<code>b</code>中的前<code>count</code>个字节相等，则返回<code>true</code>。
     */
    public static boolean arrayequals(byte[] a, byte[] b, int count) {
        for (int i = 0; i < count; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取某串byte数组在原byte数组的位置
     *
     * @param source 原byte数组
     * @param part   某串byte数组
     * @return -1 未找到  其他大于等于0的值
     */
    public static int findIndex(byte[] source, byte[] part) {
        if (source == null || part == null || source.length == 0 || part.length == 0) {
            return -1;
        }
        int i, j;
        for (i = 0; i < source.length; i++) {
            if (source[i] == part[0]) {
                for (j = 0; j < part.length; j++) {
                    if (source[i + j] != part[j]) {
                        break;
                    }
                }
                if (j == part.length) {
                    return i;
                }
            }
        }
        return -1;
    }
}
