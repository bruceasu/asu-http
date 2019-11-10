package xyz.calvinwilliams.okjson;

import java.io.IOException;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

class OkJsonGenerator {

    enum ClassFieldType {
        /**
         * string
         */
        CLASSFIELDTYPE_STRING,
        /**
         * not string
         */
        CLASSFIELDTYPE_NOT_STRING,
        /**
         * local date
         */
        CLASSFIELDTYPE_LOCALDATE,
        /**
         * local time
         */
        CLASSFIELDTYPE_LOCALTIME,
        /**
         * local date time
         */
        CLASSFIELDTYPE_LOCALDATETIME,
        /**
         * list
         */
        CLASSFIELDTYPE_LIST,
        /**
         * sub class
         */
        CLASSFIELDTYPE_SUBCLASS
    }

    class OkJsonClassField {

        char[]                  fieldName;
        char[]                  fieldNameQM;
        ClassFieldType          type;
        Field                   field;
        Method                  getMethod;
        OkJsonDateTimeFormatter okjsonDateTimeFormatter;
    }

    private static HashMap<String, LinkedList<OkJsonClassField>> classMapFieldListCache = new HashMap<>();
    private static HashMap<Class, Boolean>                       basicTypeClassMapBooleanCache;


    final public static int OKJSON_ERROR_EXCEPTION  = -8;
    final public static int OKJSON_ERROR_NEW_OBJECT = -31;

    private static final char   SEPFIELD_CHAR        = ',';
    private static final char[] SEPFIELD_CHAR_PRETTY = " ,\n".toCharArray();
    private static final char   ENTER_CHAR           = '\n';
    private static final String NULL_STRING          = "null";

    static
    {
        basicTypeClassMapBooleanCache = new HashMap<>(16);

        basicTypeClassMapBooleanCache.put(String.class, Boolean.TRUE);
        basicTypeClassMapBooleanCache.put(Byte.class, Boolean.TRUE);
        basicTypeClassMapBooleanCache.put(Short.class, Boolean.TRUE);
        basicTypeClassMapBooleanCache.put(Integer.class, Boolean.TRUE);
        basicTypeClassMapBooleanCache.put(Long.class, Boolean.TRUE);
        basicTypeClassMapBooleanCache.put(Float.class, Boolean.TRUE);
        basicTypeClassMapBooleanCache.put(Double.class, Boolean.TRUE);
        basicTypeClassMapBooleanCache.put(Boolean.class, Boolean.TRUE);
        basicTypeClassMapBooleanCache.put(LocalDate.class, Boolean.TRUE);
        basicTypeClassMapBooleanCache.put(LocalTime.class, Boolean.TRUE);
        basicTypeClassMapBooleanCache.put(LocalDateTime.class, Boolean.TRUE);
    }


    private boolean strictPolicyEnable;
    private boolean directAccessPropertyEnable;
    private boolean prettyFormatEnable;

    private Integer errorCode;
    private String  errorDesc;

    public int objectToFile(Object object, String filePath)
    {
        String jsonString = objectToString(object);
        try {
            Files.write(Paths.get(filePath), jsonString.getBytes(), StandardOpenOption.WRITE);
            return 0;
        } catch (IOException e) {
            return -1;
        }
    }

