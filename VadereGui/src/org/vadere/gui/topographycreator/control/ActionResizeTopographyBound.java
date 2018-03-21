package org.vadere.gui.topographycreator.control;

import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.gui.topographycreator.model.TopographyCreatorModel;

import java.awt.event.ActionEvent;

import javax.swing.*;

public class ActionResizeTopographyBound extends TopographyAction {

	private TopographyAction action;

	public ActionResizeTopographyBound(String name, ImageIcon icon, IDrawPanelModel<?> panelModel,
									   TopographyAction action) {
		super(name, icon, panelModel);
		this.action = action;
	}

	public ActionResizeTopographyBound(final String name, final IDrawPanelModel<?> panelModel,
									   TopographyAction action) {
		super(name, panelModel);
		this.action = action;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		//set to Selection to to be sure no accidental changes are introduced
		action.actionPerformed(e);

		TopographyCreatorModel model = (TopographyCreatorModel) getScenarioPanelModel();
		ActionResizeTopographyBoundDialog dialog = new ActionResizeTopographyBoundDialog(
				model.getTopography().getBounds().width,
				model.getTopography().getBounds().height
		);

		if (dialog.getValue()){
			model.setTopographyBound(dialog.getBound());
		}
		getScenarioPanelModel().notifyObservers();
	}
}
