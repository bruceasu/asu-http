package me.asu.http;

/**
 * {@code Header}类封装了一个单独的HTTP头部。
 */
public class Header extends Pair<String, String>{


    /**
     * 构造一个带有指定名称和值的头部。
     * 会去除前导和尾随空格。
     *
     * @param name  头部名称
     * @param value 头部值
     * @throws NullPointerException     如果名称或值为 null
     * @throws IllegalArgumentException 如果名称为空
     */
    public Header(String name, String value) {
        super(name, value);
        if (this.key.length() == 0) // [RFC9110#5.1] name cannot be empty
            throw new IllegalArgumentException("name cannot be empty");
    }

    /**
     * 返回此标题的名称。
     *
     * @return 此标题的名称
     */
    public String getName() {return key;}

    /**
     * 返回该头部的值。
     *
     * @return 该头部的值
     */
    public String getValue() {return value;}
}
