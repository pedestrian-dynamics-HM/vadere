package org.vadere.simulator.projects;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.simulator.control.PassiveCallback;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

/**
 * A VadereProject holds a list of {@link Scenario}s and functionality to manage them.
 * 
 */
public class VadereProject implements ScenarioFinishedListener {

	private static Logger logger = LogManager.getLogger(VadereProject.class);

	private String name;
	private Thread currentScenarioThread;
	private ScenarioRun currentScenarioRun;
	private PassiveCallback visualization;
	private final ConcurrentMap<String, Scenario> scenarios = new ConcurrentHashMap<>();
	private final BlockingQueue<ProjectFinishedListener> projectFinishedListener = new LinkedBlockingQueue<>();
	private final BlockingQueue<SingleScenarioFinishedListener> singleScenarioFinishedListener =
			new LinkedBlockingQueue<>();
	private LinkedBlockingDeque<Scenario> scenariosLeft;
	private Path outputDirectory;

	// TODO should be encapsulated in a class (we are not programming in C):
	private int[] migrationStats; // scenarios: [0] total, [1] legacy'ed, [2] nonmigratable

	public VadereProject(final String name, final Iterable<Scenario> scenarios) {
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
		for (Scenario srm : getScenarios()) {
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
		for (Scenario srm : getScenarios()) {
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
			singleScenarioFinishedListener.forEach(l -> l.error(currentScenarioRun.getScenario(), scenariosLeft.size(), ex));
		});
		currentScenarioThread.start();
	}

	public void runScenario(final Scenario scenario) {
		runScenarios(Collections.singleton(scenario));
	}

	/**
	 * Calls the next scenario if available.
	 */
	@Override
	public void scenarioFinished(final Scenario scenario) {
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

	private void notifyScenarioRMListenerAboutPostRun(final Scenario scenario) {
		for (SingleScenarioFinishedListener l : singleScenarioFinishedListener) {
			l.postScenarioRun(scenario, scenariosLeft.size());
		}
	}

	@Override
	public void scenarioRunThrewException(final Scenario scenario, final Throwable ex) {
		for (SingleScenarioFinishedListener l : singleScenarioFinishedListener) {
			l.error(currentScenarioRun.getScenario(), scenariosLeft.size(), ex);
		}
	}

	@Override
	public void scenarioStarted(final Scenario scenario) {
		for (SingleScenarioFinishedListener l : singleScenarioFinishedListener) {
			l.scenarioStarted(currentScenarioRun.getScenario(), scenariosLeft.size() + 1);
		}
	}

	private ScenarioRun prepareNextScenario() {
		final Scenario nextScenario = scenariosLeft.remove().clone();
		nextScenario.setScenarioFinishedListener(this);

		notifySingleScenarioFinishListener(nextScenario);

		final ScenarioRun scenarioRun = new ScenarioRun(nextScenario);
		scenarioRun.setOutputPaths(outputDirectory);
		if (visualization != null) {
			scenarioRun.addPassiveCallback(visualization);
		}
		return scenarioRun;
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
		if (currentScenarioRun.pause()) {
			for (SingleScenarioFinishedListener listener : singleScenarioFinishedListener) {
				listener.scenarioPaused(currentScenarioRun.getScenario(), scenariosLeft.size() + 1);
			}
		}
	}

	public boolean isScenarioPaused() {
		return !currentScenarioRun.isRunning();
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

	public BlockingQueue<Scenario> getScenarios() {
		return scenarios.values().stream().sorted((f1, f2) -> f1.getName().compareTo(f2.getName())).collect(Collectors.toCollection(LinkedBlockingQueue::new));
	}

	public int getScenarioIndexByName(final Scenario srm) {
		int index = -1;
		int currentIndex = 0;
		for(Scenario csrm : getScenarios()) {
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

	public Scenario getScenario(int index) {
		return getScenarios().toArray(new Scenario[] {})[index];
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

	public void setMigrationStats(int[] migrationStats) {
		this.migrationStats = migrationStats;
	}

	public int[] getMigrationStats() {
		return migrationStats;
	}
}
