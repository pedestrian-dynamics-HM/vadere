package org.vadere.gui.onlinevisualization.control;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.gui.components.control.simulation.ActionVisualization;
import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.model.SimulationModel;

import java.awt.event.ActionEvent;

import javax.swing.*;

public class ActionShowMesh extends ActionVisualization {
	private static Logger logger = LogManager.getLogger(ActionShowMesh.class);

	public ActionShowMesh(final String name, Icon icon, final  SimulationModel<? extends DefaultSimulationConfig> model) {
		super(name, icon, model);
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		model.config.setShowTargetPotentielFieldMesh(!model.config.isShowTargetPotentielFieldMesh());
		model.notifyObservers();
		super.actionPerformed(e);
	}
}


