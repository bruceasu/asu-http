package me.asu.http.common;

public enum CommonContentType {

    FORM("application/x-www-form-urlencoded"),
    FORM_DATA("multipart/form-data"),
    JSON("application/json"),
    XML("application/xml"),

    ;

    private String type;

    CommonContentType(String type) {
        this.type = type;
    }

    public String type() {
        return this.type;
    }
}
