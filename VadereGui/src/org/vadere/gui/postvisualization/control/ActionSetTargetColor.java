package org.vadere.gui.postvisualization.control;

import java.awt.Color;

import javax.swing.JPanel;

import org.vadere.gui.postvisualization.model.PostvisualizationModel;

public class ActionSetTargetColor extends ActionSetColor {

	public ActionSetTargetColor(final String name, final PostvisualizationModel model, final JPanel coloredPanel) {
		super(name, model, coloredPanel);
	}

	@Override
	protected void saveColor(Color color) {
		model.config.setTargetColor(color);
	}
}
