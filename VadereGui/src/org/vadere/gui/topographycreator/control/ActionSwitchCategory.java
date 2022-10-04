package org.vadere.gui.topographycreator.control;

import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.state.types.ScenarioElementType;

import java.awt.event.ActionEvent;

/**
 * Action: Switches the ScenarioElementType of the IDrawPanelModel to a specific type.
 * 
 * 
 */
public class ActionSwitchCategory extends TopographyAction {

	private static final long serialVersionUID = 7141800098375867819L;
	private final ScenarioElementType type;
	private final TopographyAction action;

	public ActionSwitchCategory(final String name, final IDrawPanelModel panelModel, final ScenarioElementType type,
			final TopographyAction action) {
		super(name, panelModel);
		this.type = type;
		this.action = action;
	}

	public ActionSwitchCategory(final String name, final String icon, final IDrawPanelModel panelModel,
			final ScenarioElementType type, final TopographyAction action) {
		super(name, icon, panelModel);
		this.type = type;
		this.action = action;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// PaintMethodDialog.hideDialog();
		getScenarioPanelModel().switchType(type);
		action.actionPerformed(e);
	}
}
