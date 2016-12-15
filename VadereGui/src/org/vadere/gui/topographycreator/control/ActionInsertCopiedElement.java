package org.vadere.gui.topographycreator.control;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;

import javax.swing.undo.UndoableEdit;
import javax.swing.undo.UndoableEditSupport;

import org.vadere.gui.topographycreator.model.AgentWrapper;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.state.scenario.ScenarioElement;
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

	public ActionInsertCopiedElement(final String name, final IDrawPanelModel<?> model,
			final UndoableEditSupport undoSupport) {
		super(name, model);
		this.undoSupport = undoSupport;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		IDrawPanelModel<?> model = getScenarioPanelModel();
		ScenarioElement elementToCopy = model.getCopiedElement();

		if (elementToCopy == null) {
			Toolkit.getDefaultToolkit().beep();
			return;
		}

		// 1. reposition the copy
		ScenarioElement newElement = elementToCopy.clone();
		VPoint elementPos = getElementPosition(elementToCopy);

		VPoint diff = model.getMousePosition().subtract(elementPos);
		VShape newShape = model.translateElement(elementToCopy, diff);

		if (elementToCopy instanceof AgentWrapper) {
			VPoint position = new VPoint(newShape.getBounds2D().getCenterX(), newShape.getBounds2D().getCenterY());
			((AgentWrapper) newElement).getAgentInitialStore().setPosition(position);
		} else {
			newElement.setShape(newShape);
		}

		// 2. add the copy
		UndoableEdit edit = new EditDrawShape(model, newElement.getType());
		undoSupport.postEdit(edit);
		model.addShape(newElement);
		// getScenarioPanelModel().setSelectedElement(element);
		model.notifyObservers();
	}

	private VPoint getElementPosition(ScenarioElement elementToCopy) {
		final Rectangle2D rect = elementToCopy.getShape().getBounds2D();
		if (elementToCopy.getShape() instanceof VCircle) {
			return new VPoint(rect.getCenterX(), rect.getCenterY());
		} else {
			return new VPoint(rect.getX(), rect.getY());
		}
	}

}
