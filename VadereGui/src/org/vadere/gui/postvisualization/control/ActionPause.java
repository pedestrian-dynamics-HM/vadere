package org.vadere.gui.postvisualization.control;

import java.awt.event.ActionEvent;

import javax.swing.Icon;

import org.vadere.gui.components.control.simulation.ActionVisualization;
import org.vadere.gui.postvisualization.model.PostvisualizationModel;

public class ActionPause extends ActionVisualization {

	private final PostvisualizationModel model;

	public ActionPause(String name, Icon icon, PostvisualizationModel model) {
		super(name, icon, model);
		this.model = model;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Player.getInstance(model).pause();
	}
}
