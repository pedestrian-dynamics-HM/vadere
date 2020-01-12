package org.vadere.gui.projectview.model;


import org.jetbrains.annotations.NotNull;
import org.vadere.gui.components.control.ActionScenarioChecker;
import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.projectview.control.IOutputFileRefreshListener;
import org.vadere.gui.projectview.control.IProjectChangeListener;
import org.vadere.gui.projectview.view.ProjectView;
import org.vadere.gui.projectview.view.ScenarioNamePanel;
import org.vadere.gui.projectview.view.ScenarioPanel;
import org.vadere.gui.projectview.view.VDialogManager;
import org.vadere.gui.projectview.view.VTable;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.simulator.projects.ProjectWriter;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.VadereProject;
import org.vadere.simulator.projects.io.IOOutput;
import org.vadere.util.config.VadereConfig;
import org.vadere.util.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import javax.swing.*;

public class ProjectViewModel implements IScenarioChecker {
	private static Logger logger = Logger.getLogger(ProjectViewModel.class);

	private VadereProject project;
	private Scenario currentScenario;

	private final OutputFileTableModel outputTableModel;
	private final VadereScenarioTableModel scenarioTableModel;
	//	private String currentProjectPath;
	private ExecutorService refreshOutputExecutor;


	// these are also part of the model, because only they know the current selected row
	private VTable scenarioTable;
	private VTable outputTable;

	private final Collection<IOutputFileRefreshListener> outputRefreshListeners;
	private final Collection<IProjectChangeListener> projectChangeListeners;
	private ScenarioNamePanel scenarioNamePanel; // to add or remove the "*" to indicate unsaved changes and ScenarioChecker indicator
	private boolean showSimulationResultDialog;

	public ProjectViewModel() {
		this.outputTableModel = new OutputFileTableModel();
		this.scenarioTableModel = new VadereScenarioTableModel();
		this.outputRefreshListeners = new LinkedList<>();
		this.projectChangeListeners = new LinkedList<>();
		this.project = null;
		this.refreshOutputExecutor = Executors.newSingleThreadExecutor();
		this.showSimulationResultDialog = VadereConfig.getConfig()
				.getBoolean("Project.simulationResult.show", true);
	}

	public void deleteOutputFiles(final int[] rows) throws IOException {
		// 1. delete output files on the hard disc
		int j = 0;
		for (int i = 0; i < rows.length; i++) {
			File dir = getOutputTableModel().getValue(rows[i] - j);

			if (IOOutput.deleteOutputDirectory(dir)) {
				j++;

				// 2. remove output files information from the output file table
				outputTable.getModel().remove(dir);

				// 3. remove output files information from the project output
				getProject().getProjectOutput().removeOutputDir(dir.getName());

				logger.info("output dir " + dir + " dir deleted.");
			}
		}
	}

	public void deleteScenarios(final int[] rows) {
		Arrays.stream(rows).boxed()
				.sorted((row1, row2) -> row2 - row1)
				.map(i -> getScenarioTableModel().getValue(i))
				.map(scenarioDisplay -> scenarioDisplay.scenarioRM)
				.forEach(scenario -> deleteScenario(scenario));
	}

	public String getDiffOfSelectedScenarios(final int[] rows) {
		StringBuilder collectDiffs = new StringBuilder();
		String eol = "\n---------------\n";
		getScenariosByRows(rows).forEach(scenario -> {
			String diff = scenario.getDiff();
			if (diff != null)
				collectDiffs.append("scenario <" + scenario.getName() + "> :" + diff + eol);
		});
		return collectDiffs.toString();
	}

	public void discardChangesOfSelectedScenarios(final int[] rows) {
		getScenariosByRows(rows).forEach(scenario -> scenario.discardChanges());
		ProjectView.getMainWindow().updateScenarioJPanel();
	}

