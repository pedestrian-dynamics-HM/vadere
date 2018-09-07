package org.vadere.simulator.entrypoints;


import java.lang.reflect.Field;

import org.vadere.state.attributes.Attributes;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.state.scenario.Topography;

/**
 * Setter and getter implementation to modify Attributes. This class uses use of reflection.
 * Do not use this class outside of the topographycreator package, or even not outside this
 * control-package!
 * 
 */
public class ReflectionAttributeModifier {

    /**
     * Sets the attribute to the topography element. Use this method only in the control!
     *
     * @param element the topography element
     * @param attributes the attributes
     */
    public static void setAttributes(final Topography element, final Attributes attributes) {
        try {
            Field field = element.getClass().getDeclaredField("attributes");
            field.setAccessible(true);
            field.set(element, attributes);

        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

	/**
	 * Sets the attribute to the topography element. Use this method only in the control!
	 * 
	 * @param element the topography element
	 * @param attributes the attributes
	 */
	public static void setAttributes(final ScenarioElement element, final Attributes attributes) {
		try {
			Field field = element.getClass().getDeclaredField("attributes");
			field.setAccessible(true);
			field.set(element, attributes);

		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns the attributes of a topograpyh element. Do not use this method outside of the
	 * topography-package!
	 * 
	 * @param element the topography element
	 * @return the attributes of element
	 */
	public static Attributes getAttributes(final ScenarioElement element) {
		Field field;
		try {
			field = element.getClass().getDeclaredField("attributes");
			field.setAccessible(true);
			return (Attributes) field.get(element);

		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
	}
}
