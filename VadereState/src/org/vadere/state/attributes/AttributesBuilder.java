package org.vadere.state.attributes;

import java.io.IOException;
import java.lang.reflect.Field;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.state.util.StateJsonConverter;
import org.vadere.state.util.TextOutOfNodeException;

public class AttributesBuilder<T extends Attributes> {

	private static Logger logger = LogManager.getLogger(AttributesBuilder.class);
	private final T attributes;

	public AttributesBuilder(T attributes) {
		this.attributes = cloneAttribute(attributes);
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

	public T build() {
		return (T) cloneAttribute(attributes);
	}
	
	@SuppressWarnings("unchecked")
	private T cloneAttribute(T attributes)
	{
		// TODO should we implement cloneable in Attributes instead?
		return (T) StateJsonConverter.deserializeObjectFromJson(StateJsonConverter.serializeObject(attributes), attributes.getClass());
	}
}
