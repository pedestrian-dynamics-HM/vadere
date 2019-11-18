package org.vadere.simulator.projects;

import org.vadere.simulator.control.simulation.PassiveCallback;
import org.vadere.simulator.control.simulation.ScenarioRun;
import org.vadere.simulator.projects.migration.MigrationResult;
import org.vadere.simulator.utils.cache.ScenarioCache;
import org.vadere.util.io.IOUtils;
import org.vadere.util.logging.Logger;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

/**
 * A VadereProject holds a list of {@link Scenario}s and functionality to manage them.
 */
public class VadereProject {

	private static Logger logger = Logger.getLogger(VadereProject.class);

	private String name;
	private Thread currentScenarioThread;
	private ScenarioRun currentScenarioRun;
	private PassiveCallback visualization;
	private LinkedList<SimulationResult> simulationResults = new LinkedList<>();
	private final ConcurrentMap<String, Scenario> scenarios = new ConcurrentHashMap<>();
	private final BlockingQueue<ProjectFinishedListener> projectFinishedListener = new LinkedBlockingQueue<>();
	private final BlockingQueue<SingleScenarioFinishedListener> singleScenarioFinishedListener =
			new LinkedBlockingQueue<>();
	private LinkedBlockingDeque<Scenario> scenariosLeft;
	private Path outputDirectory;
	private Path projectDirectory;
	private ProjectOutput projectOutput;

	private MigrationResult migrationStats;

	public VadereProject(final String name, final Iterable<Scenario> scenarios, Path vadereProjectDirectory) {
		this.name = name;
		scenarios.forEach(this::addScenario);
		this.projectDirectory = vadereProjectDirectory;
		this.outputDirectory = vadereProjectDirectory.resolve(IOUtils.OUTPUT_DIR);
		this.projectOutput = new ProjectOutput(this);
	}

	public void saveChanges() {
		scenarios.forEach((Id, scenarioRunManager) -> {
			scenarioRunManager.saveChanges();
		});
	}

	public boolean hasUnsavedChanges() {
		for (Scenario srm : getScenarios()) {
			if (srm.hasUnsavedChanges())
				return true;
		}
		return false;
	}

	public String getDiffs() {
		String eol = "\n---------------\n";
		StringBuilder collectDiffs = new StringBuilder();
		for (Scenario srm : getScenarios()) {
			String diff = srm.getDiff();
			if (diff != null)
				collectDiffs.append("scenario <").append(srm.getName()).append("> :\n").append(diff).append(eol);
		}
		return collectDiffs.toString();
	}

	/**
	 * Runs the given scenarios, each in a separate thread.
	 */
	public void runScenarios(final Collection<Scenario> scenariosToRun) {
		// TODO [priority=normal] [task=bugfix] this is a bug: scenariosLeft may be overwritten even if there are still scenarios in it
		scenariosLeft = new LinkedBlockingDeque<>();
		scenariosLeft.addAll(scenariosToRun);

		if (!scenariosLeft.isEmpty()) {
			notifyProjectListenerAboutPreRun();
			prepareAndStartScenarioRunThread();
		}
	}

	private void prepareAndStartScenarioRunThread() {
		currentScenarioRun = prepareNextScenario();
		currentScenarioThread = new Thread(currentScenarioRun);

		currentScenarioThread.setUncaughtExceptionHandler((t, ex) -> {
			currentScenarioRun.simulationFailed(ex);
			notifySimulationListenersSimulationError(currentScenarioRun.getScenario(), ex);
		});

		notifySimulationListenersSimulationStarted(getCurrentScenario());
		currentScenarioThread.start();
	}

	public void runScenario(final Scenario scenario) {
		runScenarios(Collections.singleton(scenario));
	}

	private void notifyProjectListenerAboutPreRun() {
		for (ProjectFinishedListener l : projectFinishedListener) {
			l.preProjectRun(this);
		}
	}

	private void notifyScenarioRMListenerAboutPostRun(final Scenario scenario) {
		for (SingleScenarioFinishedListener l : singleScenarioFinishedListener) {
			l.postScenarioRun(scenario, scenariosLeft.size());
		}
	}

	private void notifySimulationListenersSimulationError(final Scenario scenario, final Throwable ex) {
		for (SingleScenarioFinishedListener l : singleScenarioFinishedListener) {
			l.error(currentScenarioRun.getScenario(), scenariosLeft.size(), ex);
		}
	}

	private void notifySimulationListenersSimulationStarted(final Scenario scenario) {
		for (SingleScenarioFinishedListener l : singleScenarioFinishedListener) {
			l.scenarioStarted(currentScenarioRun.getScenario(), scenariosLeft.size() + 1);
		}
	}

	private ScenarioRun prepareNextScenario() {
		final Scenario nextScenario = scenariosLeft.remove();

		notifySingleScenarioFinishListener(nextScenario);

		final ScenarioRun scenarioRun = new ScenarioRun(nextScenario, scenarioFinishedListener, projectDirectory.resolve(IOUtils.SCENARIO_DIR).resolve(nextScenario.getName() + IOUtils.SCENARIO_FILE_EXTENSION), loadCache(nextScenario));
		scenarioRun.setOutputPaths(outputDirectory);
		if (visualization != null) {
			scenarioRun.addPassiveCallback(visualization);
		}
		return scenarioRun;
	}

