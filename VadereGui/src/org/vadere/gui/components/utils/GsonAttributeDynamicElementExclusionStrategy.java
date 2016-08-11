package org.vadere.gui.components.utils;

import org.vadere.state.attributes.scenario.AttributesDynamicElement;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

/**
 * This Exclusion strategy exclude the {@link AttributesDynamicElement#id} from serialization. This
 * is useful if you want
 * to protect the id from changes via text.
 *
 */
public class GsonAttributeDynamicElementExclusionStrategy implements ExclusionStrategy {

	@Override
	public boolean shouldSkipField(final FieldAttributes f) {
		return (f.getDeclaringClass() == AttributesDynamicElement.class) && f.getName().equals("id");
	}

	@Override
	public boolean shouldSkipClass(final Class<?> clazz) {
		return false;
	}
}
