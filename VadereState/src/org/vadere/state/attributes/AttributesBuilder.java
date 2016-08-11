package org.vadere.state.attributes;

import java.lang.reflect.Field;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.util.io.IOUtils;

import com.google.gson.Gson;

public class AttributesBuilder<T extends Attributes> {

	private static Logger logger = LogManager.getLogger(AttributesBuilder.class);
	private final T attributes;
	private final Gson gson;

	@SuppressWarnings("unchecked")
    @Deprecated
	public AttributesBuilder(T attributes) {
		this.gson = IOUtils.getGson();
		this.attributes = (T) gson.fromJson(gson.toJson(attributes), attributes.getClass());
	}

	public void setField(String name, Object value) {
		Field field;
		try {
			field = attributes.getClass().getDeclaredField(name);
			field.setAccessible(true);
			field.set(attributes, value);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
			logger.error(e);
		}

	}

	@SuppressWarnings("unchecked")
	public T build() {
		return (T) gson.fromJson(gson.toJson(attributes), attributes.getClass());
	}
}
