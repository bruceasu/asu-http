package me.asu.http.server;

import static me.asu.http.common.HeaderKey.ATTACHMENT;
import static me.asu.http.common.HeaderKey.CONTENT_DISPOSITION;
import static me.asu.http.common.HeaderKey.CONTENT_TYPE;
import static me.asu.http.common.HeaderKey.FORM_DATA;

import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.util.*;
import lombok.Data;
import me.asu.http.common.HeaderKey;
import me.asu.util.Bytes;
import me.asu.util.Strings;

public class MultipartRequest extends HttpRequest{

    /**
     * The Carriage Return ASCII character value.
     */
    public static final byte CR = 0x0D;

    /**
     * The Line Feed ASCII character value.
     */
    public static final byte LF = 0x0A;

    /**
     * The dash (-) ASCII character value.
     */
    public static final byte DASH = 0x2D;

    /**
     * The maximum length of <code>header-part</code> that will be
     * processed (10 kilobytes = 10240 bytes.).
     */
    public static final int HEADER_PART_SIZE_MAX = 10240;

    /**
     * The default length of the buffer used for processing a request.
     */
    protected static final int DEFAULT_BUFSIZE = 4096;

    /**
     * A byte sequence that marks the end of <code>header-part</code>
     * (<code>CRLFCRLF</code>).
     */
    protected static final byte[] HEADER_SEPARATOR = {CR, LF, CR, LF};

    /**
     * A byte sequence that that follows a delimiter that will be
     * followed by an encapsulation (<code>CRLF</code>).
     */
    protected static final byte[] FIELD_SEPARATOR = {CR, LF};

    /**
     * A byte sequence that that follows a delimiter of the last
     * encapsulation in the stream (<code>--</code>).
     */
    protected static final byte[] STREAM_TERMINATOR = {DASH, DASH};

    /**
     * A byte sequence that precedes a boundary (<code>CRLF--</code>).
     */
    protected static final byte[] BOUNDARY_PREFIX = {CR, LF, DASH, DASH};

    List<MultipartItem> items = new ArrayList<>();
    byte[] boundary;
    byte[] endBoundary;
    byte[] spBoundary;
    public MultipartRequest(HttpExchange httpExchange) {
        super(httpExchange);
        String contentType = getHeadMap().getValue(HeaderKey.CONTENT_TYPE);
        boundary = getBoundary(contentType);
    }

    @Override
    public void initBody() {
        if (boundary == null) {
            throw new IllegalStateException("Not a MultipartRequest");
        }
        spBoundary = new byte[boundary.length + 2];
        System.arraycopy(boundary, 0, endBoundary, 2, boundary.length);
        System.arraycopy(STREAM_TERMINATOR, 0, endBoundary, 0, 2);

        endBoundary = new byte[spBoundary.length + 2];
        System.arraycopy(spBoundary, 0, endBoundary, 0, spBoundary.length);
        System.arraycopy(STREAM_TERMINATOR, 0, endBoundary, spBoundary.length,2);
        parse();
    }

