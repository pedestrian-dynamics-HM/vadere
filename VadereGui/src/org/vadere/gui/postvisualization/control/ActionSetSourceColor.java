package org.vadere.gui.postvisualization.control;

import java.awt.Color;

import javax.swing.JPanel;

import org.vadere.gui.postvisualization.model.PostvisualizationModel;

public class ActionSetSourceColor extends ActionSetColor {

	public ActionSetSourceColor(String name, PostvisualizationModel model, JPanel coloredPanel) {
		super(name, model, coloredPanel);
	}

	@Override
	protected void saveColor(Color color) {
		model.config.setSourceColor(color);
	}
}
