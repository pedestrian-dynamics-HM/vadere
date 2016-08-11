package org.vadere.state.attributes;

import java.lang.reflect.Field;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

/**
 * Abstract class for all static simulation attributes. Provides reflection
 * based methods to convert the fields and values of the Attributes classes from
 * and to key-value stores.
 * 
 * 
 */
public abstract class Attributes {

	public Attributes() {}

	/**
	 * Retrieves the values from store and tries to set the fields of the given
	 * Attributes object.
	 * 
	 * @param store
	 */
	protected void fromKeyValueStore(JsonElement store) {
		Attributes newAtt = new Gson().fromJson(store, this.getClass());
		Field[] fields = this.getClass().getFields();
		for (Field field : fields) {
			Object value;
			try {
				value = newAtt.getClass().getField(field.getName()).get(newAtt);
				field.set(this, value);
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
				e.printStackTrace();
			}
		}
	}

	// public abstract Attributes clone();
}
