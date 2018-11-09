package org.vadere.gui.components.control.simulation;

import java.awt.Color;

import javax.swing.JPanel;

import org.vadere.gui.components.control.simulation.ActionSetColor;
import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.model.SimulationModel;

public class ActionSetObstacleColor extends ActionSetColor {

	public ActionSetObstacleColor(String name, SimulationModel<? extends DefaultSimulationConfig> model, JPanel coloredPanel) {
		super(name, model, coloredPanel);
	}

	@Override
	protected void saveColor(Color color) {
		model.config.setObstacleColor(color);
	}
}
