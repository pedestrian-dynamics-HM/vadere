package org.vadere.gui.topographycreator.control;

import java.lang.reflect.Field;

import org.vadere.gui.topographycreator.model.AgentWrapper;
import org.vadere.simulator.projects.Scenario;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;

/**
 * Setter and getter implementation to modify Attributes. This class uses use of reflection.
 * Do not use this class outside of the topographycreator package, or even not outside this
 * control-package!
 * 
 */
public class ReflectionAttributeModifier {
	/**
	 * Sets the shape to the attributes of an topography element. Use this method only in the
	 * control!
	 * 
	 * @param element the attributes
	 * @param shape the shape
	 */
	static void setShapeToAttributes(final ScenarioElement element, final VShape shape) {
		try {
			Field field;
			if (element instanceof AgentWrapper) {
				double x = shape.getBounds2D().getCenterX();
				double y = shape.getBounds2D().getCenterY();
				((AgentWrapper) element).getAgentInitialStore().setPosition(new VPoint(x, y));
			} else {
				Attributes attributes = element.getAttributes(); // replaces Relection code from above

				//TODO: issue #91 Cannot easily replace the relection in the following code. Some Attributes classes have a
				//TODO: setShape(shape) method such as AttributesStairs, but not all, so there is no guarantee...
				//TODO: If there is no field "shape", then only the stacktrace is printed...
				field = attributes.getClass().getDeclaredField("shape");
				field.setAccessible(true);
				field.set(attributes, shape);
			}
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}
}