	private ScenarioCache loadCache(final Scenario scenario){
		return ScenarioCache.load(scenario, projectDirectory.resolve(IOUtils.SCENARIO_DIR));
	}

	private void notifySingleScenarioFinishListener(final Scenario scenario) {
		for (SingleScenarioFinishedListener listener : singleScenarioFinishedListener) {
			listener.preScenarioRun(scenario, scenariosLeft.size() + 1);
		}
	}

	public void runAllScenarios() {
		runScenarios(getScenarios());
	}

	public void pauseRunnningScenario() {
		currentScenarioRun.pause();
		for (SingleScenarioFinishedListener listener : singleScenarioFinishedListener) {
			listener.scenarioPaused(currentScenarioRun.getScenario(), scenariosLeft.size() + 1);
		}
	}

	public boolean isScenarioPaused() {
		return !currentScenarioRun.isRunning();
	}

	public boolean isScenarioInSingleStepMode(){
		return currentScenarioRun.isScenarioInSingleStepMode();
	}

	public void setSingleStepMode(boolean singleStepMode){
		currentScenarioRun.setSingleStepMode(singleStepMode);
	}

	public boolean isWaitForSimCommand(){
		return currentScenarioRun.isWaitForSimCommand();
	}

	public void nextSimCommand(double simulateUntilInSec){
		currentScenarioRun.nextSimCommand(simulateUntilInSec);
	}

	public void resumePausedScenarios() {
		currentScenarioRun.resume();
	}

	public void interruptRunningScenarios() {
		currentScenarioThread.interrupt();
		scenariosLeft.clear();

		// after interruption the simulation may run further for a short moment,
		// if the user start again a scenario before the simulation terminates
		// modification exception and other unexpected errors may cause!!!
		// So here we wait for the simulation thread to finish.
		try {
			currentScenarioThread.join();
		} catch (InterruptedException e) {
			logger.error(e);
			e.printStackTrace();
		}

		for (SingleScenarioFinishedListener listener : singleScenarioFinishedListener) {
			listener.scenarioInterrupted(currentScenarioRun.getScenario(), scenariosLeft.size());
		}
	}

	// Adder...

	public void addProjectFinishedListener(ProjectFinishedListener listener) {
		projectFinishedListener.add(listener);
	}

	public void addSingleScenarioFinishedListener(SingleScenarioFinishedListener listener) {
		singleScenarioFinishedListener.add(listener);
	}

	public void setVisualization(PassiveCallback passiveCallback) {
		visualization = passiveCallback;
	}

	// Setter...

	public void setName(String projectName) {
		this.name = projectName;
	}

	public void setOutputDir(Path outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	// Getter...


	public LinkedList<SimulationResult> getSimulationResults() {
		return simulationResults;
	}

	public BlockingQueue<Scenario> getScenarios() {
		return scenarios.values().stream().sorted((f1, f2) -> f1.getName().compareTo(f2.getName())).collect(Collectors.toCollection(LinkedBlockingQueue::new));
	}

	public int getScenarioIndexByName(final Scenario srm) {
		int index = -1;
		int currentIndex = 0;
		for (Scenario csrm : getScenarios()) {
			if (csrm.getName().equals(srm.getName())) {
				return currentIndex;
			} else {
				currentIndex++;
			}
		}
		return index;
	}

	public String getName() {
		return name;
	}

	public Scenario getScenario(int index) {
		return getScenarios().toArray(new Scenario[]{})[index];
	}

	public void removeScenario(final Scenario scenario) {
		scenarios.remove(scenario.getName());
	}

	public Path getOutputDir() {
		return outputDirectory;
	}

	public void addScenario(final Scenario scenario) {
		this.scenarios.put(scenario.getName(), scenario);
	}

	public Scenario getCurrentScenario() {
		return currentScenarioRun.getScenario();
	}

	public void setMigrationStats(MigrationResult migrationStats) {
		this.migrationStats = migrationStats;
	}

	public MigrationResult getMigrationStats() {
		return migrationStats;
	}

	/**
	 * Starts the next simulation if any.
	 */
	private RunnableFinishedListener scenarioFinishedListener = new RunnableFinishedListener() {
		@Override
		public void finished(Runnable runnable) {
			notifyScenarioRMListenerAboutPostRun(getCurrentScenario());

			projectOutput.update();
			simulationResults.add(currentScenarioRun.getSimulationResult());

			if (scenariosLeft.isEmpty()) {
				simulationResults.stream().forEach(res -> logger.info(res.toString()));
				for (ProjectFinishedListener listener : projectFinishedListener) {
					listener.postProjectRun(VadereProject.this);
				}
				resetState();
			} else {
				prepareAndStartScenarioRunThread();
			}
		}
	};

	public ProjectOutput getProjectOutput() {
		return projectOutput;
	}

	public void setProjectOutput(ProjectOutput projectOutput) {
		this.projectOutput = projectOutput;
	}

	public void resetState() {
		simulationResults = new LinkedList<>();
	}

	public Path getProjectDirectory() {
		return projectDirectory;
	}

	public void setProjectDirectory(Path vadereProjectDirectory) {
		this.projectDirectory = vadereProjectDirectory;
	}
}
