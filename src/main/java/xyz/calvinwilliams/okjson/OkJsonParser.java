package xyz.calvinwilliams.okjson;

import java.io.IOException;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

class OkJsonParser {

    static final int OKJSON_ERROR_END_OF_BUFFER                     = 1;
    static final int OKJSON_ERROR_UNEXPECT                          = -4;
    static final int OKJSON_ERROR_EXCEPTION                         = -8;
    static final int OKJSON_ERROR_INVALID_BYTE                      = -11;
    static final int OKJSON_ERROR_FIND_FIRST_LEFT_BRACE             = -21;
    static final int OKJSON_ERROR_NAME_INVALID                      = -22;
    static final int OKJSON_ERROR_EXPECT_COLON_AFTER_NAME           = -23;
    static final int OKJSON_ERROR_UNEXPECT_TOKEN_AFTER_LEFT_BRACE   = -24;
    static final int OKJSON_ERROR_PORPERTY_TYPE_NOT_MATCH_IN_OBJECT = -26;
    static final int OKJSON_ERROR_NAME_NOT_FOUND_IN_OBJECT          = -28;
    static final int OKJSON_ERROR_NEW_OBJECT                        = -31;

    private static HashMap<String, HashMap<String, Field>>  stringMapFieldsCache    = new HashMap<>();
    private static HashMap<String, HashMap<String, Method>> stringMapMethodsCache   = new HashMap<>();
    private        StringBuilder                            fieldStringBuilderCache = new StringBuilder(1024);



    enum TokenType {
        /**
         * {
         */
        TOKEN_TYPE_LEFT_BRACE,
        /**
         * }
         */
        TOKEN_TYPE_RIGHT_BRACE,
        /**
         * [
         */
        TOKEN_TYPE_LEFT_BRACKET,
        /**
         * ]
         */
        TOKEN_TYPE_RIGHT_BRACKET,
        /**
         * :
         */
        TOKEN_TYPE_COLON,
        /**
         * ,
         */
        TOKEN_TYPE_COMMA,
        /**
         * "ABC"
         */
        TOKEN_TYPE_STRING,
        /**
         * 123
         */
        TOKEN_TYPE_INTEGER,
        /**
         * 123.456
         */
        TOKEN_TYPE_DECIMAL,
        /**
         * true or false
         */
        TOKEN_TYPE_BOOL,
        /**
         * null
         */
        TOKEN_TYPE_NULL
    }


    private boolean strictPolicyEnable;
    private boolean directAccessPropertyEnable;
    private boolean prettyFormatEnable;

    private Integer errorCode;
    private String  errorDesc;

    private int jsonOffset;
    private int jsonLength;

    private TokenType tokenType;
    private int       beginOffset;
    private int       endOffset;
    private boolean   booleanValue;

    public OkJsonParser()
    {
        this.strictPolicyEnable = false;
        this.directAccessPropertyEnable = false;
        this.prettyFormatEnable = false;
        this.errorCode = 0;
        this.errorDesc = null;
    }

    public <T> T fileToObject(String filePath, T object)
    {
        String jsonString = null;

        try {
            jsonString = new String(Files.readAllBytes(Paths.get(filePath)));
        } catch (IOException e) {
            return null;
        }

        return stringToObject(jsonString, object);
    }

    public <T> T stringToObject(String jsonString, T object)
    {
        char[] jsonCharArray;

        jsonCharArray = jsonString.toCharArray();
        jsonOffset = 0;
        jsonLength = jsonCharArray.length;

            errorCode = tokenJsonWord(jsonCharArray);
            if (errorCode != 0) {
                return null;
            }

            if (tokenType != TokenType.TOKEN_TYPE_LEFT_BRACE) {
                errorCode = OKJSON_ERROR_FIND_FIRST_LEFT_BRACE;
                return null;
            }

            errorCode = stringToObjectProperties(jsonCharArray, object);
            if (errorCode != 0) {
                return null;
            }


        return object;
    }


    public boolean isStrictPolicyEnable()
    {
        return strictPolicyEnable;
    }

    public void setStrictPolicyEnable(boolean strictPolicyEnable)
    {
        this.strictPolicyEnable = strictPolicyEnable;
    }

    public boolean isDirectAccessPropertyEnable()
    {
        return directAccessPropertyEnable;
    }

    public void setDirectAccessPropertyEnable(boolean directAccessPropertyEnable)
    {
        this.directAccessPropertyEnable = directAccessPropertyEnable;
    }

    public boolean isPrettyFormatEnable()
    {
        return prettyFormatEnable;
    }

    public void setPrettyFormatEnable(boolean prettyFormatEnable)
    {
        this.prettyFormatEnable = prettyFormatEnable;
    }

    public Integer getErrorCode()
    {
        return errorCode;
    }

    public void setErrorCode(Integer errorCode)
    {
        this.errorCode = errorCode;
    }

    public String getErrorDesc()
    {
        return errorDesc;
    }

    public void setErrorDesc(String errorDesc)
    {
        this.errorDesc = errorDesc;
    }


