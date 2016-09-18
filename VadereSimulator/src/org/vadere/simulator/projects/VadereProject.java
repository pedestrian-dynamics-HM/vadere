package org.vadere.simulator.projects;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.simulator.control.PassiveCallback;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

/**
 * A VadereProject holds a list of {@link ScenarioRunManager}s and functionality to manage them.
 * 
 */
public class VadereProject implements ScenarioFinishedListener {

	private static Logger logger = LogManager.getLogger(VadereProject.class);

	private String name;
	private Thread currentScenarioThread;
	private ScenarioRunManager currentScenario;
	private final List<PassiveCallback> visualization = new LinkedList<>();
	private final ConcurrentMap<String, ScenarioRunManager> scenarios = new ConcurrentHashMap<>();
	private final BlockingQueue<ProjectFinishedListener> projectFinishedListener = new LinkedBlockingQueue<>();
	private final BlockingQueue<SingleScenarioFinishedListener> singleScenarioFinishedListener =
			new LinkedBlockingQueue<>();
	private LinkedBlockingDeque<ScenarioRunManager> scenariosLeft;
	private Path outputDirectory;
	private int[] migrationStats; // scenarios: [0] total, [1] legacy'ed, [2] nonmigratable

	public VadereProject(final String name, final Iterable<ScenarioRunManager> scenarios) {
		this.name = name;
		scenarios.forEach(scenario -> addScenario(scenario));
		this.outputDirectory = Paths.get("output");
	}

	public void saveChanges() {
		scenarios.forEach((Id, scenarioRunManager) -> {
			scenarioRunManager.saveChanges();
		});
	}

	public boolean hasUnsavedChanges() {
		Set<String> currentScenarioIds = new HashSet<>();
		for (ScenarioRunManager srm : getScenarios()) {
			currentScenarioIds.add(srm.getName());
			if (srm.hasUnsavedChanges())
				return true;
		}
		return false;
	}

	public String getDiffs() {
		String eol = "\n---------------\n";
		Set<String> currentScenarioIds = new HashSet<>();
		StringBuilder collectDiffs = new StringBuilder();
		for (ScenarioRunManager srm : getScenarios()) {
			currentScenarioIds.add(srm.getName());
			String diff = srm.getDiff();
			if (diff != null)
				collectDiffs.append("scenario <" + srm.getName() + "> :\n" + diff + eol);
		}
		return collectDiffs.toString();
	}

	/**
	 * Runs the given scenarios, each in a separate thread.
	 */
	public void runScenarios(final Collection<ScenarioRunManager> scenariosRMsToRun) {
		for (ScenarioRunManager scenarioRM : scenariosRMsToRun) {
			scenarioRM.setOutputPaths(outputDirectory);
		}

		// TODO [priority=normal] [task=bugfix] this is a bug: scenariosLeft may be overwritten even if there are still scenarios in it
		scenariosLeft = new LinkedBlockingDeque<>();
		scenariosLeft.addAll(scenariosRMsToRun);

		if (!scenariosLeft.isEmpty()) {
			notifyProjectListenerAboutPreRun();
			prepareAndStartScenarioRunThread();
		}
	}

	private void prepareAndStartScenarioRunThread() {
		currentScenario = prepareNextScenario();
		currentScenarioThread = new Thread(currentScenario);

		currentScenarioThread.setUncaughtExceptionHandler(
				(t, ex) -> {
					currentScenario.setScenarioFailed(true);
					singleScenarioFinishedListener
							.forEach(l -> l.error(currentScenario, scenariosLeft.size(), ex));
				});
		currentScenarioThread.start();
	}

	public void runScenario(final ScenarioRunManager scenario) {
		runScenarios(Collections.singleton(scenario));
	}

	/**
	 * Calls the next scenario if available.
	 */
	@Override
	public void scenarioFinished(final ScenarioRunManager scenario) {
		notifyScenarioRMListenerAboutPostRun(scenario);

		if (scenariosLeft.isEmpty()) {
			for (ProjectFinishedListener listener : projectFinishedListener) {
				listener.postProjectRun(this);
			}
		} else {
			prepareAndStartScenarioRunThread();
		}
	}

