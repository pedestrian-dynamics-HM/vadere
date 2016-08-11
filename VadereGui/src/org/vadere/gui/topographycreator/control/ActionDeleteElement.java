package org.vadere.gui.topographycreator.control;

import javax.swing.undo.UndoableEdit;
import javax.swing.undo.UndoableEditSupport;

import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.state.scenario.ScenarioElement;

import java.awt.event.ActionEvent;

public class ActionDeleteElement extends TopographyAction {

	final UndoableEditSupport undoSupport;
	final TopographyAction basicAction;

	public ActionDeleteElement(final String name, final IDrawPanelModel model, final UndoableEditSupport undoSupport) {
		this(name, model, undoSupport, null);
	}

	public ActionDeleteElement(final String name, final IDrawPanelModel model, final UndoableEditSupport undoSupport,
			final TopographyAction basicAction) {
		super(name, model);
		this.undoSupport = undoSupport;
		this.basicAction = basicAction;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		IDrawPanelModel model = getScenarioPanelModel();

		if (model.isElementSelected()) {
			ScenarioElement element = model.getSelectedElement();
			if (model.removeElement(element)) {
				UndoableEdit edit = new EditDeleteShape(model, element);
				undoSupport.postEdit(edit);
			}
		}

		if (basicAction != null) {
			basicAction.actionPerformed(e);
		}
	}
}
