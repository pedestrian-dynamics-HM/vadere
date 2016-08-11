package org.vadere.gui.projectview.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;



@Deprecated
public abstract class JsonFieldValidator {

	private static Map<Class, Set<String>> fieldNamesMap = new HashMap<>();

	public static void validateFields(Class cls, String jsonString) throws JsonParseException {
		JsonObject jsonObj = new JsonParser().parse(jsonString).getAsJsonObject();
		Set<String> classFieldNames = getFieldNames(cls);
		for (Map.Entry<String, JsonElement> entry : jsonObj.entrySet())
			if (!classFieldNames.contains(entry.getKey()))
				throw new JsonParseException("The field \"" + entry.getKey() + "\" is unknown");
	}

	public static Set<String> getFieldNames(Class cls) {
		if (fieldNamesMap.containsKey(cls))
			return fieldNamesMap.get(cls);
		Set<String> fieldNames = new HashSet<>();
		for (Field field : cls.getDeclaredFields())
			fieldNames.add(field.getName());
		fieldNamesMap.put(cls, fieldNames);
		return fieldNames;
	}

	public static void clear() {
		fieldNamesMap.clear();
	}

}
