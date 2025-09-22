package me.asu.http;

import junit.framework.TestCase;
import me.asu.log.Log;
import org.junit.Test;

import java.util.*;

public class OKJSONTest extends TestCase {
    @Test
    public void testOKJSON() throws Exception {
        String json = "{\"a\": \"string\", \"b\": 123, \"c\": true, \"d\": [\"a1\", \"a2\", \"a3\"], \"e\": [1,2,3,4]}";
        Map<String, Object> m = OKJSON.toMap(json, OKJSON.OPTIONS_DIRECT_ACCESS_PROPERTY_ENABLE);
        Log.info("Map: " + m);
        assertEquals("string", m.get("a"));
        m.put("Z", null);
        m.put("Y", new Integer[0]);
        m.put("X", Collections.emptyList());
        Log.info("m = "+m);
        String str1 = OKJSON.stringify(m, OKJSON.OPTIONS_DIRECT_ACCESS_PROPERTY_ENABLE | OKJSON.OPTIONS_PRETTY_FORMAT_ENABLE);
        Log.info("pretty: " + str1);
        String str2 = OKJSON.stringify(m, OKJSON.OPTIONS_DIRECT_ACCESS_PROPERTY_ENABLE );
        Log.info("compact: " + str2);
        
        List list = new ArrayList();
        list.add("string");
        list.add("123");
        list.add("true");
        list.add(Arrays.asList("1", "2", "3"));
        list.add(new int[]{1,2,3,4});
        list.add(new HashMap(){{put("a","string");}});
        String listJson = OKJSON.stringify(list, OKJSON.OPTIONS_DIRECT_ACCESS_PROPERTY_ENABLE | OKJSON.OPTIONS_PRETTY_FORMAT_ENABLE);
        Log.info(listJson);

        String stringify = OKJSON.stringify(new int[]{1,2,3,4}, OKJSON.OPTIONS_PRETTY_FORMAT_ENABLE);
        Log.info("array: " + stringify);
        Log.info("list: " +  OKJSON.stringify(Arrays.asList(4,3,2,1,0), OKJSON.OPTIONS_PRETTY_FORMAT_ENABLE));
        String arrStr = "[1,2,3,4, \"sdfas\", {\"a\": \"string\", \"b\": 123, \"c\": true, \"d\": [\"a1\", \"a2\", \"a3\"], \"e\": [1,2,3,4]}]";
        List listObj = OKJSON.toJson(arrStr, List.class, OKJSON.OPTIONS_PRETTY_FORMAT_ENABLE);
        Log.info("Object: " + listObj);
        for (Object o : listObj) {
            Log.info(o.getClass().getName() + ": " + o);
        }
        Log.info(OKJSON.stringify(listObj,  OKJSON.OPTIONS_PRETTY_FORMAT_ENABLE));
    }
}