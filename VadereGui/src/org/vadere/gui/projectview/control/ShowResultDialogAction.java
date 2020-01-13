package org.vadere.gui.projectview.control;

import org.vadere.gui.projectview.model.ProjectViewModel;
import org.vadere.util.config.VadereConfig;

import java.awt.event.ActionEvent;

import javax.swing.*;

public class ShowResultDialogAction extends AbstractAction {

	ProjectViewModel model;
	JCheckBoxMenuItem item;

	public ShowResultDialogAction(final String name, final ProjectViewModel model, JCheckBoxMenuItem item) {
		super(name);
		this.model = model;
		this.item = item;
		this.item.setState(VadereConfig.getConfig().getBoolean("Project.simulationResult.show"));
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		VadereConfig.getConfig().setProperty("Project.simulationResult.show", item.getState());
		model.setShowSimulationResultDialog(item.getState());
	}
}
