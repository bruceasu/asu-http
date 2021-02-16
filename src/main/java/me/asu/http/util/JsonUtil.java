package me.asu.http.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import java.io.IOException;

/**
 * Created by Administrator on 2020/5/15.
 */
public class JsonUtil
{

	private static final ObjectMapper mapper = new ObjectMapper();

	static {
		mapper.configure(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY, true)
		      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	/**
	 * 假设content是json数据, 如果不是json数据则抛IOException异常。
	 */
	public static JsonNode toJson(String content) throws IOException
	{
		if (Strings.isEmpty(content)) {
			return NullNode.getInstance();
		}
		return mapper.readTree(content);
	}

	public static <T> T toJson(String content, TypeReference<T> type) throws IOException
	{
		return mapper.readValue(content, type);
	}

	public static <T> T toJson(String content, Class<T> clazz) throws IOException
	{
		return mapper.readValue(content, clazz);
	}

	public static String stringify(Object data) throws JsonProcessingException
	{
		return mapper.writeValueAsString(data);
	}
}
