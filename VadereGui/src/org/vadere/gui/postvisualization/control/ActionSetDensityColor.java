package org.vadere.gui.postvisualization.control;

import java.awt.Color;

import javax.swing.JPanel;

import org.vadere.gui.postvisualization.model.PostvisualizationModel;

public class ActionSetDensityColor extends ActionSetColor {

	public ActionSetDensityColor(String name, PostvisualizationModel model, JPanel coloredPanel) {
		super(name, model, coloredPanel);
	}

	@Override
	protected void saveColor(Color color) {
		model.config.setDensityColor(color);
	}

}
