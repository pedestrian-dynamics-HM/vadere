package org.vadere.gui.postvisualization.control;

import java.awt.Color;

import javax.swing.JPanel;

import org.vadere.gui.postvisualization.model.PostvisualizationModel;

public class ActionSetObstacleColor extends ActionSetColor {

	public ActionSetObstacleColor(String name, PostvisualizationModel model, JPanel coloredPanel) {
		super(name, model, coloredPanel);
	}

	@Override
	protected void saveColor(Color color) {
		model.config.setObstacleColor(color);
	}
}
