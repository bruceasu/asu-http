package me.asu.http;

import lombok.Getter;
import org.w3c.dom.Document;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static me.asu.http.Bytes.CRLF;
import static me.asu.http.Bytes.getBytes;
import static me.asu.http.HeaderKey.*;
import static me.asu.http.Headers.readHeaders;
import static me.asu.http.Streams.*;
import static me.asu.http.Strings.*;

/**
 * {@code Request} 类封装了一个单独的 HTTP 请求。
 */
@Getter
public class Request {

    protected String                       method;
    protected URI                          uri;
    protected URL                          baseURL; // cached value
    protected int                          version;
    protected Headers                      headers;
    protected Headers                      trailers;
    protected InputStream                  body;
    protected Socket                       socket;
//    protected Map<String, String>          params; // cached value
    protected HTTPServer.ContextInfo       context; // cached value
    protected HTTPServer                   server;
    protected List<MultipartIterator.Part> files = new ArrayList<>();

    /**
     * query data
     */
    protected ParamMap paramMap = new ParamMap();

    /**
     * form data
     */
    protected ParamMap bodyMap = new ParamMap();

    /**
     * xml or json parse result
     */
    protected Map<String, Object> dataMap = new HashMap<>();

    public boolean isMultipartFormData() {
        String ct = contentType();
        return (ct != null && ct.startsWith(CommonContentType.FORM_DATA.type()));
    }

    public boolean isForm() {
        String ct = contentType();
        return (ct != null && ct.toLowerCase(Locale.US).startsWith(CommonContentType.FORM.type()));
    }

    public boolean isJson() {
        String ct = contentType();
        return (ct != null && ct.startsWith(CommonContentType.JSON.type()));
    }

    public boolean isXml() {
        String contentType = contentType();
        return (contentType != null && contentType.startsWith(CommonContentType.XML.type()));
    }

    public String contentType() {
        return headers.get(CONTENT_TYPE);
    }

    public String getParameter(String param) {
        String parameter = paramMap.getParameter(param);
        if (Strings.isEmpty(parameter)) {
            // try data map
            parameter = getBodyMap().getParameter(param);
        }
        return parameter;
    }

    public Set<String> getParameters(String param) {
        List<String> list1 = paramMap.getAll(param);
        List<String> list2 = bodyMap.getAll(param);
        Set<String>  set   = new HashSet<>();
        if (list1 != null && !list1.isEmpty()) set.addAll(list1);
        if (list2 != null && !list2.isEmpty()) set.addAll(list2);
        return set;
    }

    private void initRequestParam() {
        // getQuery: 采用UTF-8编码解码%xx%xx%xx的参数信息。
        // getRawQuery: 直接返回原始数据
        List<String[]> params = new ArrayList<>(4);
        context = getContext().getContext(getPath(), 0, false, 0, params); // path params
        params.addAll(parseParameters(uri.getRawQuery())); // query params
        for (String[] param : params) {
            String key = param[0];
            String val = param[1];
            paramMap.setParameter(key, val);
        }

    }

