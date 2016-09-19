package org.vadere.simulator.projects;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.simulator.control.Simulation;
import org.vadere.simulator.models.MainModelBuilder;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.Vector2D;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.io.filewatcher.LockFileHandler;

import au.com.bytecode.opencsv.CSVReader;

/**
 * Can be used to start Vadere in [lock] mode.
 * It will search for a lock file and starts running after it is removed.
 * 
 *
 */
public class ScenarioRunLocked extends ScenarioRunManager {

	private static Logger logger = LogManager.getLogger(ScenarioRunLocked.class);

	private LockFileHandler lockFileHandler = null;

	private String timeStepFile;

	private String lockDirectory;

	public ScenarioRunLocked(String name, ScenarioStore store) {
		super(name, store);
		this.lockDirectory = null;
	}

	@Override
	public void run() {
		doBeforeSimulation();

		handleLock(Paths.get(this.lockDirectory));
	}

	/**
	 * Checks the given directory on lock files.
	 * If a lock file is present, the simulation pauses after reaching the specified finish time.
	 * When the lock gets released, the simulation is resumed with a new finish time.
	 * 
	 * @param lockDirectory
	 */
	private void handleLock(Path lockDirectory) {
		logger.info("Lock file monitoring started.");

		File folder = new File(lockDirectory.toString());

		if (!folder.exists()) {
			// Test to see if monitored folder exists
			throw new RuntimeException("Directory not found: " + lockDirectory);
		}

		final ScenarioRunManager thisVadere = this;

		while (true) {
			// TODO [priority=medium] [task=refactoring] improve error handling!
			try {
				lockFileHandler.waitForLockDelete();
				Thread.sleep(100);
			} catch (IOException e) {
				logger.error(e);
				System.exit(0);
			} catch (InterruptedException e) {
				logger.error(e);
			}

			try {
				// "file" is the reference to the removed file
				logger.info("Lock file removed.");
				logger.info(String.format("Resuming '%s'...", thisVadere.getName()));

				double currentTime = 0;
				if (simulation == null) {
					MainModelBuilder modelBuilder = new MainModelBuilder(scenarioStore);
					modelBuilder.createModelAndRandom();
					simulation = new Simulation(modelBuilder.getModel(), currentTime, getName(),
							scenarioStore, passiveCallbacks, modelBuilder.getRandom(), thisVadere.processorManager);
				} else {
					currentTime = simulation.getCurrentTime();
					// restart timer of the simulation
					simulation.setStartTimeInSec(currentTime);
				}
				logger.info("Current time: " + currentTime);

				try {
					if (!Files.exists(Paths.get(timeStepFile))) {
						Files.createFile(Paths.get(timeStepFile));
					}
					scenarioStore.topography = prepareTopographyWithTSF(scenarioStore.topography);
				} catch (IOException e) {
					logger.error(e);
				}
				try {
					prepareLockOutput(simulation);
				} catch (FileNotFoundException e) {
					logger.error(e);
				}

				simulation.run();
				scenarioStore.topography.getInitialElements(Pedestrian.class).clear(); // so that they are created only once
				doAfterSimulation();
				logger.info(String.format("Finished '%s'.", thisVadere.getName()));

			} catch (ClassNotFoundException | InstantiationException
					| IllegalAccessException e) {
				e.printStackTrace();
			}

			try {
				lockFileHandler.writeLock();
			} catch (IOException e) {
				logger.error(e);
				System.exit(0);
			}
		}
	}

	public void setWaitOnLockData(String lockDirectory, String timeStepFile, Boolean outputAll) {
		this.lockDirectory = lockDirectory;
		this.timeStepFile = timeStepFile;

		try {
			this.lockFileHandler = new LockFileHandler(lockDirectory);
		} catch (Exception e) {
			logger.error(e);
		}
	}

	private Topography prepareTopographyWithTSF(Topography topography) throws IOException {
		final char separator = ';';// TODO [task=feature] [priority=low] add config for this separator

		// read TSF
		CSVReader reader = new CSVReader(new FileReader(timeStepFile), separator);
		List<String[]> myEntries = reader.readAll();

		Collection<Pedestrian> pedestrians = topography.getElements(Pedestrian.class);
		// form a map to easily search for a pedestrian
		Map<Integer, Pedestrian> pedMap = new HashMap<>();
		for (Pedestrian pedestrian : pedestrians) {
			pedMap.put(pedestrian.getId(), pedestrian);
		}
		// this will contain all pedestrians that should be removed (i.e. present in the topography
		// but not in the TSF)
		Map<Integer, Pedestrian> pedRemoveMap = new HashMap<>(pedMap);

		// create the pedestrians stated in the TSF file. If they already exist, just move them and
		// set their next target.
		int id = 0, nextTarget = 0;
		double x = 0, y = 0, dx = 0, dy = 0, desiredSpeed = 0;

		for (String[] strings : myEntries) {
			if (strings.length == 5) {
				id = Integer.parseInt(strings[0]);
				x = Double.parseDouble(strings[1]);
				y = Double.parseDouble(strings[2]);
				nextTarget = Integer.parseInt(strings[3]);
				// int nextSource = Integer.parseInt(strings[4]); // TODO [priority=low] [task=feature] set source id
			} else if (strings.length == 8) {
				id = Integer.parseInt(strings[0]);
				x = Double.parseDouble(strings[1]);
				y = Double.parseDouble(strings[2]);
				dx = Double.parseDouble(strings[3]);
				dy = Double.parseDouble(strings[4]);
				nextTarget = Integer.parseInt(strings[5]);
				// int nextSource = Integer.parseInt(strings[6]); // TODO [priority=low] [task=feature] set source id
				desiredSpeed = Double.parseDouble(strings[7]);
			} else {
				reader.close();
				StringBuilder line = new StringBuilder();
				for (String s : strings) {
					line.append(s);
					line.append(separator);
				}

				throw new IllegalArgumentException(
						String.format("The line '%s' does not have the correct format. Correct value separator: '%s'",
								line.toString(), separator));
			}

			if (pedMap.containsKey(id)) {
				pedMap.get(id).setPosition(new VPoint(x, y));

				LinkedList<Integer> targetIds = new LinkedList<>();
				targetIds.add(nextTarget);
				pedMap.get(id).setTargets(targetIds);

				// if we have a velocity given, set it
				if (strings.length == 8 && !Double.isNaN(dx) && !Double.isNaN(dy)) {
					pedMap.get(id).setVelocity(new Vector2D(dx, dy));
					pedMap.get(id).setFreeFlowSpeed(desiredSpeed);
				}

				pedRemoveMap.remove(id);
			} else {
				// TODO [priority=low] [task=feature] set sourceId
				Pedestrian p = new Pedestrian(new AttributesAgent(id), new Random());
				p.setPosition(new VPoint(x, y));
				p.setVelocity(new Vector2D(dx, dy));
				LinkedList<Integer> targets = new LinkedList<>();
				targets.add(nextTarget);
				p.setTargets(targets);
				p.setFreeFlowSpeed(desiredSpeed);

				topography.addInitialElement(p);
			}
		}

		for (Pedestrian pedestrian : pedRemoveMap.values()) {
			topography.removeElement(pedestrian);
		}

		reader.close();

		return topography;
	}

	/**
	 * This adds the writers of the output and time-step-file to the simulation.
	 * Needed when in [lock] mode.
	 * 
	 * @param simulation
	 * @throws FileNotFoundException
	 */
	private void prepareLockOutput(Simulation simulation) throws FileNotFoundException {
	}

}
