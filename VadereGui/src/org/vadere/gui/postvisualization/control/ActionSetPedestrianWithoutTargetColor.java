package org.vadere.gui.postvisualization.control;

import java.awt.Color;

import javax.swing.JPanel;

import org.vadere.gui.postvisualization.model.PostvisualizationModel;

public class ActionSetPedestrianWithoutTargetColor extends ActionSetColor {
	public ActionSetPedestrianWithoutTargetColor(String name, PostvisualizationModel model, JPanel coloredPanel) {
		super(name, model, coloredPanel);
	}

	@Override
	protected void saveColor(Color color) {
		model.config.setPedestrianColor(-1, color);
	}
}
