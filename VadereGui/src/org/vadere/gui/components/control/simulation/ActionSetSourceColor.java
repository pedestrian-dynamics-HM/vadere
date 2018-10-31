package org.vadere.gui.components.control.simulation;

import java.awt.Color;

import javax.swing.JPanel;

import org.vadere.gui.components.control.simulation.ActionSetColor;
import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.model.SimulationModel;

public class ActionSetSourceColor extends ActionSetColor {

	public ActionSetSourceColor(String name, SimulationModel<? extends DefaultSimulationConfig> model, JPanel coloredPanel) {
		super(name, model, coloredPanel);
	}

	@Override
	protected void saveColor(Color color) {
		model.config.setSourceColor(color);
	}
}
