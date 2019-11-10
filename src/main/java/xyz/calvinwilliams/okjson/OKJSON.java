/*
 * okjson - A small efficient flexible JSON parser/generator for Java
 * author	: calvin
 * email	: calvinwilliams@163.com
 *
 * See the file LICENSE in base directory.
 */

package xyz.calvinwilliams.okjson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class OKJSON {

    public static final int OPTIONS_DIRECT_ACCESS_PROPERTY_ENABLE = 1;
    public static final int OPTIONS_PRETTY_FORMAT_ENABLE          = 2;
    public static final int OPTIONS_STRICT_POLICY                 = 4;

    public static final int OKJSON_ERROR_END_OF_BUFFER                     = OkJsonParser.OKJSON_ERROR_END_OF_BUFFER;
    public static final int OKJSON_ERROR_UNEXPECT                          = OkJsonParser.OKJSON_ERROR_UNEXPECT;
    public static final int OKJSON_ERROR_EXCEPTION                         = OkJsonParser.OKJSON_ERROR_EXCEPTION;
    public static final int OKJSON_ERROR_INVALID_BYTE                      = OkJsonParser.OKJSON_ERROR_INVALID_BYTE;
    public static final int OKJSON_ERROR_FIND_FIRST_LEFT_BRACE             = OkJsonParser.OKJSON_ERROR_FIND_FIRST_LEFT_BRACE;
    public static final int OKJSON_ERROR_NAME_INVALID                      = OkJsonParser.OKJSON_ERROR_NAME_INVALID;
    public static final int OKJSON_ERROR_EXPECT_COLON_AFTER_NAME           = OkJsonParser.OKJSON_ERROR_EXPECT_COLON_AFTER_NAME;
    public static final int OKJSON_ERROR_UNEXPECT_TOKEN_AFTER_LEFT_BRACE   = OkJsonParser.OKJSON_ERROR_UNEXPECT_TOKEN_AFTER_LEFT_BRACE;
    public static final int OKJSON_ERROR_PORPERTY_TYPE_NOT_MATCH_IN_OBJECT = OkJsonParser.OKJSON_ERROR_PORPERTY_TYPE_NOT_MATCH_IN_OBJECT;
    public static final int OKJSON_ERROR_NAME_NOT_FOUND_IN_OBJECT          = OkJsonParser.OKJSON_ERROR_NAME_NOT_FOUND_IN_OBJECT;
    public static final int OKJSON_ERROR_NEW_OBJECT                        = OkJsonParser.OKJSON_ERROR_NEW_OBJECT;

    public static int objectToFile(Object object, String filePath, int options)
    {
        String jsonString = objectToString(object, options);
        try {
            Files.write(Paths.get(filePath), jsonString.getBytes(), StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            return 0;
        } catch (IOException e) {
            return -1;
        }
    }

    public static String objectToString(Object object)
    {
        return objectToString(object, 0);
    }

    public static String objectToString(Object object, int options)
    {
        OkJsonGenerator okjsonGenerator = new OkJsonGenerator();

        if ((options & OPTIONS_DIRECT_ACCESS_PROPERTY_ENABLE) != 0) {
            okjsonGenerator.setDirectAccessPropertyEnable(true);
        } else {
            okjsonGenerator.setDirectAccessPropertyEnable(false);
        }

        if ((options & OPTIONS_PRETTY_FORMAT_ENABLE) != 0) {
            okjsonGenerator.setPrettyFormatEnable(true);
        } else {
            okjsonGenerator.setPrettyFormatEnable(false);
        }

        String string = okjsonGenerator.objectToString(object);

        if (string == null) {
            throw new OkJsonException(
                    "Stringify error",
                    okjsonGenerator.getErrorCode(),
                    okjsonGenerator.getErrorDesc());
        }
        return string;
    }

    public static <T> T fileToObject(String filePath, Class<T> clazz, int options)
    {
        String jsonString = null;

        try {
            jsonString = new String(Files.readAllBytes(Paths.get(filePath)));
        } catch (IOException e) {
            return null;
        }

        return stringToObject(jsonString, clazz, options);
    }

    public static <T> T stringToObject(String jsonString, Class<T> clazz, int options)
    {
        OkJsonParser okjsonParser = new OkJsonParser();

        if ((options & OPTIONS_DIRECT_ACCESS_PROPERTY_ENABLE) != 0) {
            okjsonParser.setDirectAccessPropertyEnable(true);
        } else {
            okjsonParser.setDirectAccessPropertyEnable(false);
        }
        if ((options & OPTIONS_STRICT_POLICY) != 0) {
            okjsonParser.setStrictPolicyEnable(true);
        } else {
            okjsonParser.setStrictPolicyEnable(false);
        }

        T object;
        try {
            object = clazz.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        object = okjsonParser.stringToObject(jsonString, object);
        if (object == null) {
            throw new OkJsonException(
                    "Parse error",
                    okjsonParser.getErrorCode(),
                    okjsonParser.getErrorDesc());
        }
        return object;
    }
}




