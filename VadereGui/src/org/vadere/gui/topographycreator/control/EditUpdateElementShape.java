package org.vadere.gui.topographycreator.control;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.util.geometry.shapes.VShape;

public class EditUpdateElementShape extends AbstractUndoableEdit {
	private static final long serialVersionUID = 3895685571385728777L;

	private final IDrawPanelModel panelModel;
	private final VShape oldShape;
	private final ScenarioElement element;
	private final VShape newShape;

	public EditUpdateElementShape(final IDrawPanelModel panelModel, final ScenarioElement element,
			final VShape oldShape) {
		this.panelModel = panelModel;
		this.oldShape = oldShape;
		this.newShape = element.getShape();
		this.element = element;
	}

	@Override
	public void undo() throws CannotUndoException {
		AttributeModifier.setShapeToAttributes(element, oldShape);
		panelModel.setSelectedElement(element);
		panelModel.notifyObservers();
	}

	@Override
	public void redo() throws CannotRedoException {
		AttributeModifier.setShapeToAttributes(element, newShape);
		panelModel.setSelectedElement(element);
		panelModel.notifyObservers();
	}

	@Override
	public boolean canUndo() {
		return true;
	}

	@Override
	public boolean canRedo() {
		return true;
	}

	@Override
	public String getPresentationName() {
		return "change element attributes";
	}
}
