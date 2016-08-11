package org.vadere.gui.topographycreator.control;

import java.awt.event.ActionEvent;

import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.state.scenario.ScenarioElement;

/**
 * Copies the reference of an ScenarioElement and put it into the panelmodel.
 * 
 *
 */
public class ActionCopyElement extends TopographyAction {

	private static final long serialVersionUID = -5926543529036212640L;

	public ActionCopyElement(final String name, final IDrawPanelModel model) {
		super(name, model);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		ScenarioElement element = getScenarioPanelModel().getSelectedElement();
		getScenarioPanelModel().setCopiedElement(element);
	}
}
