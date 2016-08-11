package org.vadere.gui.topographycreator.control;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;

import javax.swing.undo.UndoableEdit;
import javax.swing.undo.UndoableEditSupport;

import org.vadere.gui.topographycreator.model.AgentWrapper;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.AttributesBuilder;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.state.scenario.ScenarioElementBuilder;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;

/**
 * Insert the last copied element in the topography if this element is not null.
 * 
 *
 */
public class ActionInsertCopiedElement extends TopographyAction {
	private static final long serialVersionUID = 5049099647921341318L;
	private final UndoableEditSupport undoSupport;

	public ActionInsertCopiedElement(final String name, final IDrawPanelModel model,
			final UndoableEditSupport undoSupport) {
		super(name, model);
		this.undoSupport = undoSupport;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		IDrawPanelModel model = getScenarioPanelModel();
		ScenarioElement element = model.getCopiedElement();

		if (element != null) {
			// 1. reposition the copy
			VPoint elementPos;

			Rectangle2D rect = element.getShape().getBounds2D();

			if (element.getShape() instanceof VCircle) {
				elementPos = new VPoint(rect.getCenterX(), rect.getCenterY());
			} else {
				elementPos = new VPoint(rect.getX(), rect.getY());
			}

			VPoint diff = model.getMousePosition().subtract(elementPos);
			VShape newShape = model.translate(diff);
			ScenarioElement newElement = null;

			if (element instanceof AgentWrapper) {
				VPoint position = new VPoint(newShape.getBounds2D().getCenterX(), newShape.getBounds2D().getCenterY());
				newElement = element.clone();
				((AgentWrapper) newElement).getAgentInitialStore().setPosition(position);
			} else {
				// change attributes with reflection!
				ScenarioElementBuilder<ScenarioElement> elBuilder = new ScenarioElementBuilder<>(element);
				AttributesBuilder<Attributes> attBuilder = new AttributesBuilder<Attributes>(element.getAttributes());
				attBuilder.setField("shape", newShape);
				elBuilder.setAttributes(attBuilder.build());
				newElement = elBuilder.build();
			}

			// 2. add the copy
			UndoableEdit edit = new EditDrawShape(model, newElement.getType());
			undoSupport.postEdit(edit);
			model.addShape(newElement);
			// getScenarioPanelModel().setSelectedElement(element);
			model.notifyObservers();
		} else {
			Toolkit.getDefaultToolkit().beep();
		}
	}
}
