package xyz.calvinwilliams.okjson;


public class OkJsonException extends RuntimeException {
    Integer code;
    String desc;

    public OkJsonException(String message, Integer code, String desc)
    {
        super(message);
        this.code = code;
        this.desc = desc;
    }

    public OkJsonException(String message, Throwable cause, Integer code, String desc)
    {
        super(message, cause);
        this.code = code;
        this.desc = desc;
    }

    public OkJsonException(Throwable cause, Integer code, String desc)
    {
        super(cause);
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode()
    {
        return code;
    }

    public void setCode(Integer code)
    {
        this.code = code;
    }

    public String getDesc()
    {
        return desc;
    }

    public void setDesc(String desc)
    {
        this.desc = desc;
    }
}