    private int tokenJsonString(char[] jsonCharArray)
    {
        StringBuilder fieldStringBuilder = fieldStringBuilderCache;
        char          ch;

        fieldStringBuilder.setLength(0);

        jsonOffset++;
        beginOffset = jsonOffset;
        while (jsonOffset < jsonLength) {
            ch = jsonCharArray[jsonOffset];
            if (ch == '"') {
                tokenType = TokenType.TOKEN_TYPE_STRING;
                if (jsonOffset > beginOffset) {
                    fieldStringBuilder.append(jsonCharArray, beginOffset, jsonOffset - beginOffset);
                }
                endOffset = jsonOffset - 1;
                jsonOffset++;
                return 0;
            } else if (ch == '\\') {
                jsonOffset++;
                if (jsonOffset >= jsonLength) {
                    return OKJSON_ERROR_END_OF_BUFFER;
                }
                ch = jsonCharArray[jsonOffset];
                if (ch == '"') {
                    if (jsonOffset > beginOffset + 1) {
                        fieldStringBuilder
                                .append(jsonCharArray, beginOffset, jsonOffset - beginOffset - 1);
                    }
                    fieldStringBuilder.append('"');
                    beginOffset = jsonOffset + 1;
                } else if (ch == '\\') {
                    if (jsonOffset > beginOffset + 1) {
                        fieldStringBuilder
                                .append(jsonCharArray, beginOffset, jsonOffset - beginOffset - 1);
                    }
                    fieldStringBuilder.append("\\");
                    beginOffset = jsonOffset + 1;
                } else if (ch == '/') {
                    if (jsonOffset > beginOffset + 1) {
                        fieldStringBuilder
                                .append(jsonCharArray, beginOffset, jsonOffset - beginOffset - 1);
                    }
                    fieldStringBuilder.append('/');
                    beginOffset = jsonOffset + 1;
                } else if (ch == 'b') {
                    if (jsonOffset > beginOffset + 1) {
                        fieldStringBuilder
                                .append(jsonCharArray, beginOffset, jsonOffset - beginOffset - 1);
                    }
                    fieldStringBuilder.append('\b');
                    beginOffset = jsonOffset + 1;
                } else if (ch == 'f') {
                    if (jsonOffset > beginOffset + 1) {
                        fieldStringBuilder
                                .append(jsonCharArray, beginOffset, jsonOffset - beginOffset - 1);
                    }
                    fieldStringBuilder.append('\f');
                    beginOffset = jsonOffset + 1;
                } else if (ch == 'n') {
                    if (jsonOffset > beginOffset + 1) {
                        fieldStringBuilder
                                .append(jsonCharArray, beginOffset, jsonOffset - beginOffset - 1);
                    }
                    fieldStringBuilder.append('\n');
                    beginOffset = jsonOffset + 1;
                } else if (ch == 'r') {
                    if (jsonOffset > beginOffset + 1) {
                        fieldStringBuilder
                                .append(jsonCharArray, beginOffset, jsonOffset - beginOffset - 1);
                    }
                    fieldStringBuilder.append('\r');
                    beginOffset = jsonOffset + 1;
                } else if (ch == 't') {
                    if (jsonOffset > beginOffset + 1) {
                        fieldStringBuilder
                                .append(jsonCharArray, beginOffset, jsonOffset - beginOffset - 1);
                    }
                    fieldStringBuilder.append('\t');
                    beginOffset = jsonOffset + 1;
                } else if (ch == 'u') {
                    if (jsonOffset > beginOffset + 1) {
                        fieldStringBuilder
                                .append(jsonCharArray, beginOffset, jsonOffset - beginOffset - 1);
                    }
                    jsonOffset++;
                    if (jsonOffset >= jsonLength) {
                        return OKJSON_ERROR_END_OF_BUFFER;
                    }
                    ch = jsonCharArray[jsonOffset];
                    if (isNumberChar(ch) || isLetterLower(ch) || isLetterUpper(ch)) {
                        jsonOffset++;
                        if (jsonOffset >= jsonLength) {
                            return OKJSON_ERROR_END_OF_BUFFER;
                        }
                        ch = jsonCharArray[jsonOffset];
                        if ((isNumberChar(ch)) || (isLetterLower(ch)) || (isLetterUpper(ch))) {
                            jsonOffset++;
                            if (jsonOffset >= jsonLength) {
                                return OKJSON_ERROR_END_OF_BUFFER;
                            }
                            ch = jsonCharArray[jsonOffset];
                            if ((isNumberChar(ch)) || (isLetterLower(ch)) || (isLetterUpper(ch))) {
                                jsonOffset++;
                                if (jsonOffset >= jsonLength) {
                                    return OKJSON_ERROR_END_OF_BUFFER;
                                }
                                ch = jsonCharArray[jsonOffset];
                                if ((isNumberChar(ch)) || (isLetterLower(ch)) || (isLetterUpper(
                                        ch))) {
                                    String unicodeString =
                                            "0x" + jsonCharArray[jsonOffset - 3] + jsonCharArray[
                                                    jsonOffset - 2] + jsonCharArray[jsonOffset - 1]
                                                    + jsonCharArray[jsonOffset];
                                    int unicodeInt = Integer.decode(unicodeString).intValue();
                                    if (fieldStringBuilder.length() == 0) {
                                        fieldStringBuilder.append(jsonCharArray, beginOffset,
                                                jsonOffset - 4 - beginOffset - 1);
                                    }
                                    fieldStringBuilder.append((char) unicodeInt);
                                    beginOffset = jsonOffset + 1;
                                } else {
                                    fieldStringBuilder.append(jsonCharArray, beginOffset,
                                            jsonOffset - beginOffset);
                                    beginOffset = jsonOffset;
                                    jsonOffset--;
                                }
                            } else {
                                fieldStringBuilder.append(jsonCharArray, beginOffset,
                                        jsonOffset - beginOffset);
                                beginOffset = jsonOffset;
                                jsonOffset--;
                            }
                        } else {
                            fieldStringBuilder
                                    .append(jsonCharArray, beginOffset, jsonOffset - beginOffset);
                            beginOffset = jsonOffset;
                            jsonOffset--;
                        }
                    } else {
                        fieldStringBuilder
                                .append(jsonCharArray, beginOffset, jsonOffset - beginOffset);
                        beginOffset = jsonOffset;
                        jsonOffset--;
                    }
                } else {
                    fieldStringBuilder
                            .append(jsonCharArray, beginOffset, jsonOffset - beginOffset - 1);
                    fieldStringBuilder.append(ch);
                }
            }

            jsonOffset++;
        }

        return OKJSON_ERROR_END_OF_BUFFER;
    }

    private boolean isLetterUpper(char ch)
    {
        return 'A' <= ch && ch <= 'Z';
    }

    private boolean isLetterLower(char ch)
    {
        return 'a' <= ch && ch <= 'z';
    }

    private boolean isNumberChar(char ch)
    {
        return '0' <= ch && ch <= '9';
    }