	private void notifyProjectListenerAboutPreRun() {
		for (ProjectFinishedListener l : projectFinishedListener) {
			l.preProjectRun(this);
		}
	}

	private void notifyScenarioRMListenerAboutPostRun(final ScenarioRunManager scenario) {
		for (SingleScenarioFinishedListener l : singleScenarioFinishedListener) {
			l.postScenarioRun(scenario, scenariosLeft.size());
		}
	}

	@Override
	public void scenarioRunThrewException(final ScenarioRunManager scenario, final Throwable ex) {
		for (SingleScenarioFinishedListener l : singleScenarioFinishedListener) {
			l.error(currentScenario, scenariosLeft.size(), ex);
		}
	}

	@Override
	public void scenarioStarted(final ScenarioRunManager scenario) {
		for (SingleScenarioFinishedListener l : singleScenarioFinishedListener) {
			l.scenarioStarted(currentScenario, scenariosLeft.size() + 1);
		}
	}

	private ScenarioRunManager prepareNextScenario() {
		ScenarioRunManager nextScenario = scenariosLeft.remove().clone();
		nextScenario.setScenarioFinishedListener(this);

		for (SingleScenarioFinishedListener listener : singleScenarioFinishedListener) {
			listener.preScenarioRun(nextScenario, scenariosLeft.size() + 1);
		}

		if (!this.visualization.isEmpty()) {
			nextScenario.addPassiveCallback(this.visualization.get(0));
		}
		return nextScenario;
	}

	public void runAllScenarios() {
		runScenarios(getScenarios());
	}

	public void pauseRunnningScenario() {
		if (currentScenario.pause()) {
			for (SingleScenarioFinishedListener listener : singleScenarioFinishedListener) {
				listener.scenarioPaused(currentScenario, scenariosLeft.size() + 1);
			}
		}
	}

	public boolean isScenarioPaused() {
		return !currentScenario.isRunning();
	}

	public void resumePausedScenarios() {
		currentScenario.resume();
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
			listener.scenarioInterrupted(currentScenario, scenariosLeft.size());
		}
	}

	// Adder...

	public void addProjectFinishedListener(ProjectFinishedListener listener) {
		projectFinishedListener.add(listener);
	}

	public void addSingleScenarioFinishedListener(SingleScenarioFinishedListener listener) {
		singleScenarioFinishedListener.add(listener);
	}

	public void addVisualization(PassiveCallback pc) {
		visualization.clear();
		visualization.add(pc);
	}

	// Setter...

	public void setName(String projectName) {
		this.name = projectName;
	}

	public void setOutputDir(Path outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	// Getter...

	public BlockingQueue<ScenarioRunManager> getScenarios() {
		return scenarios.values().stream().sorted((f1, f2) -> f1.getName().compareTo(f2.getName())).collect(Collectors.toCollection(LinkedBlockingQueue::new));
	}

	public int getScenarioIndexByName(final ScenarioRunManager srm) {
		int index = -1;
		int currentIndex = 0;
		for(ScenarioRunManager csrm : getScenarios()) {
			if(csrm.getName().equals(srm.getName())) {
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

	public ScenarioRunManager getScenario(int index) {
		return getScenarios().toArray(new ScenarioRunManager[] {})[index];
	}

	public void removeScenario(final ScenarioRunManager scenario) {
		scenarios.remove(scenario.getName());
	}

	public Path getOutputDir() {
		return outputDirectory;
	}

	public void addScenario(final ScenarioRunManager scenario) {
		this.scenarios.put(scenario.getName(), scenario);
	}

	public ScenarioRunManager getCurrentScenario() {
		return currentScenario;
	}

	public void setMigrationStats(int[] migrationStats) {
		this.migrationStats = migrationStats;
	}

	public int[] getMigrationStats() {
		return migrationStats;
	}
}
