package org.vadere.gui.projectview.control;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.gui.projectview.model.ProjectViewModel;
import org.vadere.gui.projectview.model.ProjectViewModel.OutputBundle;
import org.vadere.simulator.control.OfflineSimulation;
import org.vadere.simulator.projects.ScenarioRunManager;
import org.vadere.simulator.projects.io.IOOutput;

import java.awt.event.ActionEvent;
import java.io.IOException;

import javax.swing.*;

public class ActionRunOutput extends AbstractAction {

	private static final long serialVersionUID = 1L;

	private static Logger logger = LogManager.getLogger(ActionRunOutput.class);

	private ProjectViewModel model;

	public ActionRunOutput(final String name, final ProjectViewModel model) {
		super(name);
		this.model = model;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		try {
			OutputBundle outputBundle = model.getSelectedOutputBundle();

			String directoryName = outputBundle.getDirectory().getName();
			try {
				ScenarioRunManager vadere =
						IOOutput.readScenarioRunManager(outputBundle.getProject(), directoryName);
				OfflineSimulation offlineSimulation = new OfflineSimulation(
						IOOutput.readTrajectories(outputBundle.getProject(), vadere, directoryName),
						vadere, model.getProject().getOutputDir());
				offlineSimulation.run();
			} catch (IOException e1) {
				logger.error("Could not run offline simulation (simulate output):"
						+ e1.getLocalizedMessage());
				e1.printStackTrace();
			}
		} catch (IOException e2) {
			e2.printStackTrace();
		}
	}
}
