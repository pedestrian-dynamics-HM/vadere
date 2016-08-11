package org.vadere.gui.postvisualization.control;

import java.awt.Color;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import org.vadere.gui.postvisualization.model.PostvisualizationModel;

public class ActionSetPedestrianColor extends ActionSetColor {
	private final JComboBox<Integer> comboBox;

	public ActionSetPedestrianColor(final String name, final PostvisualizationModel model, final JPanel coloredPanel,
			final JComboBox<Integer> comboBox) {
		super(name, model, coloredPanel);
		this.comboBox = comboBox;
	}

	@Override
	protected void saveColor(Color color) {
		model.config.setPedestrianColor(comboBox.getSelectedIndex() + 1, color);
	}
}
