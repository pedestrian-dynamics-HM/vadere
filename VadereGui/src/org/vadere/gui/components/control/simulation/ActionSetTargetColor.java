package org.vadere.gui.components.control.simulation;

import java.awt.Color;

import javax.swing.JPanel;

import org.vadere.gui.components.control.simulation.ActionSetColor;
import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.model.SimulationModel;

public class ActionSetTargetColor extends ActionSetColor {

	public ActionSetTargetColor(final String name, final SimulationModel<? extends DefaultSimulationConfig> model, final JPanel coloredPanel) {
		super(name, model, coloredPanel);
	}

	@Override
	protected void saveColor(Color color) {
		model.config.setTargetColor(color);
	}
}
