package org.vadere.simulator.projects.io;


import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.annotations.Expose;

/**
 * Excludes every field that has a @Expose annotation.
 * 
 *
 */
@Deprecated
public class JsonProcessorExclusionStrategy implements ExclusionStrategy {

	@Override
	public boolean shouldSkipField(FieldAttributes f) {
		if (f.getAnnotation(Expose.class) == null) {
			return false;
		}
		return true;
	}

	@Override
	public boolean shouldSkipClass(Class<?> clazz) {
		return false;
	}

}
