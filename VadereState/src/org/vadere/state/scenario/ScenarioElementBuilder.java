package org.vadere.state.scenario;

import java.lang.reflect.Field;

import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.AttributesBuilder;

public class ScenarioElementBuilder<T extends ScenarioElement> {

	private final T element;

	@SuppressWarnings("unchecked")
	public ScenarioElementBuilder(final T element) {
		this.element = (T) element.clone();
	}


	/**
	 * Sets the attribute to the topography element. This method is not type save.
	 * The attributes.getClass() has to fit to element.getClass();
	 * 
	 * @param attributes the attributes
	 */
	public void setAttributes(final Attributes attributes) {
		try {
			Field field = element.getClass().getDeclaredField("attributes");
			field.setAccessible(true);
			field.set(element, new AttributesBuilder<Attributes>(attributes).build());

		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public T build() {
		return (T) element.clone();
	}
}