	public void saveSelectedScenarios(final int[] rows) {
		getScenariosByRows(rows).forEach(scenario -> {
			try {
				saveScenarioToDisk(scenario);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	public void saveScenarioToDisk(Scenario scenario) throws IOException {
		ProjectWriter.writeScenarioFileJson(getCurrentProjectPath(), scenario);
		scenario.saveChanges();
	}

	private List<Scenario> getScenariosByRows(final int[] rows) {
		List<Scenario> scenarios = new ArrayList<>();
		Arrays.stream(rows).boxed() // TODO code [priority=medium] [task=refactoring] copied from deleteScenarios(), might be possible simpler?
				.sorted((row1, row2) -> row2 - row1)
				.map(i -> getScenarioTableModel().getValue(i))
				.map(scenarioDisplay -> scenarioDisplay.scenarioRM)
				.forEach(scenario -> scenarios.add(scenario));
		return scenarios;
	}

	public boolean selectedScenariosContainChangedOnes(final int[] rows) {
		for (Scenario srm : getScenariosByRows(rows))
			if (srm.hasUnsavedChanges())
				return true;
		return false;
	}

	private void deleteScenario(final Scenario scenario) {
		try {
			ProjectWriter.deleteScenario(scenario, getCurrentProjectPath());
			getProject().removeScenario(scenario);
			getScenarioTableModel().remove(scenario);
		} catch (IOException e) {
			e.printStackTrace();
			logger.error(e.getLocalizedMessage());
		}
	}

	public void refreshOutputTable() {
		refreshOutputExecutor.execute(new OutputRefresher());
	}

	public void addScenario(final Scenario scenario) {
		project.addScenario(scenario);
		getScenarioTableModel()
				.insertValue(new VadereScenarioTableModel.VadereDisplay(scenario, VadereState.INITIALIZED));
		getScenarioTableModel().fireTableDataChanged(); // TODO [priority=low] [task=refactoring] isn't this done by Swing?
	}

	public boolean isProjectAvailable() {
		return project != null;
	}

	public boolean isProjectEmpty() {
		return !isProjectAvailable() || project.getScenarios().isEmpty();
	}

	public boolean hasProjectChanged() { // = has a scenario file changed
		if (project == null)
			return false;
		return project.hasUnsavedChanges();
	}

	public VadereProject getProject() {
		return project;
	}

	public void setProject(final VadereProject project) {
		this.project = project;
		getScenarioTableModel().init(project);
		fireProjectChanged();
	}

	public OutputFileTableModel getOutputTableModel() {
		return outputTableModel;
	}

	public VadereScenarioTableModel getScenarioTableModel() {
		return scenarioTableModel;
	}

	/**
	 * Get path of the directory where the project is saved. It may be null if the model have not
	 * been saved to disk yet.
	 */
	public String getCurrentProjectPath() {

		return isProjectAvailable() ? project.getProjectDirectory().toAbsolutePath().toString() : null;
	}

	/**
	 * Set path of the directory where the project is saved. It may be null if the model have not
	 * been saved to disk yet.
	 */
	public void setCurrentProjectPath(@NotNull final String currentProjectPath) {
		if (isProjectAvailable())
			project.setProjectDirectory(Paths.get(currentProjectPath));
		else
			throw new IllegalStateException();
	}

	public OutputBundle getSelectedOutputBundle() throws IOException {
		File directory = outputTableModel.getValue(outputTable.getSelectedRow());
		Scenario scenarioRM = IOOutput.readScenarioRunManager(project, directory.getName());
		return new OutputBundle(directory, project, IOOutput.listSelectedOutputDirs(project, scenarioRM));
	}

	public ScenarioBundle getRunningScenario() {
		Scenario scenarioRM = project.getCurrentScenario();
		List<String> outputDirectories = project.getProjectOutput().listSelectedOutputDirs(scenarioRM)
				.stream().map(file -> file.getAbsolutePath()).collect(Collectors.toList());
		return new ScenarioBundle(project, scenarioRM, outputDirectories);
	}

	public ScenarioBundle getSelectedScenarioBundle() {
		Scenario scenarioRM = getSelectedScenarioRunManager();
		List<String> outputDirectories = project.getProjectOutput().listSelectedOutputDirs(scenarioRM)
				.stream().map(file -> file.getAbsolutePath()).collect(Collectors.toList());
		return new ScenarioBundle(project, scenarioRM, outputDirectories);
	}

	private Scenario getSelectedScenarioRunManager() {
		return scenarioTableModel.getValue(scenarioTable.getSelectedRow()).scenarioRM;
	}

	public Collection<Scenario> getScenarios(final int[] rows) {
		return Arrays.stream(rows)
				.boxed()
				.map(i -> getScenarioTableModel().getValue(i))
				.map(scenario -> scenario.scenarioRM)
				.collect(Collectors.toList());
	}

	public boolean isScenarioNameInConflict(final String name) {
		return isProjectAvailable()
				&& project.getScenarios().stream()
				.filter(scenario -> scenario.getName().equals(name))
				.findAny().isPresent();
	}

	public boolean getShowSimulationResultDialog() {
		return showSimulationResultDialog;
	}

	public void fireRefreshOutputStarted() {
		outputRefreshListeners.forEach(l -> l.preRefresh());
	}

	public void fireRefreshOutputCompleted() {
		outputRefreshListeners.forEach(l -> l.postRefresh());
	}

	public void addOutputFileRefreshListener(final IOutputFileRefreshListener listener) {
		outputRefreshListeners.add(listener);
	}

	public void fireProjectPropertyChanged() {
		projectChangeListeners.forEach(l -> l.propertyChanged(project));
	}

	public void fireProjectChanged() {
		projectChangeListeners.forEach(l -> l.projectChanged(project));
	}

	public void addProjectChangeListener(final IProjectChangeListener listener) {
		projectChangeListeners.add(listener);
	}

	public void setScenarioNamePanel(ScenarioNamePanel scenarioNamePanel) {
		this.scenarioNamePanel = scenarioNamePanel;
	}

	public void setScenarioNameLabelString(final String name) {
		this.scenarioNamePanel.setScenarioName(name);
	}

	private class OutputRefresher implements Runnable {
		@Override
		public void run() {
			fireRefreshOutputStarted();
			project.getProjectOutput().update();
			outputTableModel.init(project);
			fireRefreshOutputCompleted();
		}
	}

	public static class ScenarioBundle {
		private final Scenario scenarioRM;
		private final VadereProject project;
		private final Collection<String> outputDirectories;

		public ScenarioBundle(final VadereProject project, final Scenario scenarioRM,
							  final Collection<String> outputDirectories) {
			this.project = project;
			this.scenarioRM = scenarioRM;
			this.outputDirectories = outputDirectories;
		}

		public Collection<String> getOutputDirectories() {
			return outputDirectories;
		}

		public Scenario getScenario() {
			return scenarioRM;
		}

		public VadereProject getProject() {
			return project;
		}
	}

	public static class OutputBundle {
		private final File directory;
		private final VadereProject project;
		private final Collection<File> outputDirectories;

		public OutputBundle(final File directory, final VadereProject project,
							final Collection<File> outputDirectories) {
			this.directory = directory;
			this.project = project;
			this.outputDirectories = outputDirectories;
		}

		public File getDirectory() {
			return directory;
		}

		public VadereProject getProject() {
			return project;
		}

		public Collection<File> getOutputDirectories() {
			return outputDirectories;
		}

		public Scenario getScenarioRM() {
			return project.getProjectOutput().getScenario(directory.getName());
		}
	}

	public VTable createOutputTable() {
		outputTable = new VTable(outputTableModel);
		return outputTable;
	}

	public VTable createScenarioTable() {
		scenarioTable = new VTable(scenarioTableModel);
		scenarioTable.setProjectViewModel(this);
		return scenarioTable;
	}

	/**
	 * Set selection in scenario JTable. Why in this class? It is GUI stuff! Because some Actions
	 * have use this method and Actions only have access to the model. "actions only access the
	 * model" -- that seems pretty idealistic. We already break this concept by using ProjectView's
	 * getMainWindow().
	 */
	public void setSelectedRowIndexInScenarioTable(final int rowIndex) {
		if (scenarioTable.getRowCount() > 0)
			scenarioTable.getSelectionModel().setSelectionInterval(rowIndex, rowIndex);
	}

	/**
	 * Set selection in scenario JTable.
	 */
	public void selectScenario(Scenario scenarioRM) {
		int i = scenarioTableModel.indexOfRow(scenarioRM);
		setSelectedRowIndexInScenarioTable(i);
	}

	public boolean runScenarioIsOk() {
		return runScenarioIsOk(false);
	}

	public boolean runScenarioIsOk(boolean checkAll) {

		List<Scenario> scenariosToCheck = new ArrayList<>();
		if (checkAll) {
			scenariosToCheck = Arrays.asList(getProject().getScenarios().toArray(Scenario[]::new));
		} else {
			scenariosToCheck.add(getCurrentScenario());
		}

		if (scenariosToCheck.size() < 1) {
			throw new IllegalArgumentException("runScenarioIsOk expected at least one scenario");
		}

		for (Scenario srm : scenariosToCheck) {
			String response = srm.readyToRunResponse();
			if (response != null) {
				VDialogManager.showMessageDialogWithBodyAndTextArea("Error",
						Messages.getString("RunScenarioNotReadyToRun.text"),
						response, JOptionPane.ERROR_MESSAGE);
				return false;
			}
		}

		String errorMsg = ScenarioPanel.getActiveJsonParsingErrorMsg();
		if (errorMsg != null) {
			VDialogManager.showMessageDialogWithBodyAndTextArea(
					Messages.getString("RunScenarioJsonErrors.title"),
					Messages.getString("RunScenarioJsonErrors.text"),
					errorMsg, JOptionPane.ERROR_MESSAGE);
			return false;
		}

		JEditorPane errorPanel = null;
		for (Scenario s : scenariosToCheck) {
			// always rerun the ScenarioChecker before running any simulations. This ensures
			// that all scenarios which will be run are checked before any Scenario is stared
			// to allow fixing any errors.
			// This check will be executed even if the ScnearioChecker is deactivated in the GUI.
			ActionScenarioChecker.performManualCheck(s);
			errorPanel = ScenarioPanel.getActiveTopographyErrorMsg();
			if (errorPanel != null) {
				VDialogManager.showMessageDialogWithBodyAndTextEditorPane(
						Messages.getString("RunScenarioTopographyCheckerErrors.title"),
						Messages.getString("RunScenarioTopographyCheckerErrors.text"),
						errorPanel, JOptionPane.ERROR_MESSAGE);
				return false;
			}
		}

		return true;
	}

	public void refreshScenarioNames() {
		if (scenarioTable.getRowCount() > 0) {
			scenarioTable.repaint();
			scenarioNamePanel.setScenarioName(currentScenario.getDisplayName());
		}
	}

	public void setCurrentScenario(Scenario scenario) {
		this.currentScenario = scenario;
	}

	public Scenario getCurrentScenario() {
		return currentScenario;
	}

	public boolean isShowSimulationResultDialog() {
		return showSimulationResultDialog;
	}

	public void setShowSimulationResultDialog(boolean showSimulationResultDialog) {
		this.showSimulationResultDialog = showSimulationResultDialog;
	}

	public void scenarioCheckerStopObserve() {
		scenarioNamePanel.stopObserver();
	}

	public void scenarioCheckerStartObserve(IDrawPanelModel model) {
		scenarioNamePanel.observerIDrawPanelModel(model);
	}

	public void scenarioCheckerCheck(final Scenario scenario) {
		scenarioNamePanel.check(scenario);
	}


	@Override
	public void checkScenario(final Scenario scenario) {
		scenarioNamePanel.check(scenario);
	}
}
