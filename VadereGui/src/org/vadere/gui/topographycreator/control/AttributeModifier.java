package org.vadere.gui.topographycreator.control;

import org.vadere.gui.topographycreator.model.AgentWrapper;
import org.vadere.state.attributes.scenario.AttributesVisualElement;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;

/**
 * Setter implementation to modify shape of Attributes.
 * Do not use this class outside of the topographycreator package, or even not outside this
 * control-package!
 *
 */
public class AttributeModifier {
	/**
	 * Sets the shape to the attributes of an topography element. Use this method only in the
	 * control!
	 * 
	 * @param element the attributes
	 * @param shape the shape
	 */
	public static void setShapeToAttributes(final ScenarioElement element, final VShape shape) {
		try {
			if (element instanceof AgentWrapper) {
				double x = shape.getBounds2D().getCenterX();
				double y = shape.getBounds2D().getCenterY();
				((AgentWrapper) element).getAgentInitialStore().setPosition(new VPoint(x, y));
			} else {
				AttributesVisualElement attributes = (AttributesVisualElement) element.getAttributes();
				attributes.setShape(shape);
			}
		} catch (ClassCastException e) {
			e.printStackTrace();
		}
	}
}