    public String objectToString(Object object)
    {

        OkJsonCharArrayBuilder jsonCharArrayBuilder = new OkJsonCharArrayBuilder(1024);
        jsonCharArrayBuilder.setLength(0);
        if (prettyFormatEnable) {
            jsonCharArrayBuilder.appendCharArray("{\n".toCharArray());
        } else {
            jsonCharArrayBuilder.appendChar('{');
        }

        errorCode = objectToPropertiesString(object, jsonCharArrayBuilder, 0);
        if (errorCode != 0) {
            return null;
        }

        if (prettyFormatEnable) {
            jsonCharArrayBuilder.appendCharArray("}\n".toCharArray());
        } else {
            jsonCharArrayBuilder.appendChar('}');
        }

        return jsonCharArrayBuilder.toString();
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

    public OkJsonGenerator()
    {
        this.strictPolicyEnable = false;
        this.directAccessPropertyEnable = false;
        this.prettyFormatEnable = false;
        this.errorCode = 0;
        this.errorDesc = null;
    }


    private int objectToListString(List<Object> array,
                                   int arrayCount,
                                   OkJsonClassField classField,
                                   OkJsonCharArrayBuilder jsonCharArrayBuilder,
                                   int depth)
    {
        try {
            Type              type      = classField.field.getGenericType();
            ParameterizedType pt        = (ParameterizedType) type;
            Class<?>          typeClazz = (Class<?>) pt.getActualTypeArguments()[0];
            Boolean           isBaseType         = basicTypeClassMapBooleanCache.get(typeClazz);
            if (isBaseType == null) {
                isBaseType = false;
            }
            if (isBaseType) {
                processBaseType(array, classField, jsonCharArrayBuilder, depth, typeClazz);
            } else {
                Integer nret = processNonBaseType(array, jsonCharArrayBuilder, depth);
                if (nret != null) {
                    return nret;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return OKJSON_ERROR_EXCEPTION;
        }

        if (prettyFormatEnable) {
            jsonCharArrayBuilder.appendChar(ENTER_CHAR);
        }

        return 0;
    }

    private Integer processNonBaseType(List<Object> array,
                                       OkJsonCharArrayBuilder jsonCharArrayBuilder,
                                       int depth)
    {
        int arrayIndex = 0;
        int                     nret;
        for (Object object : array) {
            arrayIndex++;
            if (arrayIndex > 1) {
                if (prettyFormatEnable) {
                    jsonCharArrayBuilder.appendCharArrayWith3(SEPFIELD_CHAR_PRETTY);
                } else {
                    jsonCharArrayBuilder.appendChar(SEPFIELD_CHAR);
                }
            }

            if (object != null) {
                if (prettyFormatEnable) {
                    jsonCharArrayBuilder.appendTabs(depth + 1).appendString("{\n");
                } else {
                    jsonCharArrayBuilder.appendChar('{');
                }
                nret = objectToPropertiesString(object, jsonCharArrayBuilder, depth + 1);
                if (nret != 0) {
                    return nret;
                }
                if (prettyFormatEnable) {
                    jsonCharArrayBuilder.appendTabs(depth + 1).appendString("}");
                } else {
                    jsonCharArrayBuilder.appendChar('}');
                }
            }
        }
        return null;
    }

    private void processBaseType(List<Object> array,
                                 OkJsonClassField classField,
                                 OkJsonCharArrayBuilder jsonCharArrayBuilder,
                                 int depth,
                                 Class<?> typeClazz)
    {
        int arrayIndex = 0;
        for (Object object : array) {
            arrayIndex++;
            if (arrayIndex > 1) {
                if (prettyFormatEnable) {
                    jsonCharArrayBuilder.appendCharArrayWith3(SEPFIELD_CHAR_PRETTY)
                                        .appendTabs(depth + 1);
                } else {
                    jsonCharArrayBuilder.appendChar(SEPFIELD_CHAR);
                }
            } else {
                if (prettyFormatEnable) {
                    jsonCharArrayBuilder.appendTabs(depth + 1);
                }
            }

            if (typeClazz == String.class && object != null) {
                String str = (String) object;
                jsonCharArrayBuilder.appendJsonQmStringQm(str);
            } else if (typeClazz == LocalDate.class && object != null) {
                LocalDate localDate       = (LocalDate) object;
                String    localDateString = formatLocalDate(classField, localDate);
                jsonCharArrayBuilder.appendJsonQmStringQm(localDateString);
            } else if (typeClazz == LocalTime.class && object != null) {
                LocalTime localTime       = (LocalTime) object;
                String    localTimeString = formatLocalTime(classField, localTime);
                jsonCharArrayBuilder.appendJsonQmStringQm(localTimeString);
            } else if (typeClazz == LocalDateTime.class && object != null) {
                LocalDateTime localDateTime       = (LocalDateTime) object;
                String        localDateTimeString = formatLocalDateTime(classField, localDateTime);
                jsonCharArrayBuilder.appendJsonQmStringQm(localDateTimeString);
            } else {
                jsonCharArrayBuilder.appendJsonString(object.toString());
            }
        }
    }

    private String formatLocalDate(OkJsonClassField classField, LocalDate localDate)
    {
        String defaultDateTimeFormatter;
        if (classField.okjsonDateTimeFormatter != null) {
            defaultDateTimeFormatter = classField.okjsonDateTimeFormatter.format();
        } else {
            defaultDateTimeFormatter = "yyyy-MM-dd";
        }
        return DateTimeFormatter.ofPattern(defaultDateTimeFormatter).format(localDate);
    }

    private String formatLocalTime(OkJsonClassField classField, LocalTime localTime)
    {
        String defaultDateTimeFormatter;
        if (classField.okjsonDateTimeFormatter != null) {
            defaultDateTimeFormatter = classField.okjsonDateTimeFormatter.format();
        } else {
            defaultDateTimeFormatter = "HH:mm:ss";
        }
        return DateTimeFormatter.ofPattern(defaultDateTimeFormatter).format(localTime);
    }

    private String formatLocalDateTime(OkJsonClassField classField, LocalDateTime localDateTime)
    {
        String        defaultDateTimeFormatter;
        if (classField.okjsonDateTimeFormatter != null) {
            defaultDateTimeFormatter = classField.okjsonDateTimeFormatter.format();
        } else {
            defaultDateTimeFormatter = "yyyy-MM-dd HH:mm:ss";
        }
        return DateTimeFormatter
                .ofPattern(defaultDateTimeFormatter).format(localDateTime);
    }

    private String unfoldEscape(String value)
    {

        OkJsonCharArrayBuilder fieldCharArrayBuilder = new OkJsonCharArrayBuilder(1024);
        char[]                 jsonCharArrayBuilder;
        int                    jsonCharArrayLength;
        int                    jsonCharArrayIndex;
        int                    segmentBeginOffset;
        int                    segmentLen;
        char                   c;

        if (value == null) {
            return null;
        }

        jsonCharArrayBuilder = value.toCharArray();
        jsonCharArrayLength = value.length();

        fieldCharArrayBuilder.setLength(0);

        segmentBeginOffset = 0;
        for (jsonCharArrayIndex = 0; jsonCharArrayIndex < jsonCharArrayLength;
                jsonCharArrayIndex++) {
            c = jsonCharArrayBuilder[jsonCharArrayIndex];
            if (c == '\"') {
                segmentLen = jsonCharArrayIndex - segmentBeginOffset;
                if (segmentLen > 0) {
                    fieldCharArrayBuilder.appendBytesFromOffsetWithLength(jsonCharArrayBuilder,
                            segmentBeginOffset, segmentLen);
                }
                fieldCharArrayBuilder.appendCharArray("\\\"".toCharArray());
                segmentBeginOffset = jsonCharArrayIndex + 1;
            } else if (c == '\\') {
                segmentLen = jsonCharArrayIndex - segmentBeginOffset;
                if (segmentLen > 0) {
                    fieldCharArrayBuilder.appendBytesFromOffsetWithLength(jsonCharArrayBuilder,
                            segmentBeginOffset, segmentLen);
                }
                fieldCharArrayBuilder.appendCharArray("\\\\".toCharArray());
                segmentBeginOffset = jsonCharArrayIndex + 1;
            } else if (c == '/') {
                segmentLen = jsonCharArrayIndex - segmentBeginOffset;
                if (segmentLen > 0) {
                    fieldCharArrayBuilder.appendBytesFromOffsetWithLength(jsonCharArrayBuilder,
                            segmentBeginOffset, segmentLen);
                }
                fieldCharArrayBuilder.appendCharArray("\\/".toCharArray());
                segmentBeginOffset = jsonCharArrayIndex + 1;
            } else if (c == '\t') {
                segmentLen = jsonCharArrayIndex - segmentBeginOffset;
                if (segmentLen > 0) {
                    fieldCharArrayBuilder.appendBytesFromOffsetWithLength(jsonCharArrayBuilder,
                            segmentBeginOffset, segmentLen);
                }
                fieldCharArrayBuilder.appendCharArray("\\t".toCharArray());
                segmentBeginOffset = jsonCharArrayIndex + 1;
            } else if (c == '\f') {
                segmentLen = jsonCharArrayIndex - segmentBeginOffset;
                if (segmentLen > 0) {
                    fieldCharArrayBuilder.appendBytesFromOffsetWithLength(jsonCharArrayBuilder,
                            segmentBeginOffset, segmentLen);
                }
                fieldCharArrayBuilder.appendCharArray("\\f".toCharArray());
                segmentBeginOffset = jsonCharArrayIndex + 1;
            } else if (c == '\b') {
                segmentLen = jsonCharArrayIndex - segmentBeginOffset;
                if (segmentLen > 0) {
                    fieldCharArrayBuilder.appendBytesFromOffsetWithLength(jsonCharArrayBuilder,
                            segmentBeginOffset, segmentLen);
                }
                fieldCharArrayBuilder.appendCharArray("\\b".toCharArray());
                segmentBeginOffset = jsonCharArrayIndex + 1;
            } else if (c == '\n') {
                segmentLen = jsonCharArrayIndex - segmentBeginOffset;
                if (segmentLen > 0) {
                    fieldCharArrayBuilder.appendBytesFromOffsetWithLength(jsonCharArrayBuilder,
                            segmentBeginOffset, segmentLen);
                }
                fieldCharArrayBuilder.appendCharArray("\\n".toCharArray());
                segmentBeginOffset = jsonCharArrayIndex + 1;
            } else if (c == '\r') {
                segmentLen = jsonCharArrayIndex - segmentBeginOffset;
                if (segmentLen > 0) {
                    fieldCharArrayBuilder.appendBytesFromOffsetWithLength(jsonCharArrayBuilder,
                            segmentBeginOffset, segmentLen);
                }
                fieldCharArrayBuilder.appendCharArray("\\r".toCharArray());
                segmentBeginOffset = jsonCharArrayIndex + 1;
            }
        }
        if (fieldCharArrayBuilder.getLength() > 0 && segmentBeginOffset < jsonCharArrayIndex) {
            segmentLen = jsonCharArrayIndex - segmentBeginOffset;
            if (segmentLen > 0) {
                fieldCharArrayBuilder
                        .appendBytesFromOffsetWithLength(jsonCharArrayBuilder, segmentBeginOffset,
                                segmentLen);
            }
        }

        if (fieldCharArrayBuilder.getLength() == 0) {
            return value;
        } else {
            return fieldCharArrayBuilder.toString();
        }
    }

    private int objectToPropertiesString(Object object,
                                         OkJsonCharArrayBuilder jsonCharArrayBuilder,
                                         int depth)
    {

        Class<?>                     clazz;
        LinkedList<OkJsonClassField> classFieldList;
        int                          fieldIndex;

        int nret = 0;

        clazz = object.getClass();

        classFieldList = getClassFieldListFromCache(clazz);
        if (fillClassFieldListIfEmpty(clazz, classFieldList)) {
            return OKJSON_ERROR_EXCEPTION;
        }

        fieldIndex = 0;
        for (OkJsonClassField classField : classFieldList) {
            fieldIndex++;
            if (fieldIndex > 1) {
                if (prettyFormatEnable) {
                    jsonCharArrayBuilder.appendCharArrayWith3(SEPFIELD_CHAR_PRETTY);
                } else {
                    jsonCharArrayBuilder.appendChar(SEPFIELD_CHAR);
                }
            }

            try {
                ClassFieldProcessor processor = getClassFieldProcessor(jsonCharArrayBuilder,
                        classField, object, depth);
                processor.process();
            } catch (ObjectToListStringException e) {
                return e.getNret();
            } catch (ObjectToPropertiesStringException e) {
                return e.getNret();
            } catch (Exception e) {
                e.printStackTrace();
                return OKJSON_ERROR_EXCEPTION;
            }
        }

        if (prettyFormatEnable) {
            jsonCharArrayBuilder.appendChar(ENTER_CHAR);
        }

        return 0;
    }

    private boolean fillClassFieldListIfEmpty(Class<?> clazz,
                                              LinkedList<OkJsonClassField> classFieldList)
    {
        Field[]                 fields;
        String                  methodName;
        if (classFieldList.isEmpty()) {
            OkJsonClassField classField;

            fields = clazz.getDeclaredFields();
            for (Field f : fields) {
                f.setAccessible(true);

                classField = new OkJsonClassField();
                classField.fieldName = f.getName().toCharArray();
                classField.fieldNameQM = ('\"' + f.getName() + '\"').toCharArray();
                classField.field = f;
                if (f.getType() == String.class) {
                    classField.type = ClassFieldType.CLASSFIELDTYPE_STRING;
                } else if (f.getType() == LocalDate.class) {
                    classField.type = ClassFieldType.CLASSFIELDTYPE_LOCALDATE;
                } else if (f.getType() == LocalTime.class) {
                    classField.type = ClassFieldType.CLASSFIELDTYPE_LOCALTIME;
                } else if (f.getType() == LocalDateTime.class) {
                    classField.type = ClassFieldType.CLASSFIELDTYPE_LOCALDATETIME;
                } else if (f.getType() == ArrayList.class || f.getType() == LinkedList.class) {
                    classField.type = ClassFieldType.CLASSFIELDTYPE_LIST;
                } else if (basicTypeClassMapBooleanCache.get(f.getType()) != null || f.getType()
                                                                                 .isPrimitive()) {
                    classField.type = ClassFieldType.CLASSFIELDTYPE_NOT_STRING;
                } else {
                    classField.type = ClassFieldType.CLASSFIELDTYPE_SUBCLASS;
                }

                try {
                    if (f.getType() == Boolean.class || "boolean".equals(f.getType().getName())) {
                        methodName =
                                "is" + f.getName().substring(0, 1).toUpperCase(Locale.getDefault())
                                        + f.getName().substring(1);
                    } else {
                        methodName =
                                "get" + f.getName().substring(0, 1).toUpperCase(Locale.getDefault())
                                        + f.getName().substring(1);
                    }
                    classField.getMethod = clazz.getMethod(methodName);
                    classField.getMethod.setAccessible(true);
                } catch (NoSuchMethodException e) {
                    ;
                } catch (Exception e) {
                    e.printStackTrace();
                    return true;
                }

                if (f.isAnnotationPresent(OkJsonDateTimeFormatter.class)) {
                    classField.okjsonDateTimeFormatter = f
                            .getAnnotation(OkJsonDateTimeFormatter.class);
                } else {
                    classField.okjsonDateTimeFormatter = null;
                }

                if (Modifier.isPublic(f.getModifiers())) {
                    classFieldList.add(classField);
                } else if (classField.getMethod != null && Modifier
                        .isPublic(classField.getMethod.getModifiers())) {
                    classFieldList.add(classField);
                }
            }
        }
        return false;
    }

    private LinkedList<OkJsonClassField> getClassFieldListFromCache(Class<?> clazz)
    {
        LinkedList<OkJsonClassField> classFieldList;
        classFieldList = classMapFieldListCache.get(clazz.getName());
        if (classFieldList == null) {
            classFieldList = new LinkedList<>();
            classMapFieldListCache.put(clazz.getName(), classFieldList);
        }
        return classFieldList;
    }

    private void processLocalDateTimeString(OkJsonCharArrayBuilder jsonCharArrayBuilder,
                                            OkJsonClassField classField,
                                            String localDateTimeString,
                                            int depth)
    {
        if (localDateTimeString != null) {
            if (prettyFormatEnable) {
                jsonCharArrayBuilder.appendTabs(depth + 1)
                                    .appendJsonNameAndColonAndQmStringQmPretty(classField.fieldName,
                                            localDateTimeString);
            } else {
                jsonCharArrayBuilder.appendJsonNameAndColonAndQmStringQm(classField.fieldName,
                        localDateTimeString);
            }
        } else {
            if (prettyFormatEnable) {
                jsonCharArrayBuilder.appendTabs(depth + 1)
                                    .appendJsonNameAndColonAndStringPretty(classField.fieldName,
                                            NULL_STRING);
            } else {
                jsonCharArrayBuilder
                        .appendJsonNameAndColonAndString(classField.fieldName, NULL_STRING);
            }
        }
    }

    private Object getValue(OkJsonClassField classField, Object object)
    throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        Object subObject;
        if (classField.getMethod != null) {
            subObject = classField.getMethod.invoke(object);
        } else {
            subObject = classField.field.get(object);
        }
        return subObject;
    }

    private ClassFieldProcessor getClassFieldProcessor(OkJsonCharArrayBuilder jsonCharArrayBuilder,
                                                       OkJsonClassField classField,
                                                       Object object,
                                                       int depth)
    {
        switch (classField.type) {
        case CLASSFIELDTYPE_STRING:
            return new ClassFieldProcessorOfString(jsonCharArrayBuilder, classField, object, depth);
        case CLASSFIELDTYPE_NOT_STRING:
            return new ClassFieldProcessorOfNotString(jsonCharArrayBuilder, classField, object, depth);
        case CLASSFIELDTYPE_LOCALDATE:
            return new ClassFieldProcessorOfLocalDate(jsonCharArrayBuilder, classField, object, depth);
        case CLASSFIELDTYPE_LOCALTIME:
            return new ClassFieldProcessorOfLocalTime(jsonCharArrayBuilder, classField, object, depth);
        case CLASSFIELDTYPE_LOCALDATETIME:
            return new ClassFieldProcessorOfLocalDateTime(jsonCharArrayBuilder, classField, object, depth);
        case CLASSFIELDTYPE_LIST:
            return new ClassFieldProcessorOfList(jsonCharArrayBuilder, classField, object, depth);
        case CLASSFIELDTYPE_SUBCLASS:
            return new ClassFieldProcessorOfSubClass(jsonCharArrayBuilder, classField, object, depth);
        default:
            throw new IllegalStateException("Unexpected value: " + classField.type);
        }

    }

    interface ClassFieldProcessor {

        void process() throws Exception;
    }

    class ClassFieldProcessorOfString implements ClassFieldProcessor {

        OkJsonCharArrayBuilder jsonCharArrayBuilder;
        OkJsonClassField       classField;
        Object                 object;
        int                    depth;

        ClassFieldProcessorOfString(OkJsonCharArrayBuilder jsonCharArrayBuilder,
                                    OkJsonClassField classField,
                                    Object object,
                                    int depth)
        {
            this.jsonCharArrayBuilder = jsonCharArrayBuilder;
            this.classField = classField;
            this.object = object;
            this.depth = depth;
        }

        @Override
        public void process() throws Exception
        {
            String string = (String) getValue(classField, object);
            string = unfoldEscape(string);
            processLocalDateTimeString(jsonCharArrayBuilder, classField, string, depth);
        }
    }

    class ClassFieldProcessorOfNotString implements ClassFieldProcessor {

        OkJsonCharArrayBuilder jsonCharArrayBuilder;
        OkJsonClassField       classField;
        Object                 object;
        int                    depth;

        ClassFieldProcessorOfNotString(OkJsonCharArrayBuilder jsonCharArrayBuilder,
                                       OkJsonClassField classField,
                                       Object object,
                                       int depth)
        {
            this.jsonCharArrayBuilder = jsonCharArrayBuilder;
            this.classField = classField;
            this.object = object;
            this.depth = depth;
        }

        @Override
        public void process() throws Exception
        {
            Object value = getValue(classField, object);
            if (value != null) {
                if (prettyFormatEnable) {
                    jsonCharArrayBuilder.appendTabs(depth + 1)
                                        .appendJsonNameAndColonAndStringPretty(
                                                classField.fieldName, value.toString());
                } else {
                    jsonCharArrayBuilder
                            .appendJsonNameAndColonAndString(classField.fieldName,
                                    value.toString());
                }
            } else {
                if (prettyFormatEnable) {
                    jsonCharArrayBuilder.appendTabs(depth + 1)
                                        .appendJsonNameAndColonAndStringPretty(
                                                classField.fieldName, NULL_STRING);
                } else {
                    jsonCharArrayBuilder
                            .appendJsonNameAndColonAndString(classField.fieldName,
                                    NULL_STRING);
                }
            }
        }
    }

    class ClassFieldProcessorOfLocalDate implements ClassFieldProcessor {

        OkJsonCharArrayBuilder jsonCharArrayBuilder;
        OkJsonClassField       classField;
        Object                 object;
        int                    depth;

        ClassFieldProcessorOfLocalDate(OkJsonCharArrayBuilder jsonCharArrayBuilder,
                                       OkJsonClassField classField,
                                       Object object,
                                       int depth)
        {
            this.jsonCharArrayBuilder = jsonCharArrayBuilder;
            this.classField = classField;
            this.object = object;
            this.depth = depth;
        }

        @Override
        public void process() throws Exception
        {
            LocalDate localDate       = (LocalDate) getValue(classField, object);
            String    localDateString = formatLocalDate(classField, localDate);
            processLocalDateTimeString(jsonCharArrayBuilder, classField, localDateString, depth);
        }
    }

    class ClassFieldProcessorOfLocalTime implements ClassFieldProcessor {

        OkJsonCharArrayBuilder jsonCharArrayBuilder;
        OkJsonClassField       classField;
        Object                 object;
        int                    depth;

        ClassFieldProcessorOfLocalTime(OkJsonCharArrayBuilder jsonCharArrayBuilder,
                                       OkJsonClassField classField,
                                       Object object,
                                       int depth)
        {
            this.jsonCharArrayBuilder = jsonCharArrayBuilder;
            this.classField = classField;
            this.object = object;
            this.depth = depth;
        }

        @Override
        public void process() throws Exception
        {
            LocalTime localTime       = (LocalTime) getValue(classField, object);
            String    localTimeString = formatLocalTime(classField, localTime);
            processLocalDateTimeString(jsonCharArrayBuilder, classField, localTimeString,
                    depth);
        }
    }

    class ClassFieldProcessorOfLocalDateTime implements ClassFieldProcessor {

        OkJsonCharArrayBuilder jsonCharArrayBuilder;
        OkJsonClassField       classField;
        Object                 object;
        int                    depth;

        ClassFieldProcessorOfLocalDateTime(OkJsonCharArrayBuilder jsonCharArrayBuilder,
                                           OkJsonClassField classField,
                                           Object object,
                                           int depth)
        {
            this.jsonCharArrayBuilder = jsonCharArrayBuilder;
            this.classField = classField;
            this.object = object;
            this.depth = depth;
        }

        @Override
        public void process() throws Exception
        {
            LocalDateTime localDateTime = (LocalDateTime) getValue(classField, object);
            String localDateTimeString = formatLocalDateTime(classField, localDateTime);
            processLocalDateTimeString(jsonCharArrayBuilder, classField, localDateTimeString, depth);
        }
    }

    class ClassFieldProcessorOfList implements ClassFieldProcessor {

        OkJsonCharArrayBuilder jsonCharArrayBuilder;
        OkJsonClassField       classField;
        Object                 object;
        int                    depth;

        ClassFieldProcessorOfList(OkJsonCharArrayBuilder jsonCharArrayBuilder,
                                  OkJsonClassField classField,
                                  Object object,
                                  int depth)
        {
            this.jsonCharArrayBuilder = jsonCharArrayBuilder;
            this.classField = classField;
            this.object = object;
            this.depth = depth;
        }

        @Override
        public void process() throws Exception
        {
            int nret = 0;
            List<Object> array = (List<Object>) getValue(classField, object);
            if (array != null) {
                int arrayCount = array.size();
                if (arrayCount > 0) {
                    if (prettyFormatEnable) {
                        jsonCharArrayBuilder.appendTabs(depth + 1)
                                            .appendJsonNameAndColonAndOpenBytePretty(
                                                    classField.fieldName, '[');
                        nret = objectToListString(array, arrayCount, classField,
                                jsonCharArrayBuilder, depth + 1);
                        if (nret != 0) {
                            throw new ObjectToListStringException(nret);
                        }
                        jsonCharArrayBuilder.appendTabs(depth + 1).appendChar(']');
                    } else {
                        jsonCharArrayBuilder
                                .appendJsonNameAndColonAndOpenByte(classField.fieldName,
                                        '[');
                        nret = objectToListString(array, arrayCount, classField,
                                jsonCharArrayBuilder, depth + 1);
                        if (nret != 0) {
                            throw new ObjectToListStringException(nret);
                        }
                        jsonCharArrayBuilder.appendCloseByte(']');
                    }
                }
            } else {
                if (prettyFormatEnable) {
                    jsonCharArrayBuilder.appendTabs(depth + 1)
                                        .appendJsonNameAndColonAndStringPretty(
                                                classField.fieldName, NULL_STRING);
                } else {
                    jsonCharArrayBuilder
                            .appendJsonNameAndColonAndString(classField.fieldName,
                                    NULL_STRING);
                }
            }
        }
    }

    class ClassFieldProcessorOfSubClass implements ClassFieldProcessor {

        OkJsonCharArrayBuilder jsonCharArrayBuilder;
        OkJsonClassField       classField;
        Object                 object;
        int                    depth;

        ClassFieldProcessorOfSubClass(OkJsonCharArrayBuilder jsonCharArrayBuilder,
                                      OkJsonClassField classField,
                                      Object object,
                                      int depth)
        {
            this.jsonCharArrayBuilder = jsonCharArrayBuilder;
            this.classField = classField;
            this.object = object;
            this.depth = depth;
        }

        @Override
        public void process() throws Exception
        {
            int nret = 0;
            Object subObject = getValue(classField, object);
            if (subObject != null) {
                if (prettyFormatEnable) {
                    jsonCharArrayBuilder.appendTabs(depth + 1)
                                        .appendJsonNameAndColonAndOpenBytePretty(
                                                classField.fieldName, '{');
                    nret = objectToPropertiesString(subObject, jsonCharArrayBuilder,
                            depth + 1);
                    if (nret != 0) {
                        throw new ObjectToPropertiesStringException(nret);
                    }
                    jsonCharArrayBuilder.appendTabs(depth + 1).appendChar('}');
                } else {
                    jsonCharArrayBuilder
                            .appendJsonNameAndColonAndOpenByte(classField.fieldName, '{');
                    nret = objectToPropertiesString(subObject, jsonCharArrayBuilder,
                            depth + 1);
                    if (nret != 0) {
                        throw new ObjectToPropertiesStringException(nret);
                    }
                    jsonCharArrayBuilder.appendCloseByte('}');
                }
            } else {
                if (prettyFormatEnable) {
                    jsonCharArrayBuilder.appendTabs(depth + 1)
                                        .appendJsonNameAndColonAndStringPretty(
                                                classField.fieldName, NULL_STRING);
                } else {
                    jsonCharArrayBuilder
                            .appendJsonNameAndColonAndString(classField.fieldName,
                                    NULL_STRING);
                }
            }
        }
    }

    class ObjectToListStringException extends Exception {
        int nret = 0;
        ObjectToListStringException(int nret) {
            this.nret = nret;
        }

        public int getNret()
        {
            return nret;
        }

        public void setNret(int nret)
        {
            this.nret = nret;
        }
    }
    class ObjectToPropertiesStringException extends Exception {
        int nret = 0;
        ObjectToPropertiesStringException(int nret) {
            this.nret = nret;
        }

        public int getNret()
        {
            return nret;
        }

        public void setNret(int nret)
        {
            this.nret = nret;
        }
    }


}