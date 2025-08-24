package me.asu.http;

import junit.framework.TestCase;
import org.junit.Test;

import java.util.*;

public class OKJSONTest extends TestCase {
    @Test
    public void testOKJSON() throws Exception {
        String json = "{\"a\": \"string\", \"b\": 123, \"c\": true, \"d\": [\"a1\", \"a2\", \"a3\"], \"e\": [1,2,3,4]}";
        Map<String, Object> m = OKJSON.toMap(json, OKJSON.OPTIONS_DIRECT_ACCESS_PROPERTY_ENABLE);
        System.out.println(m);
        assertEquals("string", m.get("a"));
        m.put("Z", null);
        m.put("Y", new Integer[0]);
        m.put("X", Collections.emptyList());
        System.out.println("m = "+m);
        String str1 = OKJSON.stringify(m, OKJSON.OPTIONS_DIRECT_ACCESS_PROPERTY_ENABLE | OKJSON.OPTIONS_PRETTY_FORMAT_ENABLE);
        System.out.println(str1);
        String str2 = OKJSON.stringify(m, OKJSON.OPTIONS_DIRECT_ACCESS_PROPERTY_ENABLE );
        System.out.println(str2);
        
        List list = new ArrayList();
        list.add("string");
        list.add("123");
        list.add("true");
        list.add(Arrays.asList("1", "2", "3"));
        list.add(new int[]{1,2,3,4});
        list.add(new HashMap(){{put("a","string");}});
        String listJson = OKJSON.stringify(list, OKJSON.OPTIONS_DIRECT_ACCESS_PROPERTY_ENABLE | OKJSON.OPTIONS_PRETTY_FORMAT_ENABLE);
        System.out.println(listJson);

        int[] arr = new int[]{1,2,3,4};
        String stringify = OKJSON.stringify(arr, OKJSON.OPTIONS_PRETTY_FORMAT_ENABLE);
        System.out.println(stringify);
    }
}