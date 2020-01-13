package org.vadere.gui.projectview.control;

import org.vadere.gui.projectview.model.ProjectViewModel;
import org.vadere.util.config.VadereConfig;

import java.awt.event.ActionEvent;

import javax.swing.*;

public class ToggleScenarioManagerAction extends AbstractAction {

	ProjectViewModel model;
	JCheckBoxMenuItem item;

	public ToggleScenarioManagerAction(final String name, final ProjectViewModel model, JCheckBoxMenuItem item) {
		super(name);
		this.model = model;
		this.item = item;
		this.item.setState(VadereConfig.getConfig().getBoolean("Project.ScenarioChecker.active"));
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		VadereConfig.getConfig().setProperty("Project.ScenarioChecker.active", item.getState());
	}

}
