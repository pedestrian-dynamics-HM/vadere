package org.vadere.gui.components.control.simulation;

import javax.swing.*;

import org.vadere.gui.components.control.simulation.ActionSetColor;
import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.model.SimulationModel;

import java.awt.*;

public class ActionSetStairsColor extends ActionSetColor {

	public ActionSetStairsColor(final String name, final SimulationModel<? extends DefaultSimulationConfig> model, final JPanel coloredPanel) {
		super(name, model, coloredPanel);
	}

	@Override
	protected void saveColor(Color color) {
		model.config.setStairColor(color);
	}

}
