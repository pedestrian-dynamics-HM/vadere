package org.vadere.gui.components.control.simulation;


import org.vadere.gui.components.control.DefaultSelectionMode;
import org.vadere.gui.components.control.DrawVoronoiDiagramMode;
import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.model.SimulationModel;
import org.vadere.util.logging.Logger;

import java.awt.event.ActionEvent;

import javax.swing.*;

public class ActionSwapSelectionMode extends ActionVisualization {

	private static Logger logger = Logger.getLogger(ActionSwapSelectionMode.class);

	public ActionSwapSelectionMode(final String name, final SimulationModel<? extends DefaultSimulationConfig> model) {
		super(name, model);
	}

	public ActionSwapSelectionMode(final String name, Icon icon, final  SimulationModel<? extends DefaultSimulationConfig> model) {
		super(name, icon, model);
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		if (model.getMouseSelectionMode() instanceof DrawVoronoiDiagramMode) {
			model.setMouseSelectionMode(new DefaultSelectionMode(model));
		} else {
			model.setMouseSelectionMode(new DrawVoronoiDiagramMode(model));
		}
		super.actionPerformed(e);
	}

}