    public String getString() {
        String charset = headers.getParams(CONTENT_TYPE).get("charset");
        try {
            return readToken(body, -1, charset == null ? "UTF-8" : charset, 8192);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getString(String charset) {
        try {
            return readToken(body, -1, charset == null ? "UTF-8" : charset, 8192);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void initFormParam() throws IOException {
        String ct = headers.get(CONTENT_TYPE); // body params
        if (ct != null && ct.toLowerCase(Locale.US).startsWith(CommonContentType.FORM.type())) {

        }
        final List<String[]> strings = parseParameters(readToken(body, -1, "UTF-8", 2097152));
        for (String[] string : strings) {
            String key = string[0];
            String val = string[1];
            bodyMap.setParameter(key, val);
        }
    }

    public <T> T getJson(Class<T> clazz) throws IOException {
       return OKJSON.stringToObject( getString(), clazz, OKJSON.OPTIONS_DIRECT_ACCESS_PROPERTY_ENABLE ) ;
    }

    public Document getXmlDocument() throws IOException {
        return ParseXMLUtils.parseXmlString(getString());
    }

    public Map<String, Object> getXmlBodyMap() throws IOException {
        return ParseXMLUtils.string2Map(getString());
    }

    public void initBody() throws IOException {
        if (isForm()) {
            initFormParam();
        } else if (isMultipartFormData()) {
            final MultipartIterator multipartIterator = new MultipartIterator(this);
            while (multipartIterator.hasNext()) {
                final MultipartIterator.Part next = multipartIterator.next();
                // todo: parse the data
                if (next.type == MultipartIterator.Part.FILE) {
                    next.writeToTempFile();
                    files.add(next);
                } else {
                    dataMap.put(next.name, next.getString());
                }
            }
        } else {
            // a text data, maybe is json or xml, use getString() to get the content or getJosn(class) to an object.
            // Max length is 8192, I don't believe a text request is larger than 8 kb.
            // If upload a file, should be use multipart/form-data request.
        }

    }

    public void cleanup() {
        if (!files.isEmpty()) {
            for (MultipartIterator.Part file : files) {
                try {
                    Files.deleteIfExists(file.getPath());
                } catch (IOException e) {
                    // ignored
                }
            }
        }
    }

    /**
     * 从给定输入流中的数据构造请求。
     *
     * @param in     请求读取的输入流
     * @param socket 连接的底层套接字
     * @throws IOException 如果发生错误
     */
    public Request(HTTPServer server, InputStream in, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        readRequestLine(in);
        headers = readHeaders(in);

        // [RFC9112#6.3] 如果存在传输编码，则会覆盖内容长度。
        // 如果“分块”编码是最终的编码形式，则它决定了主体的长度；
        // 否则，我们必须返回错误并关闭连接。
        // 如果不存在传输编码，则使用内容长度来确定长度。
        // 如果没有传输编码且没有内容长度，则表示没有主体。
        // [RFC9110#8.4] “标识”传输编码不再有效。
        String header = headers.get(TRANSFER_ENCODING);
        if (header != null) {
            List<String> encodings = Arrays.asList(splitElements(header, true));
            if (encodings.isEmpty() || !encodings.get(encodings.size() - 1).equals("chunked"))
                throw new IOException("final transfer encoding must be \"chunked\"");
            trailers = new Headers();
            body = new ChunkedInputStream(in, trailers); // [RFC9110#6.5] separate trailers from headers
        } else {
            header = headers.get(CONTENT_LENGTH);
            long len = header == null ? 0 : parseULong(header, 10);
            body = new LimitedInputStream(in, len, true);
        }
        initRequestParam();
        initBody();
    }

    /**
     * 返回请求URI的路径组件，在应用了URL解码（使用UTF-8字符集）之后。
     *
     * @return 请求URI的解码路径组件
     */
    public String getPath() {
        return uri.getPath();
    }

    /**
     * 设置请求 URI 的路径组件。这在 URL 重写等场景中可能非常有用。
     *
     * @param path 要设置的路径
     * @throws IllegalArgumentException 如果给定的路径格式不正确
     */
    public void setPath(String path) {
        try {
            uri = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(),
                    trimDuplicates(path, '/'), uri.getQuery(), uri.getFragment());
            context = null; // 清除缓存的上下文，以便重新进行计算。
            paramMap.clear();
            initRequestParam();
        } catch (URISyntaxException use) {
            throw new IllegalArgumentException("error setting path", use);
        }
    }

    /**
     * 返回请求资源的基本 URL（协议、主机和端口）。
     * 主机名取自请求 URI 或者 Host 头部，或默认主机（详见 RFC9110#7.1, RFC9112#3.3）。
     *
     * @return 请求资源的基本 URL，如果它格式不正确则返回 null
     */
    public URL getBaseURL() {
        if (baseURL != null) return baseURL;
        // [RFC9112#3.2.2] 也接受绝对网址，其优先于主机设置。
        String host = uri.getHost();
        if (host == null) {
            host = headers.get("Host");
            if (host == null) // 在HTTP/1.0中缺失。
                host = detectLocalHostName();
        }
        int pos = host.indexOf(':'); // 忽略请求端口并使用真实端口。
        host = pos < 0 ? host : host.substring(0, pos);
        try { // [RFC9112#3.3] 使用配置中指定的“https”或“http”协议。
            return baseURL = new URL(server.secure ? "https" : "http", host, server.port, "");
        } catch (MalformedURLException mue) {
            return null;
        }
    }

    /**
     * 返回本地主机自动检测到的名称。
     *
     * @return 本地主机名称
     */
    public static String detectLocalHostName() {
        try {
            return InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException uhe) {
            return "localhost";
        }
    }

    /**
     * 修剪给定字符串中连续重复出现的指定字符，
     * 将其替换为该字符的单个实例。
     *
     * @param s 要修剪的字符串
     * @param c 要修剪的字符
     * @return 经修剪后，给定字符串中连续重复出现的字符 c
     * 被替换为单个实例的字符串
     */
    public static String trimDuplicates(String s, char c) {
        int start = 0;
        while ((start = s.indexOf(c, start) + 1) > 0) {
            int end;
            for (end = start; end < s.length() && s.charAt(end) == c; end++) ;
            if (end > start)
                s = s.substring(0, start) + s.substring(end);
        }
        return s;
    }

    /**
     * 从给定的“x-www-form-urlencoded” MIME 类型字符串解析名称-值对参数。
     * 这种编码用于通过 HTTP GET 方法传递的查询参数，
     * 以及以 HTTP POST 方法提交的 HTML 表单内容（只要它们在 ENCTYPE 属性中使用默认的
     * “application/x-www-form-urlencoded” 编码）。
     * 假定使用 UTF-8 编码。
     * <p>
     * 参数将作为字符串数组的列表返回，每个数组的第一个元素为参数名称，
     * 第二个元素为其对应的值（如果没有值则为空字符串）。
     * <p>
     * 该列表保持参数的原始顺序。
     *
     * @param s 一个“application/x-www-form-urlencoded”字符串
     * @return 从给定字符串解析出的参数名称-值对，
     * 或者如果没有则返回一个空列表
     */
    public static List<String[]> parseParameters(String s) {
        if (s == null || s.length() == 0)
            return Collections.emptyList();
        List<String[]> params = new ArrayList<>(8);
        for (String pair : split(s, "&", -1)) {
            int    pos  = pair.indexOf('=');
            String name = pos < 0 ? pair : pair.substring(0, pos);
            String val  = pos < 0 ? "" : pair.substring(pos + 1);
            try {
                name = URLDecoder.decode(name.trim(), "UTF-8");
                val = URLDecoder.decode(val.trim(), "UTF-8");
                if (name.length() > 0)
                    params.add(new String[]{name, val});
            } catch (UnsupportedEncodingException ignore) {} // never thrown
        }
        return params;
    }

    /**
     * 返回从 Range 头部读取的绝对（零起始）内容范围值。
     * 如果请求多个范围，将返回一个包含所有范围的单一范围。
     * 对于除 GET 方法以外的方法，应忽略 Range 头部。
     *
     * @param length 请求资源的完整长度
     * @return 请求范围的起始和结束位置（包括），
     * 如果 Range 头部缺失或无效，则返回 null
     */
    public long[] getRange(long length) {
        String header = headers.get("Range"); // [RFC9110#14.1] case-insensitive units
        return header == null || !header.toLowerCase(Locale.US).startsWith("bytes=")
                ? null : parseRange(header.substring(6), length);
    }

    /**
     * 返回由给定范围字符串指定的绝对（以零为基数）内容范围值。如果请求多个范围，
     * 则它们将合并为一个包含所有范围的单一范围。
     *
     * @param range  包含范围描述的字符串
     * @param length 请求资源的总长度
     * @return 返回请求的范围（仅在其起始位置小于长度时满足），
     * 如果范围值无效或整个资源应发送，则返回null
     */
    public static long[] parseRange(String range, long length) {
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        try {
            for (String token : splitElements(range, false)) {
                long start;
                long end  = length - 1;
                int  dash = token.indexOf('-');
                if (dash == 0) { // suffix range
                    long i = parseULong(token.substring(1), 10);
                    start = length < i ? 0 : length - i;
                    if (length == 0 && i > 0)
                        return null;
                } else { // full range or open-ended range
                    start = parseULong(token.substring(0, dash), 10);
                    if (dash < token.length() - 1) { // full range
                        long i = parseULong(token.substring(dash + 1), 10);
                        end = i < end ? i : end;
                        if (i < start)
                            throw new RuntimeException();
                    }
                }
                if (start < min)
                    min = start;
                if (end > max)
                    max = end;
            }
            return max < -1 ? null : new long[]{min, max}; // 最小值不能大于等于长度
        } catch (RuntimeException re) {  // NFE、IOOBE 或显式 RE
            return null; // [RFC9110#14.2] 如果范围请求头不合法，则忽略该请求头。
        }
    }

    /**
     * 读取请求行，解析方法、URI 和版本字符串。
     *
     * @param in 从中读取请求行的输入流
     * @throws IOException 如果发生错误或请求行无效
     */
    protected void readRequestLine(InputStream in) throws IOException {
        // [RFC9112#2.2] accept empty lines before request line
        // [RFC9112#3] tolerate additional whitespace between tokens
        int     c;
        byte[]  b     = null;
        int     i     = 0;
        int     len   = 0;
        int     token = 0; // number of tokens parsed
        boolean query = false; // true when reaching URL query or fragment
        try {
            while ((c = in.read()) != -1) {
                if (c > ' ') { // append char to token
                    if (i == len) { // buffer is full
                        if (i >= 8192)
                            throw new IOException(token == 1 ? "URI too long" : "request line too long");
                        len = len == 0 ? 128 : 2 * len; // double capacity
                        byte[] temp = new byte[len]; // lazy init since connection may close between requests
                        if (b != null)
                            System.arraycopy(b, 0, temp, 0, i);
                        b = temp;
                    }
                    // if path is a relative uri, we must remove "//" prefix which URI parses as host name
                    // we also merge repeated slashes in path as is common practice (although not required)
                    if (c == '?' || c == '#') // don't merge slashes in query or fragment
                        query = true;
                    if (c != '/' || i == 0 || b[i - 1] != '/' || b[0] != '/' || query)
                        b[i++] = (byte) c;
                } else { // whitespace delimiter
                    if (i > 0) { // the end of a non-empty token
                        if (token == 0) { // method
                            method = new String(b, 0, i, "ISO8859_1");
                        } else if (token == 1) { // uri
                            this.uri = new URI(new String(b, 0, i, "ISO8859_1"));
                        } else if (token == 2) { // version
                            if (i != 8 || b[0] != 'H' || b[1] != 'T' || b[2] != 'T' || b[3] != 'P' || b[4] != '/'
                                    || b[6] != '.' || b[5] < '0' || b[5] > '9' || b[7] < '0' || b[7] > '9')
                                throw new IOException("invalid version");
                            version = 10 * (b[5] - '0') + (b[7] - '0'); // parse as 2-digit integer
                        }
                        i = 0; // start next token
                        token++;
                    }
                    if (c == '\n' && token > 0) { // end of request line (unless it's empty)
                        if (token == 3) // got our 3 valid tokens
                            return;
                        throw new IOException("invalid request line"); // wrong number of tokens
                    }
                }
            }
            throw new EOFException("unexpected end of stream");
        } catch (URISyntaxException use) {
            throw new IOException("invalid URI: " + use.getMessage());
        } catch (IOException ioe) {
            if (i > 0 || token > 0) // if already started parsing request
                throw ioe; // rethrow exception to send error response
            throw new IOException("missing request line"); // otherwise, close connection without response
        }
    }

    /**
     * 返回处理此请求的上下文信息。
     *
     * @return 处理此请求的上下文信息，或一个空上下文
     */
    public HTTPServer.ContextInfo getContext() {
        return context != null ? context : (context = server.rootContext);
    }

    /**
     * {@code ChunkedInputStream} 负责解码已应用 "chunked" 传输编码的数据流，
     * <p>
     * Transfer-Encoding: chunked
     * 并提供其底层数据。
     */
    static class ChunkedInputStream extends LimitedInputStream {

        protected Headers headers;
        protected boolean initialized;

        /**
         * 构造一个具有给定底层流的分块输入流，并提供一个头部容器，
         * 以便将流的尾部头信息添加到该容器中。
         *
         * @param in      底层的“分块”编码输入流
         * @param headers 头部容器，流的尾部头信息将被添加到此容器中，
         *                若要丢弃这些头信息，则为null
         * @throws NullPointerException 若给定的流为null
         */
        public ChunkedInputStream(InputStream in, Headers headers) {
            super(in, 0, true);
            this.headers = headers;
        }

        @Override
        public int read() throws IOException {
            return limit <= 0 && initChunk() < 0 ? -1 : super.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return limit <= 0 && initChunk() < 0 ? -1 : super.read(b, off, len);
        }

        /**
         * 初始化下一个数据块。如果前一个数据块尚未结束，
         * 或者已达到流的末尾，则不执行任何操作。
         *
         * @return 数据块的长度，如果已达到流的末尾，则返回 -1
         * @throws IOException 如果发生 IO 错误或流损坏
         */
        protected long initChunk() throws IOException {
            if (limit == 0) { // finished previous chunk
                // read chunk-terminating CRLF if it's not the first chunk
                if (initialized && readLine(in).length() > 0)
                    throw new IOException("chunk data must end with CRLF");
                initialized = true;
                limit = parseChunkSize(readLine(in)); // read next chunk size
                if (limit == 0) { // last chunk has size 0
                    limit = -1; // mark end of stream
                    // read trailing headers, if any
                    Headers trailingHeaders = readHeaders(in);
                    if (headers != null)
                        headers.addAll(trailingHeaders);
                }
            }
            return limit;
        }

        /**
         * 解析一个块大小行。
         *
         * @param line 要解析的块大小行
         * @return 块大小
         * @throws IllegalArgumentException 如果块大小行无效
         */
        protected static long parseChunkSize(String line) throws IllegalArgumentException {
            int pos = line.indexOf(';'); // [RFC9112#7.1.1] ignore extensions and whitespace
            line = pos < 0 ? line : line.substring(0, pos).trim();
            try {
                return parseULong(line, 16); // throws NFE
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException(
                        "invalid chunk size line: \"" + line + "\"");
            }
        }
    }

    /**
     * The {@code LimitedInputStream} provides access to a limited number
     * of consecutive bytes from the underlying InputStream, starting at its
     * current position. If this limit is reached, it behaves as though the end
     * of stream has been reached (although the underlying stream remains open
     * and may contain additional data).
     */
    static class LimitedInputStream extends FilterInputStream {

        protected long    limit; // decremented when read, until it reaches zero
        protected boolean prematureEndException;

        /**
         * Constructs a LimitedInputStream with the given underlying
         * input stream and limit.
         *
         * @param in                    the underlying input stream
         * @param limit                 the maximum number of bytes that may be consumed from
         *                              the underlying stream before this stream ends. If zero or
         *                              negative, this stream will be at its end from initialization.
         * @param prematureEndException specifies the stream's behavior when
         *                              the underlying stream end is reached before the limit is
         *                              reached: if true, an exception is thrown, otherwise this
         *                              stream reaches its end as well (i.e. read() returns -1)
         * @throws NullPointerException if the given stream is null
         */
        public LimitedInputStream(InputStream in, long limit, boolean prematureEndException) {
            super(in);
            if (in == null)
                throw new NullPointerException("input stream is null");
            this.limit = limit < 0 ? 0 : limit;
            this.prematureEndException = prematureEndException;
        }

        @Override
        public int read() throws IOException {
            int res = limit == 0 ? -1 : in.read();
            if (res < 0 && limit > 0 && prematureEndException)
                throw new IOException("unexpected end of stream");
            limit = res < 0 ? 0 : limit - 1;
            return res;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int res = limit == 0 ? -1 : in.read(b, off, len > limit ? (int) limit : len);
            if (res < 0 && limit > 0 && prematureEndException)
                throw new IOException("unexpected end of stream");
            limit = res < 0 ? 0 : limit - res;
            return res;
        }

        @Override
        public long skip(long len) throws IOException {
            long res = in.skip(len > limit ? limit : len);
            limit -= res;
            return res;
        }

        @Override
        public int available() throws IOException {
            int res = in.available();
            return res > limit ? (int) limit : res;
        }

        @Override
        public boolean markSupported() {
            return false;
        }

        @Override
        public void close() {
            limit = 0; // end this stream, but don't close the underlying stream
        }
    }

    /**
     * {@code MultipartIterator} 用于迭代 multipart/form-data 请求的各个部分。
     * <p>
     * 例如，为了支持从网络浏览器上传文件：
     * <ol>
     * <li>创建一个 HTML 表单，该表单包含一个类型为 “file” 的输入字段， 属性为 method="post" 和 enctype="multipart/form-data"，
     * 并指定一个您选择的操作 URL，
     * 例如 action="/upload"。该表单可以像其他任何资源一样正常提供，例如通过硬盘上的 HTML 文件。
     * <li>为操作路径（在本示例中为 "/upload"）添加上下文处理程序，
     * 可以使用显式的 {@link HTTPServer#addContext} 方法或 {@link Context} 注释进行添加。
     * </ol>
     */
    static class MultipartIterator implements Iterator<MultipartIterator.Part> {

        /**
         * The {@code Part} class encapsulates a single part of the multipart.
         */
        @Getter
        public static class Part {
            public static final int         FORM = 0;
            public static final int         FILE = 1;
            public              String      name;
            public              String      filename;
            public              Headers     headers;
            public              InputStream body;
            public              int         type = Part.FORM; // 0 : form, 1: file
            public              String      contentType;
            public              Path        path;

            /***
             * 以字符串形式返回部分的主体。如果该部分的
             * 头信息未指定字符集，则使用UTF-8编码。
             *
             * @return 该部分的主体以字符串形式返回
             * @throws IOException 如果发生IO错误
             */
            public String getString() throws IOException {
                String charset = headers.getParams(CONTENT_TYPE).get("charset");
                return readToken(body, -1, charset == null ? "UTF-8" : charset, 8192);
            }

            public byte[] getBytes() throws IOException {
                return Bytes.toByteArray(body);
            }

            public void writeToTempFile() throws IOException {
                path = Files.createTempFile("temp-file-", ".dat");
                path.toFile().deleteOnExit();
                try(OutputStream os = new FileOutputStream(path.toFile())) {
                    transfer(body, os, -1);
                }
            }
        }

        protected final MultipartInputStream in;
        protected       boolean              next;

        /**
         * Creates a new MultipartIterator from the given request.
         *
         * @param req the multipart/form-data request
         * @throws IOException              if an IO error occurs
         * @throws IllegalArgumentException if the given request's content type
         *                                  is not multipart/form-data, or is missing the boundary
         */
        public MultipartIterator(Request req) throws IOException {
            Map<String, String> ct = req.getHeaders().getParams(CONTENT_TYPE);
            if (!ct.containsKey(MULTIPART_FORM_DATA))
                throw new IllegalArgumentException("Content-Type is not multipart/form-data");
            String boundary = ct.get(BOUNDARY); // should be US-ASCII
            if (boundary == null)
                throw new IllegalArgumentException("Content-Type is missing boundary");
            in = new MultipartInputStream(req.getBody(), getBytes(boundary));
        }

        @Override
        public boolean hasNext() {
            try {
                return next || (next = in.nextPart());
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }

        @Override
        public Part next() {
            if (!hasNext())
                throw new NoSuchElementException();
            next = false;
            Part p = new Part();
            try {
                p.headers = readHeaders(in);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
            Map<String, String> cd = p.headers.getParams(CONTENT_DISPOSITION);
            p.name = cd.get("name");
            p.filename = cd.get("filename");
            p.body = in;
            if (!isEmpty(p.filename)) {
                p.type = Part.FILE;
            }
            return p;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * {@code MultipartInputStream} 解码一种内容类型为 "multipart/*" 的 InputStream（如 RFC 2046 中所定义），
     * 提供其各个部分的基础数据。
     * <p>
     * {@code InputStream} 方法（例如 {@link #read}）仅与当前部分相关，而 {@link #nextPart} 方法则用于
     * 向下一个部分的开头推进。
     */
    static class MultipartInputStream extends FilterInputStream {

        protected final byte[] boundary; // including leading CRLF--
        protected final byte[] buf = new byte[4096];
        protected       int    head, tail; // indices of current part's data in buf
        protected int end; // last index of input data read into buf
        protected int len; // length of found boundary
        protected int state; // initial, started data, start boundary, EOS, last boundary, epilogue

        /**
         * Constructs a MultipartInputStream with the given underlying stream.
         * <pre>
         * POST /t2/upload.do HTTP/1.1
         * User-Agent: SOHUWapRebot
         * Accept-Language: zh-cn,zh;q=0.5
         * Accept-Charset: GBK,utf-8;q=0.7,*;q=0.7
         * Connection: keep-alive
         * Content-Length: 60408
         * Content-Type:multipart/form-data; boundary=--ZnGpDtePMx0KrHh_G0X99Yef9r8JZsRJSXC
         * Host: localhost
         * <p>
         * --ZnGpDtePMx0KrHh_G0X99Yef9r8JZsRJSXC
         * Content-Disposition: form-data;name=”desc”
         * Content-Type: text/plain; charset=UTF-8
         * Content-Transfer-Encoding: 8bit
         * <p>
         * [……][……][……][……]………………………
         * --ZnGpDtePMx0KrHh_G0X99Yef9r8JZsRJSXC
         * Content-Disposition: form-data;name=”pic”; filename=”photo.jpg”
         * Content-Type: application/octet-stream
         * Content-Transfer-Encoding: binary
         * <p>
         * [图片二进制数据]
         * --ZnGpDtePMx0KrHh_G0X99Yef9r8JZsRJSXC--
         * </pre>
         *
         * @param in       the underlying multipart stream
         * @param boundary the multipart boundary
         * @throws NullPointerException     if the given stream or boundary is null
         * @throws IllegalArgumentException if the given boundary's size is not
         *                                  between 1 and 70
         */
        public MultipartInputStream(InputStream in, byte[] boundary) {
            super(in);
            int len = boundary.length;
            if (len == 0 || len > 70)
                throw new IllegalArgumentException("invalid boundary length");
            this.boundary = new byte[len + 4]; // CRLF--boundary
            System.arraycopy(CRLF, 0, this.boundary, 0, 2);
            this.boundary[2] = this.boundary[3] = '-';
            System.arraycopy(boundary, 0, this.boundary, 4, len);
        }

        @Override
        public int read() throws IOException {
            if (!fill())
                return -1;
            return buf[head++] & 0xFF;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (!fill())
                return -1;
            len = Math.min(tail - head, len);
            System.arraycopy(buf, head, b, off, len); // throws IOOBE as necessary
            head += len;
            return len;
        }

        @Override
        public long skip(long len) throws IOException {
            if (len <= 0 || !fill())
                return 0;
            len = Math.min(tail - head, len);
            head += len;
            return len;
        }

        @Override
        public int available() throws IOException {
            return tail - head;
        }

        @Override
        public boolean markSupported() {
            return false;
        }

        /**
         * 将流的位置向前推进至下一部分的开始。
         * 在第一次调用此方法之前读取的数据为前言，
         * 在此方法返回false后读取的数据为尾声。
         *
         * @return 如果成功则返回true，如果没有更多部分则返回false
         * @throws IOException 如果发生错误则抛出该异常
         */
        public boolean nextPart() throws IOException {
            while (skip(buf.length) != 0) ; // skip current part (until boundary)
            head = tail += len; // the next part starts right after boundary
            state |= 1; // started data (after first boundary)
            if (state >= 8) { // found last boundary
                state |= 0x10; // now beyond last boundary (epilogue)
                return false;
            }
            findBoundary(); // update indices
            return true;
        }

        /**
         * 从底层流中填充缓冲区以获取更多数据。
         *
         * @return 如果当前部分有可用数据，则返回真；如果当前部分的结束已达到，则返回假
         * @throws IOException 如果发生错误或输入格式无效
         */
        protected boolean fill() throws IOException {
            // check if we already have more available data
            if (head != tail) // remember that if we continue, head == tail below
                return true;
            // if there's no more room, shift extra unread data to beginning of buffer
            if (tail > buf.length - 256) { // max boundary + whitespace supported size
                System.arraycopy(buf, tail, buf, 0, end -= tail);
                head = tail = 0;
            }
            // read more data and look for boundary (or potential partial boundary)
            int read;
            do {
                read = super.read(buf, end, buf.length - end);
                if (read < 0)
                    state |= 4; // end of stream (EOS)
                else
                    end += read;
                findBoundary(); // updates tail and length to next potential boundary
                // if we found a partial boundary with no data before it, we must
                // continue reading to determine if there is more data or not
            } while (read > 0 && tail == head && len == 0);
            // update and validate state
            if (tail != 0) // anything but a boundary right at the beginning
                state |= 1; // started data (preamble or after boundary)
            if (state < 8 && len > 0)
                state |= 2; // found start boundary
            if ((state & 6) == 4 // EOS but no start boundary found
                    || len == 0 && ((state & 0xFC) == 4 // EOS but no last and no more boundaries
                    || read == 0 && tail == head)) // boundary longer than buffer
                throw new IOException("missing boundary");
            if (state >= 0x10) // in epilogue
                tail = end; // ignore boundaries, return everything
            return tail > head; // available data in current part
        }

        /**
         * 在缓冲区剩余数据中寻找第一个（潜在的）边界。
         * 相应地更新尾部、长度和状态字段。
         *
         * @throws IOException 如果发生错误或输入格式无效
         */
        protected void findBoundary() throws IOException {
            // see RFC2046#5.1.1 for boundary syntax
            len = 0;
            int off = tail - ((state & 1) != 0 || buf[0] != DASH ? 0 : 2); // skip initial CRLF?
            for (int end = this.end; tail < end; tail++, off = tail) {
                int j = tail; // end of potential boundary
                // try to match boundary value (leading CRLF is optional at first boundary)
                while (j < end && j - off < boundary.length && buf[j] == boundary[j - off])
                    j++;
                // return potential partial boundary which is cut off at end of current data
                if (j + 1 >= end) // at least two more chars needed for full boundary (CRLF or --)
                    return;
                // if we found the boundary value, expand selection to include full line
                if (j - off == boundary.length) {
                    // check if last boundary of entire multipart
                    if (buf[j] == DASH && buf[j + 1] == DASH) {
                        j += 2;
                        state |= 8; // found last boundary that ends multipart
                    }
                    // allow linear whitespace after boundary
                    while (j < end && (buf[j] == SPC || buf[j] == TAB))
                        j++;
                    // check for CRLF (required, except in last boundary with no epilogue)
                    if (j + 1 < end && buf[j] ==  0x0D && buf[j + 1] == 0x0A) // found CRLF
                        len = j - tail + 2; // including optional whitespace and CRLF
                    else if (j + 1 < end || (state & 4) != 0 && j + 1 == end) // should have found or never will
                        throw new IOException("boundary must end with CRLF");
                    else if ((state & 4) != 0) // last boundary with no CRLF at end of data is valid
                        len = j - tail;
                    return;
                }
            }
        }

        /**
         * The Carriage Return ASCII character value.
         */
        public static final byte CR = 0x0D;

        /**
         * The Line Feed ASCII character value.
         */
        public static final byte LF = 0x0A;

        /**
         * The \t ASCII character value.
         */
        public static final byte TAB = 0x09;

        /**
         * The space  ASCII character value.
         */
        public static final byte SPC = 0x20;
        /**
         * The dash (-) ASCII character value.
         */
        public static final byte DASH = 0x2D;

    }


}