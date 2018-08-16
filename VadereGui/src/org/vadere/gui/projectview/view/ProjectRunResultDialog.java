package org.vadere.gui.projectview.view;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.gui.projectview.model.ProjectViewModel;
import org.vadere.simulator.projects.ProjectFinishedListener;
import org.vadere.simulator.projects.SimulationResult;
import org.vadere.simulator.projects.VadereProject;

import java.util.LinkedList;

import javax.swing.*;

public class ProjectRunResultDialog implements ProjectFinishedListener {
	private static Logger logger = LogManager.getLogger(ProjectRunResultDialog.class);

	private final ProjectView projectView;
	private final ProjectViewModel projectViewModel;

	public ProjectRunResultDialog(ProjectView projectView, ProjectViewModel projectViewModel) {
		this.projectView = projectView;
		this.projectViewModel = projectViewModel;
	}

	@Override
	public void preProjectRun(VadereProject project) {

	}

	@Override
	public void postProjectRun(VadereProject project) {
		LinkedList<SimulationResult> simulationResultList = project.getSimulationResults();
		StringBuilder sb = new StringBuilder();
		for (SimulationResult res : simulationResultList) {
			sb.append(res.getScenarioName()).append(":\n")
					.append("    Runtime: ").append(res.getRunTimeAsString()).append("\n")
					.append("    Overlaps: ").append(res.getTotalOverlaps()).append("\n")
					.append("    State: ").append(res.getState()).append("\n\n");
		}

		String title = simulationResultList.size() > 1 ? "Simulation Results" : "Simulation Result";
		String infoMessage = sb.toString();

		if (projectViewModel.isShowSimulationResultDialog()){
			SwingUtilities.invokeLater(() -> {
				JOptionPane.showMessageDialog(projectView, infoMessage, title, JOptionPane.INFORMATION_MESSAGE);
			});
		} else {
			logger.info(sb.toString());
		}

	}
}
