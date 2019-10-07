package me.asu.http.common;

public class HeaderKey {
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String LAST_MODIFIED = "Last-Modified";
    public static final String REFERER = "Referer";
    public static final String USER_AGENT = "User-Agent";
    public static final String IF_MODIFIED_SINCE = "If-Modified-Since";
    public static final String ETAG = "Etag";
    /**
     * HTTP content disposition header name.
     */
    public static final String CONTENT_DISPOSITION = "Content-disposition";

    /**
     * HTTP content length header name.
     */
    public static final String CONTENT_LENGTH = "Content-length";

    /**
     * Content-disposition value for form data.
     */
    public static final String FORM_DATA = "form-data";

    /**
     * Content-disposition value for file attachment.
     */
    public static final String ATTACHMENT = "attachment";

    /**
     * Part of HTTP content type header.
     */
    public static final String MULTIPART = "multipart/";

    /**
     * HTTP content type header for multipart forms.
     */
    public static final String MULTIPART_FORM_DATA = "multipart/form-data";

    /**
     * HTTP content type header for multiple uploads.
     */
    public static final String MULTIPART_MIXED = "multipart/mixed";

}