    private int tokenJsonNumber(char[] jsonCharArray)
    {
        char    ch;
        boolean decimalPointFlag;

        beginOffset = jsonOffset;

        ch = jsonCharArray[jsonOffset];
        if (ch == '-') {
            jsonOffset++;
        }

        decimalPointFlag = false;
        while (jsonOffset < jsonLength) {
            ch = jsonCharArray[jsonOffset];
            if (isNumberChar(ch)) {
                jsonOffset++;
            } else if (ch == '.') {
                decimalPointFlag = true;
                jsonOffset++;
            } else if (ch == 'e' || ch == 'E') {
                jsonOffset++;
                if (jsonOffset >= jsonLength) {
                    return OKJSON_ERROR_END_OF_BUFFER;
                }
                ch = jsonCharArray[jsonOffset];
                if (ch == '-' || ch == '+') {
                    jsonOffset++;
                } else if (isNumberChar(ch)) {
                    jsonOffset++;
                }
            } else {
                if (decimalPointFlag == true) {
                    tokenType = TokenType.TOKEN_TYPE_DECIMAL;
                } else {
                    tokenType = TokenType.TOKEN_TYPE_INTEGER;
                }
                endOffset = jsonOffset - 1;
                return 0;
            }
        }

        return OKJSON_ERROR_END_OF_BUFFER;
    }

    private int tokenJsonWord(char[] jsonCharArray)
    {
        char ch;

        while (jsonOffset < jsonLength) {
            ch = jsonCharArray[jsonOffset];
            if (ch == ' ' || ch == '\b' || ch == '\t' || ch == '\f' || ch == '\r' || ch == '\n') {
                jsonOffset++;
            } else if (ch == '{') {
                tokenType = TokenType.TOKEN_TYPE_LEFT_BRACE;
                beginOffset = jsonOffset;
                endOffset = jsonOffset;
                jsonOffset++;
                return 0;
            } else if (ch == '}') {
                tokenType = TokenType.TOKEN_TYPE_RIGHT_BRACE;
                beginOffset = jsonOffset;
                endOffset = jsonOffset;
                jsonOffset++;
                return 0;
            } else if (ch == '[') {
                tokenType = TokenType.TOKEN_TYPE_LEFT_BRACKET;
                beginOffset = jsonOffset;
                endOffset = jsonOffset;
                jsonOffset++;
                return 0;
            } else if (ch == ']') {
                tokenType = TokenType.TOKEN_TYPE_RIGHT_BRACKET;
                beginOffset = jsonOffset;
                endOffset = jsonOffset;
                jsonOffset++;
                return 0;
            } else if (ch == '"') {
                return tokenJsonString(jsonCharArray);
            } else if (ch == ':') {
                tokenType = TokenType.TOKEN_TYPE_COLON;
                beginOffset = jsonOffset;
                endOffset = jsonOffset;
                jsonOffset++;
                return 0;
            } else if (ch == ',') {
                tokenType = TokenType.TOKEN_TYPE_COMMA;
                beginOffset = jsonOffset;
                endOffset = jsonOffset;
                jsonOffset++;
                return 0;
            } else if (ch == '-' || (isNumberChar(ch))) {
                return tokenJsonNumber(jsonCharArray);
            } else if (ch == 't') {
                beginOffset = jsonOffset;
                jsonOffset++;
                if (jsonOffset >= jsonLength) {
                    return OKJSON_ERROR_END_OF_BUFFER;
                }
                ch = jsonCharArray[jsonOffset];
                if (ch == 'r') {
                    jsonOffset++;
                    if (jsonOffset >= jsonLength) {
                        return OKJSON_ERROR_END_OF_BUFFER;
                    }
                    ch = jsonCharArray[jsonOffset];
                    if (ch == 'u') {
                        jsonOffset++;
                        if (jsonOffset >= jsonLength) {
                            return OKJSON_ERROR_END_OF_BUFFER;
                        }
                        ch = jsonCharArray[jsonOffset];
                        if (ch == 'e') {
                            tokenType = TokenType.TOKEN_TYPE_BOOL;
                            booleanValue = true;
                            endOffset = jsonOffset;
                            jsonOffset++;
                            return 0;
                        }
                    }
                }
            } else if (ch == 'f') {
                beginOffset = jsonOffset;
                jsonOffset++;
                if (jsonOffset >= jsonLength) {
                    return OKJSON_ERROR_END_OF_BUFFER;
                }
                ch = jsonCharArray[jsonOffset];
                if (ch == 'a') {
                    jsonOffset++;
                    if (jsonOffset >= jsonLength) {
                        return OKJSON_ERROR_END_OF_BUFFER;
                    }
                    ch = jsonCharArray[jsonOffset];
                    if (ch == 'l') {
                        jsonOffset++;
                        if (jsonOffset >= jsonLength) {
                            return OKJSON_ERROR_END_OF_BUFFER;
                        }
                        ch = jsonCharArray[jsonOffset];
                        if (ch == 's') {
                            jsonOffset++;
                            if (jsonOffset >= jsonLength) {
                                return OKJSON_ERROR_END_OF_BUFFER;
                            }
                            ch = jsonCharArray[jsonOffset];
                            if (ch == 'e') {
                                tokenType = TokenType.TOKEN_TYPE_BOOL;
                                booleanValue = false;
                                endOffset = jsonOffset;
                                jsonOffset++;
                                return 0;
                            }
                        }
                    }
                }
            } else if (ch == 'n') {
                beginOffset = jsonOffset;
                jsonOffset++;
                if (jsonOffset >= jsonLength) {
                    return OKJSON_ERROR_END_OF_BUFFER;
                }
                ch = jsonCharArray[jsonOffset];
                if (ch == 'u') {
                    jsonOffset++;
                    if (jsonOffset >= jsonLength) {
                        return OKJSON_ERROR_END_OF_BUFFER;
                    }
                    ch = jsonCharArray[jsonOffset];
                    if (ch == 'l') {
                        jsonOffset++;
                        if (jsonOffset >= jsonLength) {
                            return OKJSON_ERROR_END_OF_BUFFER;
                        }
                        ch = jsonCharArray[jsonOffset];
                        if (ch == 'l') {
                            tokenType = TokenType.TOKEN_TYPE_NULL;
                            booleanValue = true;
                            endOffset = jsonOffset;
                            jsonOffset++;
                            return 0;
                        }
                    }
                }
            } else {
                errorDesc = "Invalid byte '" + ch + "'";
                return OKJSON_ERROR_INVALID_BYTE;
            }
        }

        return OKJSON_ERROR_END_OF_BUFFER;
    }

