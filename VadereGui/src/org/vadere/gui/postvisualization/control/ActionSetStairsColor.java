package org.vadere.gui.postvisualization.control;

import javax.swing.*;

import org.vadere.gui.postvisualization.model.PostvisualizationModel;

import java.awt.*;

public class ActionSetStairsColor extends ActionSetColor {

	public ActionSetStairsColor(final String name, final PostvisualizationModel model, final JPanel coloredPanel) {
		super(name, model, coloredPanel);
	}

	@Override
	protected void saveColor(Color color) {
		model.config.setStairColor(color);
	}

}
