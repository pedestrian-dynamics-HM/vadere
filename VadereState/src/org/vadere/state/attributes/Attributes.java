package org.vadere.state.attributes;

import java.lang.reflect.Field;

import org.apache.commons.attributes.DefaultSealable;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

/**
 * Abstract class for all static simulation attributes. Provides reflection
 * based methods to convert the fields and values of the Attributes classes from
 * and to key-value store.
 * 
 * Attributes are "static" parameters i.e. they are immutable while a simulation
 * is running. This is implemented by deriving from the
 * {@link org.apache.commons.attributes.Sealable} interface. Attributes can have
 * setters, but the setters must call the {@code checkSealed()} method before
 * changing a value! In addition, if an attributes class contains other
 * attributes classes as fields, it must override {@link #seal()} to also seal
 * these objects. All other fields must be immutable (e.g. String, Double,
 * VPoint,...).
 * 
 */
public abstract class Attributes extends DefaultSealable {

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

}