    @SuppressWarnings("unchecked")
    private int addArrayObject(char[] jsonCharArray,
                               TokenType valueTokenType,
                               int valueBeginOffset,
                               int valueEndOffset,
                               Object object,
                               Field field)
    {

        try {
            Class<?> clazz = field.getType();
            if (List.class.isAssignableFrom(clazz)) {
                Type              type      = field.getGenericType();
                ParameterizedType pt        = (ParameterizedType) type;
                Class<?>          typeClass = (Class<?>) pt.getActualTypeArguments()[0];
                String stringValue = new String(jsonCharArray, valueBeginOffset,
                        valueEndOffset - valueBeginOffset + 1);
                if (typeClass == String.class) {
                    if (valueTokenType == TokenType.TOKEN_TYPE_STRING) {
                        String value = stringValue;
                        ((List<Object>) object).add(value);
                    } else if (valueTokenType == TokenType.TOKEN_TYPE_NULL) {
                        ;
                    }
                } else if (typeClass == Byte.class) {
                    if (valueTokenType == TokenType.TOKEN_TYPE_INTEGER) {
                        Byte value = Byte.valueOf(stringValue);
                        ((List<Object>) object).add(value);
                    } else if (valueTokenType == TokenType.TOKEN_TYPE_NULL) {
                        ;
                    }
                } else if (typeClass == Short.class) {
                    if (valueTokenType == TokenType.TOKEN_TYPE_INTEGER) {
                        Short value = Short.valueOf(stringValue);
                        ((List<Object>) object).add(value);
                    } else if (valueTokenType == TokenType.TOKEN_TYPE_NULL) {
                        ;
                    }
                } else if (typeClass == Integer.class) {
                    if (valueTokenType == TokenType.TOKEN_TYPE_INTEGER) {
                        Integer value = Integer.valueOf(stringValue);
                        ((List<Object>) object).add(value);
                    } else if (valueTokenType == TokenType.TOKEN_TYPE_NULL) {
                        ;
                    }
                } else if (typeClass == Long.class) {
                    if (valueTokenType == TokenType.TOKEN_TYPE_INTEGER) {
                        Long value = Long.valueOf(stringValue);
                        ((List<Object>) object).add(value);
                    } else if (valueTokenType == TokenType.TOKEN_TYPE_NULL) {
                        ;
                    }
                } else if (typeClass == Float.class) {
                    if (valueTokenType == TokenType.TOKEN_TYPE_DECIMAL) {
                        Float value = Float.valueOf(stringValue);
                        ((List<Object>) object).add(value);
                    } else if (valueTokenType == TokenType.TOKEN_TYPE_NULL) {
                        ;
                    }
                } else if (typeClass == Double.class) {
                    if (valueTokenType == TokenType.TOKEN_TYPE_DECIMAL) {
                        Double value = Double.valueOf(stringValue);
                        ((List<Object>) object).add(value);
                    } else if (valueTokenType == TokenType.TOKEN_TYPE_NULL) {
                        ;
                    }
                } else if (typeClass == Boolean.class) {
                    if (valueTokenType == TokenType.TOKEN_TYPE_BOOL) {
                        ((List<Object>) object).add(booleanValue);
                    } else if (valueTokenType == TokenType.TOKEN_TYPE_NULL) {
                        ;
                    }
                } else if (typeClass == LocalDate.class) {
                    if (valueTokenType == TokenType.TOKEN_TYPE_STRING) {
                        OkJsonDateTimeFormatter okjsonDateTimeFormatter;
                        String                  defaultDateTimeFormatter;
                        LocalDate               localDate;
                        if (field.isAnnotationPresent(OkJsonDateTimeFormatter.class)) {
                            okjsonDateTimeFormatter = field
                                    .getAnnotation(OkJsonDateTimeFormatter.class);
                            defaultDateTimeFormatter = okjsonDateTimeFormatter.format();
                        } else {
                            defaultDateTimeFormatter = "yyyy-MM-dd";
                        }
                        localDate = LocalDate.parse(stringValue,
                                DateTimeFormatter.ofPattern(defaultDateTimeFormatter));
                        ((List<Object>) object).add(localDate);
                    } else if (valueTokenType == TokenType.TOKEN_TYPE_NULL) {
                        ;
                    }
                } else if (typeClass == LocalTime.class) {
                    if (valueTokenType == TokenType.TOKEN_TYPE_STRING) {
                        OkJsonDateTimeFormatter okjsonDateTimeFormatter;
                        String                  defaultDateTimeFormatter;
                        LocalTime               localTime;
                        if (field.isAnnotationPresent(OkJsonDateTimeFormatter.class)) {
                            okjsonDateTimeFormatter = field
                                    .getAnnotation(OkJsonDateTimeFormatter.class);
                            defaultDateTimeFormatter = okjsonDateTimeFormatter.format();
                        } else {
                            defaultDateTimeFormatter = "HH:mm:ss";
                        }
                        localTime = LocalTime.parse(stringValue,
                                DateTimeFormatter.ofPattern(defaultDateTimeFormatter));
                        ((List<Object>) object).add(localTime);
                    } else if (valueTokenType == TokenType.TOKEN_TYPE_NULL) {
                        ;
                    }
                } else if (typeClass == LocalDateTime.class) {
                    if (valueTokenType == TokenType.TOKEN_TYPE_STRING) {
                        OkJsonDateTimeFormatter okjsonDateTimeFormatter;
                        String                  defaultDateTimeFormatter;
                        LocalDateTime           localDateTime;
                        if (field.isAnnotationPresent(OkJsonDateTimeFormatter.class)) {
                            okjsonDateTimeFormatter = field
                                    .getAnnotation(OkJsonDateTimeFormatter.class);
                            defaultDateTimeFormatter = okjsonDateTimeFormatter.format();
                        } else {
                            defaultDateTimeFormatter = "yyyy-MM-dd HH:mm:ss";
                        }
                        localDateTime = LocalDateTime.parse(stringValue,
                                DateTimeFormatter.ofPattern(defaultDateTimeFormatter));
                        ((List<Object>) object).add(localDateTime);
                    } else if (valueTokenType == TokenType.TOKEN_TYPE_NULL) {
                        ;
                    }
                } else if (valueTokenType == TokenType.TOKEN_TYPE_NULL) {
                    ;
                } else {
                    if (strictPolicyEnable == true) {
                        return OKJSON_ERROR_PORPERTY_TYPE_NOT_MATCH_IN_OBJECT;
                    }
                }
            } else {
                if (strictPolicyEnable == true) {
                    return OKJSON_ERROR_PORPERTY_TYPE_NOT_MATCH_IN_OBJECT;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return OKJSON_ERROR_EXCEPTION;
        }

        return 0;
    }

    private int stringToArrayObject(char[] jsonCharArray, Object object, Field field)
    {

        TokenType valueTokenType;
        int       valueBeginOffset;
        int       valueEndOffset;

        int nret;

        while (true) {
            // token "value" or '{'
            nret = tokenJsonWord(jsonCharArray);
            if (nret == OKJSON_ERROR_END_OF_BUFFER) {
                break;
            }
            if (nret != 0) {
                return nret;
            }

            if (tokenType == TokenType.TOKEN_TYPE_LEFT_BRACE) {
                try {
                    if (field != null) {
                        Class<?> clazz = field.getType();
                        if (List.class.isAssignableFrom(clazz)) {
                            Type              type      = field.getGenericType();
                            ParameterizedType pt        = (ParameterizedType) type;
                            Class<?>          typeClazz = (Class<?>) pt.getActualTypeArguments()[0];
                            Object            childObject;
                            childObject = typeClazz.newInstance();
                            nret = stringToObjectProperties(jsonCharArray, childObject);
                            if (nret != 0) {
                                return nret;
                            }

                            ((List<Object>) object).add(childObject);
                        }
                    } else {
                        nret = stringToObjectProperties(jsonCharArray, null);
                        if (nret != 0) {
                            return nret;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return OKJSON_ERROR_EXCEPTION;
                }
            } else if (tokenType == TokenType.TOKEN_TYPE_STRING
                    || tokenType == TokenType.TOKEN_TYPE_INTEGER
                    || tokenType == TokenType.TOKEN_TYPE_DECIMAL
                    || tokenType == TokenType.TOKEN_TYPE_BOOL) {
                ;
            } else {
                int beginPos = endOffset - 16;
                if (beginPos < 0) {
                    beginPos = 0;
                }
                errorDesc = "unexpect \"" + String
                        .copyValueOf(jsonCharArray, beginOffset, endOffset - beginOffset + 1)
                        + "\"";
                return OKJSON_ERROR_UNEXPECT_TOKEN_AFTER_LEFT_BRACE;
            }

            valueTokenType = tokenType;
            valueBeginOffset = beginOffset;
            valueEndOffset = endOffset;

            // token ',' or ']'
            nret = tokenJsonWord(jsonCharArray);
            if (nret == OKJSON_ERROR_END_OF_BUFFER) {
                break;
            }
            if (nret != 0) {
                return nret;
            }

            if (tokenType == TokenType.TOKEN_TYPE_COMMA
                    || tokenType == TokenType.TOKEN_TYPE_RIGHT_BRACKET) {
                if (object != null && field != null) {
                    errorCode = addArrayObject(jsonCharArray, valueTokenType, valueBeginOffset,
                            valueEndOffset, object, field);
                    if (errorCode != 0) {
                        return errorCode;
                    }
                }

                if (tokenType == TokenType.TOKEN_TYPE_RIGHT_BRACKET) {
                    break;
                }
            } else {
                errorDesc = "unexpect \"" + String
                        .copyValueOf(jsonCharArray, beginOffset, endOffset - beginOffset + 1)
                        + "\"";
                return OKJSON_ERROR_UNEXPECT_TOKEN_AFTER_LEFT_BRACE;
            }
        }

        return 0;
    }

    private int setObjectProperty(char[] jsonCharArray,
                                  TokenType valueTokenType,
                                  int valueBeginOffset,
                                  int valueEndOffset,
                                  Object object,
                                  Field field,
                                  Method method)
    {

        StringBuilder fieldStringBuilder;

        fieldStringBuilder = fieldStringBuilderCache;

        String stringValue = new String(jsonCharArray, valueBeginOffset,
                valueEndOffset - valueBeginOffset + 1);
        try {
            if (field.getType() == String.class) {
                if (valueTokenType == TokenType.TOKEN_TYPE_STRING) {
                    setStringValue(object, field, method, fieldStringBuilder, stringValue);
                }
            } else if (field.getType() == Byte.class) {
                if (valueTokenType == TokenType.TOKEN_TYPE_INTEGER) {
                    setByteValue(object, field, method, stringValue);
                }
            } else if (field.getType() == Short.class) {
                if (valueTokenType == TokenType.TOKEN_TYPE_INTEGER) {
                    setShortValue(object, field, method, stringValue);
                }
            } else if (field.getType() == Integer.class) {
                if (valueTokenType == TokenType.TOKEN_TYPE_INTEGER) {
                    setIntegerValue(object, field, method, stringValue);
                }
            } else if (field.getType() == Long.class) {
                if (valueTokenType == TokenType.TOKEN_TYPE_INTEGER) {
                    setLongValue(object, field, method, stringValue);
                }
            } else if (field.getType() == Float.class) {
                if (valueTokenType == TokenType.TOKEN_TYPE_DECIMAL) {
                    setFlobatValue(object, field, method, stringValue);
                }
            } else if (field.getType() == Double.class) {
                if (valueTokenType == TokenType.TOKEN_TYPE_DECIMAL) {
                    setDoubleValue(object, field, method, stringValue);
                }
            } else if (field.getType() == Boolean.class) {
                if (valueTokenType == TokenType.TOKEN_TYPE_BOOL) {
                    setBooleanValue(object, field, method, stringValue);
                }
            } else if ("byte".equals(field.getType().getName())
                    && valueTokenType == TokenType.TOKEN_TYPE_INTEGER) {
                setBytePrimitiveValue(object, field, method, stringValue);
            } else if ("short".equals(field.getType().getName())
                    && valueTokenType == TokenType.TOKEN_TYPE_INTEGER) {
                setShortPrimitiveValue(object, field, method, stringValue);
            } else if ("int".equals(field.getType().getName())
                    && valueTokenType == TokenType.TOKEN_TYPE_INTEGER) {
                setIntegerPrimitiveValue(object, field, method, stringValue);
            } else if ("long".equals(field.getType().getName())
                    && valueTokenType == TokenType.TOKEN_TYPE_INTEGER) {
                setLongPrimitiveValue(object, field, method, stringValue);
            } else if ("float".equals(field.getType().getName())
                    && valueTokenType == TokenType.TOKEN_TYPE_DECIMAL) {
                setFloatPrimitiveValue(object, field, method, stringValue);
            } else if ("double".equals(field.getType().getName())
                    && valueTokenType == TokenType.TOKEN_TYPE_DECIMAL) {
                setDoublePrimitiveValue(object, field, method, stringValue);
            } else if ("boolean".equals(field.getType().getName())
                    && valueTokenType == TokenType.TOKEN_TYPE_BOOL) {
                setBooleanPrimitiveValue(object, field, method);
            } else if (field.getType() == LocalDate.class) {
                if (valueTokenType == TokenType.TOKEN_TYPE_STRING) {
                    setLocalDateValue(object, field, method, stringValue, fieldStringBuilder);
                }
            } else if (field.getType() == LocalTime.class) {
                if (valueTokenType == TokenType.TOKEN_TYPE_STRING) {
                    setLocalTimeValue(object, field, method, stringValue, fieldStringBuilder);
                }
            } else if (field.getType() == LocalDateTime.class) {
                if (valueTokenType == TokenType.TOKEN_TYPE_STRING) {
                    setLocalDateTimeValue(object, field, method, stringValue, fieldStringBuilder);
                }
            } else if (valueTokenType == TokenType.TOKEN_TYPE_NULL) {
                setFieldValue(object, field, method, null);
            } else {
                if (strictPolicyEnable) {
                    return OKJSON_ERROR_PORPERTY_TYPE_NOT_MATCH_IN_OBJECT;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return OKJSON_ERROR_EXCEPTION;
        }
        return 0;
    }

    private void setLocalDateTimeValue(Object object,
                                       Field field,
                                       Method method,
                                       String stringValue,
                                       StringBuilder fieldStringBuilder)
    throws IllegalAccessException, InvocationTargetException
    {
        OkJsonDateTimeFormatter okjsonDateTimeFormatter;
        String                  defaultDateTimeFormatter;
        LocalDateTime           localDateTime;
        if (field.isAnnotationPresent(OkJsonDateTimeFormatter.class)) {
            okjsonDateTimeFormatter = field.getAnnotation(OkJsonDateTimeFormatter.class);
            defaultDateTimeFormatter = okjsonDateTimeFormatter.format();
        } else {
            defaultDateTimeFormatter = "yyyy-MM-dd HH:mm:ss";
        }
        if (fieldStringBuilder.length() > 0) {
            localDateTime = LocalDateTime.parse(fieldStringBuilder.toString(),
                    DateTimeFormatter.ofPattern(defaultDateTimeFormatter));
        } else {
            localDateTime = LocalDateTime
                    .parse(stringValue, DateTimeFormatter.ofPattern(defaultDateTimeFormatter));
        }
        if (method != null) {
            method.invoke(object, localDateTime);
        } else if (directAccessPropertyEnable) {
            field.set(object, localDateTime);
        }
    }

    private void setLocalTimeValue(Object object,
                                   Field field,
                                   Method method,
                                   String stringValue,
                                   StringBuilder fieldStringBuilder)
    throws IllegalAccessException, InvocationTargetException
    {
        OkJsonDateTimeFormatter okjsonDateTimeFormatter;
        String                  defaultDateTimeFormatter;
        LocalTime               localTime;
        if (field.isAnnotationPresent(OkJsonDateTimeFormatter.class)) {
            okjsonDateTimeFormatter = field.getAnnotation(OkJsonDateTimeFormatter.class);
            defaultDateTimeFormatter = okjsonDateTimeFormatter.format();
        } else {
            defaultDateTimeFormatter = "HH:mm:ss";
        }
        if (fieldStringBuilder.length() > 0) {
            localTime = LocalTime.parse(fieldStringBuilder.toString(),
                    DateTimeFormatter.ofPattern(defaultDateTimeFormatter));
        } else {
            localTime = LocalTime
                    .parse(stringValue, DateTimeFormatter.ofPattern(defaultDateTimeFormatter));
        }
        if (method != null) {
            method.invoke(object, localTime);
        } else if (directAccessPropertyEnable) {
            field.set(object, localTime);
        }
    }

    private void setLocalDateValue(Object object,
                                   Field field,
                                   Method method,
                                   String stringValue,
                                   StringBuilder fieldStringBuilder)
    throws IllegalAccessException, InvocationTargetException
    {
        OkJsonDateTimeFormatter okjsonDateTimeFormatter;
        String                  defaultDateTimeFormatter;
        LocalDate               localDate;
        if (field.isAnnotationPresent(OkJsonDateTimeFormatter.class)) {
            okjsonDateTimeFormatter = field.getAnnotation(OkJsonDateTimeFormatter.class);
            defaultDateTimeFormatter = okjsonDateTimeFormatter.format();
        } else {
            defaultDateTimeFormatter = "yyyy-MM-dd";
        }
        if (fieldStringBuilder.length() > 0) {
            localDate = LocalDate.parse(fieldStringBuilder.toString(),
                    DateTimeFormatter.ofPattern(defaultDateTimeFormatter));
        } else {
            localDate = LocalDate
                    .parse(stringValue, DateTimeFormatter.ofPattern(defaultDateTimeFormatter));
        }
        if (method != null) {
            method.invoke(object, localDate);
        } else if (directAccessPropertyEnable) {
            field.set(object, localDate);
        }
    }

    private void setBooleanPrimitiveValue(Object object, Field field, Method method)
    throws IllegalAccessException, InvocationTargetException
    {
        if (method != null) {
            method.invoke(object, booleanValue);
        } else if (directAccessPropertyEnable) {
            field.setBoolean(object, booleanValue);
        }
    }

    private void setDoublePrimitiveValue(Object object,
                                         Field field,
                                         Method method,
                                         String stringValue)
    throws IllegalAccessException, InvocationTargetException
    {
        double value = Double.valueOf(stringValue).doubleValue();
        if (method != null) {
            method.invoke(object, value);
        } else if (directAccessPropertyEnable) {
            field.setDouble(object, value);
        }
    }

    private void setFloatPrimitiveValue(Object object,
                                        Field field,
                                        Method method,
                                        String stringValue)
    throws IllegalAccessException, InvocationTargetException
    {
        float value = Float.valueOf(stringValue).floatValue();
        if (method != null) {
            method.invoke(object, value);
        } else if (directAccessPropertyEnable) {
            field.setFloat(object, value);
        }
    }

    private void setLongPrimitiveValue(Object object,
                                       Field field,
                                       Method method,
                                       String stringValue)
    throws IllegalAccessException, InvocationTargetException
    {
        long value = Long.valueOf(stringValue).longValue();
        if (method != null) {
            method.invoke(object, value);
        } else if (directAccessPropertyEnable) {
            field.setLong(object, value);
        }
    }

    private void setIntegerPrimitiveValue(Object object,
                                          Field field,
                                          Method method,
                                          String stringValue)
    throws IllegalAccessException, InvocationTargetException
    {
        int value = Integer.valueOf(stringValue).intValue();
        if (method != null) {
            method.invoke(object, value);
        } else if (directAccessPropertyEnable) {
            field.setInt(object, value);
        }
    }

    private void setShortPrimitiveValue(Object object,
                                        Field field,
                                        Method method,
                                        String stringValue)
    throws IllegalAccessException, InvocationTargetException
    {
        short value = Integer.valueOf(stringValue).shortValue();
        if (method != null) {
            method.invoke(object, value);
        } else if (directAccessPropertyEnable) {
            field.setShort(object, value);
        }
    }

    private void setBytePrimitiveValue(Object object,
                                       Field field,
                                       Method method,
                                       String stringValue)
    throws IllegalAccessException, InvocationTargetException
    {
        byte value = Integer.valueOf(stringValue).byteValue();
        if (method != null) {
            method.invoke(object, value);
        } else if (directAccessPropertyEnable) {
            field.setByte(object, value);
        }
    }

    private void setBooleanValue(Object object, Field field, Method method, String stringValue)
    throws IllegalAccessException, InvocationTargetException
    {
        Boolean value = Boolean.valueOf(stringValue);
        if (method != null) {
            method.invoke(object, value);
        } else if (directAccessPropertyEnable) {
            field.set(object, value);
        }
    }

    private void setDoubleValue(Object object, Field field, Method method, String stringValue)
    throws IllegalAccessException, InvocationTargetException
    {
        Double value = Double.valueOf(stringValue);
        if (method != null) {
            method.invoke(object, value);
        } else if (directAccessPropertyEnable) {
            field.set(object, value);
        }
    }

    private void setFlobatValue(Object object, Field field, Method method, String stringValue)
    throws IllegalAccessException, InvocationTargetException
    {
        Float value = Float.valueOf(stringValue);
        if (method != null) {
            method.invoke(object, value);
        } else if (directAccessPropertyEnable) {
            field.set(object, value);
        }
    }

    private void setLongValue(Object object, Field field, Method method, String stringValue)
    throws IllegalAccessException, InvocationTargetException
    {
        Long value = Long.valueOf(stringValue);
        if (method != null) {
            method.invoke(object, value);
        } else if (directAccessPropertyEnable) {
            field.set(object, value);
        }
    }

    private void setIntegerValue(Object object, Field field, Method method, String stringValue)
    throws IllegalAccessException, InvocationTargetException
    {
        Integer value = Integer.valueOf(stringValue);
        if (method != null) {
            method.invoke(object, value);
        } else if (directAccessPropertyEnable) {
            field.set(object, value);
        }
    }

    private void setShortValue(Object object, Field field, Method method, String stringValue)
    throws IllegalAccessException, InvocationTargetException
    {
        Short value = Short.valueOf(stringValue);
        if (method != null) {
            method.invoke(object, value);
        } else if (directAccessPropertyEnable) {
            field.set(object, value);
        }
    }

    private void setByteValue(Object object, Field field, Method method, String stringValue)
    throws IllegalAccessException, InvocationTargetException
    {
        Byte value = Byte.valueOf(stringValue);
        if (method != null) {
            method.invoke(object, value);
        } else if (directAccessPropertyEnable) {
            field.set(object, value);
        }
    }

    private void setStringValue(Object object,
                                Field field,
                                Method method,
                                StringBuilder fieldStringBuilder,
                                String stringValue)
    throws IllegalAccessException, InvocationTargetException
    {
        String value;
        if (fieldStringBuilder.length() > 0) {
            value = fieldStringBuilder.toString();
        } else {
            value = stringValue;
        }
        setFieldValue(object, field, method, value);
    }

    private void setFieldValue(Object object, Field field, Method method, String value)
    throws IllegalAccessException, InvocationTargetException
    {
        if (method != null) {
            method.invoke(object, value);
        } else if (directAccessPropertyEnable) {
            field.set(object, value);
        }
    }

    private int stringToObjectProperties(char[] jsonCharArray, Object object)
    {

        Class                   clazz;
        HashMap<String, Field>  stringMapFields;
        HashMap<String, Method> stringMapMethods;
        Field[]                 fields;
        Field                   field;
        Method                  method = null;
        TokenType               fieldNameTokenType;
        int                     fieldNameBeginOffset;
        int                     fieldNameEndOffset;
        String                  fieldName;
        TokenType               valueTokenType;
        int                     valueBeginOffset;
        int                     valueEndOffset;

        int nret;

        if (object != null) {
            clazz = object.getClass();

            stringMapFields = stringMapFieldsCache
                    .computeIfAbsent(clazz.getName(), k -> new HashMap<>());

            stringMapMethods = stringMapMethodsCache
                    .computeIfAbsent(clazz.getName(), k -> new HashMap<>());

            initMethodsIfEmpty(clazz, stringMapFields, stringMapMethods);
        } else {
            stringMapFields = null;
            stringMapMethods = null;
        }

        while (true) {
            // token "name"
            nret = tokenJsonWord(jsonCharArray);
            if (nret == OKJSON_ERROR_END_OF_BUFFER) {
                break;
            }
            if (nret != 0) {
                return nret;
            }

            fieldNameTokenType = tokenType;
            fieldNameBeginOffset = beginOffset;
            fieldNameEndOffset = endOffset;
            fieldName = new String(jsonCharArray, fieldNameBeginOffset,
                    fieldNameEndOffset - fieldNameBeginOffset + 1);

            if (object != null) {
                field = stringMapFields.get(fieldName);
                if (field == null) {
                    if (strictPolicyEnable == true) {
                        return OKJSON_ERROR_NAME_NOT_FOUND_IN_OBJECT;
                    }
                }

                method = stringMapMethods.get(fieldName);
            } else {
                field = null;
                method = null;
            }

            if (tokenType != TokenType.TOKEN_TYPE_STRING) {
                errorDesc = "expect a name but \"" + String
                        .copyValueOf(jsonCharArray, beginOffset, endOffset - beginOffset + 1)
                        + "\"";
                return OKJSON_ERROR_NAME_INVALID;
            }

            // token ':' or ',' or '}' or ']'
            nret = tokenJsonWord(jsonCharArray);
            if (nret == OKJSON_ERROR_END_OF_BUFFER) {
                break;
            }
            if (nret != 0) {
                return nret;
            }

            if (tokenType == TokenType.TOKEN_TYPE_COLON) {
                ;
            } else if (tokenType == TokenType.TOKEN_TYPE_COMMA
                    || tokenType == TokenType.TOKEN_TYPE_RIGHT_BRACE) {
                clazz = field.getType();
                if (List.class.isAssignableFrom(clazz)) {
                    nret = addArrayObject(jsonCharArray, fieldNameTokenType, fieldNameBeginOffset,
                            fieldNameEndOffset, object, field);
                    if (nret != 0) {
                        return nret;
                    }

                    if (tokenType == TokenType.TOKEN_TYPE_RIGHT_BRACE) {
                        break;
                    }
                }
            } else if (tokenType == TokenType.TOKEN_TYPE_RIGHT_BRACKET) {
                break;
            } else {
                errorDesc = "expect ':' but \"" + String
                        .copyValueOf(jsonCharArray, beginOffset, endOffset - beginOffset + 1)
                        + "\"";
                return OKJSON_ERROR_EXPECT_COLON_AFTER_NAME;
            }

            // token '{' or '[' or "value"
            nret = tokenJsonWord(jsonCharArray);
            if (nret == OKJSON_ERROR_END_OF_BUFFER) {
                break;
            }
            if (nret != 0) {
                return nret;
            }

            valueTokenType = tokenType;
            valueBeginOffset = beginOffset;
            valueEndOffset = endOffset;

            if (tokenType == TokenType.TOKEN_TYPE_LEFT_BRACE
                    || tokenType == TokenType.TOKEN_TYPE_LEFT_BRACKET) {
                try {
                    Object childObject = null;

                    if (field != null) {
                        Class<?> type = field.getType();
                        if (type.isInterface()) {
                            if (Map.class.isAssignableFrom(type)) {
                                childObject = new HashMap();
                            } else if (List.class.isAssignableFrom(type)) {
                                childObject = new ArrayList();
                            }
                        } else {
                            childObject = type.newInstance();
                        }
                        if (childObject == null) {
                            return OKJSON_ERROR_UNEXPECT;
                        }
                    } else {
                        childObject = null;
                    }

                    if (tokenType == TokenType.TOKEN_TYPE_LEFT_BRACE) {
                        nret = stringToObjectProperties(jsonCharArray, childObject);
                    } else {
                        nret = stringToArrayObject(jsonCharArray, childObject, field);
                    }
                    if (nret != 0) {
                        return nret;
                    }

                    if (field != null) {
                        field.set(object, childObject);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return OKJSON_ERROR_EXCEPTION;
                }
            } else {
                if (object != null && field != null) {
                    nret = setObjectProperty(jsonCharArray, valueTokenType, valueBeginOffset,
                            valueEndOffset, object, field, method);
                    if (nret != 0) {
                        return nret;
                    }
                }
            }

            // token ',' or '}' or ']'
            nret = tokenJsonWord(jsonCharArray);
            if (nret == OKJSON_ERROR_END_OF_BUFFER) {
                break;
            }
            if (nret != 0) {
                return nret;
            }

            if (tokenType == TokenType.TOKEN_TYPE_COMMA) {
                ;
            } else if (tokenType == TokenType.TOKEN_TYPE_RIGHT_BRACE) {
                break;
            } else if (tokenType == TokenType.TOKEN_TYPE_RIGHT_BRACKET) {
                break;
            } else {
                errorDesc = "expect ',' or '}' or ']' but \"" + String
                        .copyValueOf(jsonCharArray, beginOffset, endOffset - beginOffset + 1)
                        + "\"";
                return OKJSON_ERROR_EXPECT_COLON_AFTER_NAME;
            }
        }

        return 0;
    }

    private void initMethodsIfEmpty(Class clazz,
                                    HashMap<String, Field> stringMapFields,
                                    HashMap<String, Method> stringMapMethods)
    {
        Field[] fields;
        String  fieldName;
        Method  method;
        if (stringMapFields.isEmpty()) {
            fields = clazz.getDeclaredFields();
            for (Field f : fields) {
                f.setAccessible(true);
                fieldName = f.getName();
                method = null;
                try {
                    method = clazz.getMethod(
                            "set" + fieldName.substring(0, 1).toUpperCase(Locale.getDefault())
                                    + fieldName.substring(1), f.getType());
                    method.setAccessible(true);
                } catch (NoSuchMethodException | SecurityException e2) {
                    ;
                }

                if (method != null && Modifier.isPublic(method.getModifiers())) {
                    stringMapMethods.put(fieldName, method);
                    stringMapFields.put(fieldName, f);
                } else if (Modifier.isPublic(f.getModifiers())) {
                    stringMapFields.put(fieldName, f);
                }
            }
        }
    }


}