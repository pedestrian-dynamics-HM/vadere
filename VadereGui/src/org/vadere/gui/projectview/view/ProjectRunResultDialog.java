package org.vadere.gui.projectview.view;

import org.vadere.simulator.projects.ProjectFinishedListener;
import org.vadere.simulator.projects.SimulationResult;
import org.vadere.simulator.projects.VadereProject;

import java.util.LinkedList;

import javax.swing.*;

public class ProjectRunResultDialog implements ProjectFinishedListener {


	@Override
	public void preProjectRun(VadereProject project) {

	}

	@Override
	public void postProjectRun(VadereProject project) {
		LinkedList<SimulationResult> simulationResultList = project.getSimulationResults();
		String title = "Simulation Result: " + simulationResultList.getFirst().getScenarioName();
		String infoMessage = "TotalOverlap: " + simulationResultList.getFirst().getTotalOverlaps();
		// todo parentComponten to ProjectView?????
		JOptionPane.showMessageDialog(null, infoMessage, title, JOptionPane.INFORMATION_MESSAGE);
	}
}