    /**
     * Retrieves the boundary from the <code>Content-type</code> header.
     *
     * @param contentType The value of the content type header from which to
     *                    extract the boundary value.
     *
     * @return The boundary, as a byte array.
     */
    protected byte[] getBoundary(String contentType) {
        ParameterParser parser = new ParameterParser();
        parser.setLowerCaseNames(true);
        // Parameter parser can handle null input
        Map<String, String> params = parser.parse(contentType, new char[] {';', ','});
        String boundaryStr = params.get("boundary");

        if (boundaryStr == null) {
            return null;
        }
        byte[] boundary;
        try {
            boundary = boundaryStr.getBytes("ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            boundary = boundaryStr.getBytes(); // Intentionally falls back to default charset
        }
        return boundary;
    }

    private String getFileName(Map<String, String> headers) {
        return getFileName(headers.get(CONTENT_DISPOSITION));
    }

    /**
     * Returns the given content-disposition headers file name.
     * @param pContentDisposition The content-disposition headers value.
     * @return The file name
     */
    private String getFileName(String pContentDisposition) {
        String fileName = null;
        if (pContentDisposition != null) {
            String cdl = pContentDisposition.toLowerCase(Locale.ENGLISH);
            if (cdl.startsWith(FORM_DATA) || cdl.startsWith(ATTACHMENT)) {
                ParameterParser parser = new ParameterParser();
                parser.setLowerCaseNames(true);
                // Parameter parser can handle null input
                Map<String, String> params = parser.parse(pContentDisposition, ';');
                if (params.containsKey("filename")) {
                    fileName = params.get("filename");
                    if (fileName != null) {
                        fileName = fileName.trim();
                    } else {
                        // Even if there is no value, the parameter is present,
                        // so we return an empty file name rather than no file
                        // name.
                        fileName = "";
                    }
                }
            }
        }
        return fileName;
    }

    /**
     * Retrieves the field name from the <code>Content-disposition</code>
     * header.
     *
     * @param headers A <code>Map</code> containing the HTTP request headers.
     *
     * @return The field name for the current <code>encapsulation</code>.
     */
    private String getFieldName(Map<String, String> headers) {
        return getFieldName(headers.get(CONTENT_DISPOSITION));
    }

    /**
     * Returns the field name, which is given by the content-disposition
     * header.
     * @param pContentDisposition The content-dispositions header value.
     * @return The field jake
     */
    private String getFieldName(String pContentDisposition) {
        String fieldName = null;
        if (pContentDisposition != null
                && pContentDisposition.toLowerCase(Locale.ENGLISH).startsWith(FORM_DATA)) {
            ParameterParser parser = new ParameterParser();
            parser.setLowerCaseNames(true);
            // Parameter parser can handle null input
            Map<String, String> params = parser.parse(pContentDisposition, ';');
            fieldName = params.get("name");
            if (fieldName != null) {
                fieldName = fieldName.trim();
            }
        }
        return fieldName;
    }

    /**
     * POST /t2/upload.do HTTP/1.1
     * User-Agent: SOHUWapRebot
     * Accept-Language: zh-cn,zh;q=0.5
     * Accept-Charset: GBK,utf-8;q=0.7,*;q=0.7
     * Connection: keep-alive
     * Content-Length: 60408
     * Content-Type:multipart/form-data; boundary=--ZnGpDtePMx0KrHh_G0X99Yef9r8JZsRJSXC
     * Host: www.111cn.net
     *
     * --ZnGpDtePMx0KrHh_G0X99Yef9r8JZsRJSXC
     * Content-Disposition: form-data;name=”desc”
     * Content-Type: text/plain; charset=UTF-8
     * Content-Transfer-Encoding: 8bit
     *
     * [……][……][……][……]………………………
     * --ZnGpDtePMx0KrHh_G0X99Yef9r8JZsRJSXC
     * Content-Disposition: form-data;name=”pic”; filename=”photo.jpg”
     * Content-Type: application/octet-stream
     * Content-Transfer-Encoding: binary
     *
     * [图片二进制数据]
     * --ZnGpDtePMx0KrHh_G0X99Yef9r8JZsRJSXC--
     */
    private void parse() {
        try ( InputStream is = getHttpExchange().getRequestBody();
              BufferedInputStream bis = new BufferedInputStream(is);) {
            byte[] buffer = new byte[1024];
            int length = 0;
            OutputStream os = null;
            int step = 0;
            ByteArrayOutputStream currentBuffer = new ByteArrayOutputStream();
            length = loadContent(bis, currentBuffer);
            if (length == -1) {
                // 没数据
                return;
            }
            // 开始
            byte[] bytes = skipPreamble(currentBuffer);
            step = 1;
            MultipartItem current = new MultipartItem();
            while (true) {
                // 处理 headers
                if (step == 1) {
                    int index = findIndx(bytes, HEADER_SEPARATOR);
                    if (index == -1) {
                        // 头部还每有结束
                        currentBuffer.reset();
                        currentBuffer.write(bytes);
                        length = loadContent(bis, currentBuffer);
                        if (length == -1) {
                            // 没有数据了
                            return;
                        }
                        bytes = currentBuffer.toByteArray();
                        continue;
                    } else {
                        bytes = processHeaders(bytes, index, current);
                        step = 2;
                    }
                }

                if (step ==2 ) {
                    int index = findIndx(bytes, spBoundary);
                    boolean last = findIndx(bytes, endBoundary) != -1;

                    if (index == -1) {
                        currentBuffer.reset();
                        currentBuffer.write(bytes);
                        length = loadContent(bis, currentBuffer);
                        if (length == -1) {
                            // 没有数据了
                            return;
                        }
                        continue;
                    } else {
                        bytes = processBody(bytes, index, current);
                        current = new MultipartItem();
                        // new part
                        step = 1;
                        if(last) {
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private byte[] processBody(byte[] bytes, int index, MultipartItem current) {
        byte[] origin = bytes;
        byte[] body = new byte[index];
        bytes = new byte[bytes.length - index - boundary.length];

        System.arraycopy(origin, 0, body, 0, body.length);
        System.arraycopy(origin, index + boundary.length , bytes, 0, bytes.length);
        // process body
        if (current.getType() == MultipartItem.FORM) {
            current.setValue(Bytes.toString(body));
        } else {
            current.setContent(body);
        }
        items.add(current);
        return bytes;
    }

    private byte[] processHeaders(byte[] bytes, int index, MultipartItem current) {
        byte[] origin = bytes;
        byte[] headers = new byte[index-2]; // 开头是\r\n ，删除
        bytes = new byte[bytes.length - index - 4];
        System.arraycopy(origin, 2, headers, 0, headers.length);
        System.arraycopy(origin, index + 4, bytes, 0, bytes.length);
        // process headers
        createHeader(current, headers);

        String fieldName = getFieldName(current.getHeaders());
        current.setName(fieldName);

        String fileName = getFileName(current.getHeaders());
        if (Strings.isNotEmpty(fileName)) {
            current.setType(MultipartItem.FILE);
            current.setFileName(fileName);
            String s = current.getHeaders().get(CONTENT_TYPE);
            current.setCountentType(s);
        }
        return bytes;
    }

    private byte[] skipPreamble(ByteArrayOutputStream currentBuffer) {
        byte[] bytes = currentBuffer.toByteArray();
        int idx = findIndx(bytes, boundary);
        if (idx == -1) {
            throw new IllegalStateException("Not a MultipartRequest");
        } else {
            int off = idx + boundary.length;
            int len = bytes.length - off;
            byte[] origin = bytes;
            bytes = new byte[len];
            System.arraycopy(origin, off, bytes, 0, len);
        }
        return bytes;
    }

    private void createHeader(MultipartItem current, byte[] headers) {
        ByteArrayOutputStream b= new ByteArrayOutputStream();
        for (int i = 0; i < headers.length; i++) {
            if (headers[i] == CR) {
                if (i < headers.length - 2 && headers[i+1] == LF) {
                    String s = b.toString();
                    String[] split = s.split(":");
                    current.getHeaders().put(split[0].trim(), split[1].trim());
                    b.reset();
                }
            }
        }
        if (b.size() > 0) {
            String s = b.toString();
            String[] split = s.split(":");
            current.getHeaders().put(split[0].trim(), split[1].trim());
            b.reset();
        }
    }

    private int loadContent(BufferedInputStream bis, OutputStream os) throws IOException {
        byte[] buffer = new byte[1024];
        int length = 0;
        length = bis.read(buffer);
        if (length != -1) {
            os.write(buffer, 0, length);
        }
        return length;
    }


    /**
     * 获取某串byte数组在原byte数组的位置
     *
     * @param source 原byte数组
     * @param part   某串byte数组
     * @return -1 未找到  其他大于等于0的值
     */
    int findIndx(byte[] source, byte[] part) {
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

    /**
     * Compares <code>count</code> first bytes in the arrays
     * <code>a</code> and <code>b</code>.
     *
     * @param a     The first array to compare.
     * @param b     The second array to compare.
     * @param count How many bytes should be compared.
     *
     * @return <code>true</code> if <code>count</code> first bytes in arrays
     *         <code>a</code> and <code>b</code> are equal.
     */
    public static boolean arrayequals(byte[] a,
                                      byte[] b,
                                      int count) {
        for (int i = 0; i < count; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }
    @Data
    public static class MultipartItem {
        public static final int FORM = 0;
        public static final int FILE = 1;
        public Map<String, String> headers = new HashMap<>();
        public String name;
        public String fileName;
        public String value;
        public byte[] content;
        public int type = MultipartItem.FORM; // 0 : form, 1: file
        public String countentType;
    }
}
