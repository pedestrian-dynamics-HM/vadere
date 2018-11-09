package org.vadere.gui.postvisualization.control;

import java.awt.event.ActionEvent;

import javax.swing.Icon;

import org.vadere.gui.components.control.simulation.ActionVisualization;
import org.vadere.gui.postvisualization.model.PostvisualizationModel;

public class ActionStop extends ActionVisualization {

	private PostvisualizationModel model;

	public ActionStop(String name, Icon icon, PostvisualizationModel model) {
		super(name, icon, model);
		this.model = model;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Player.getInstance(model).stop();
	}
}
