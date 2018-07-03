package org.vadere.simulator.control;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.simulator.control.factory.SourceControllerFactory;
import org.vadere.simulator.models.DynamicElementFactory;
import org.vadere.simulator.models.MainModel;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.models.potential.PotentialFieldModel;
import org.vadere.simulator.models.potential.fields.IPotentialFieldTarget;
import org.vadere.simulator.projects.ScenarioStore;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.state.attributes.AttributesSimulation;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Source;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class Simulation {

	private static Logger logger = LogManager.getLogger(Simulation.class);

	private final AttributesSimulation attributesSimulation;
	private final AttributesAgent attributesAgent;

	private final Collection<SourceController> sourceControllers;
	private final Collection<TargetController> targetControllers;
	private TeleporterController teleporterController;
	private TopographyController topographyController;
	private DynamicElementFactory dynamicElementFactory;

	private final List<PassiveCallback> passiveCallbacks;
	private List<Model> models;

	private boolean runSimulation = false;
	private boolean paused = false;
	/**
	 * current simulation time (seconds)
	 */
	private double simTimeInSec = 0;
	/**
	 * time (seconds) where the simulation starts
	 */
	private double startTimeInSec = 0;
	/**
	 * time (seconds) that should be simulated, i.e. the final time is startTimeInSec + runTimeInSec
	 */
	private double runTimeInSec = 0;
	private long lastFrameInMs = 0;
	private int step = 0;
	private SimulationState simulationState;

	private String name;
	private final ScenarioStore scenarioStore;
	private final MainModel mainModel;
	/** Hold the topography in an extra field for convenience. */
	private final Topography topography;
	private final ProcessorManager processorManager;
	private final SourceControllerFactory sourceControllerFactory;

	public Simulation(MainModel mainModel, double startTimeInSec, final String name, ScenarioStore scenarioStore,
			List<PassiveCallback> passiveCallbacks, Random random, ProcessorManager processorManager) {
		this.name = name;
		this.mainModel = mainModel;
		this.scenarioStore = scenarioStore;
		this.attributesSimulation = scenarioStore.attributesSimulation;
		this.attributesAgent = scenarioStore.getTopography().getAttributesPedestrian();
		this.sourceControllers = new LinkedList<>();
		this.targetControllers = new LinkedList<>();
		this.topography = scenarioStore.getTopography();
		this.runTimeInSec = attributesSimulation.getFinishTime();
		this.startTimeInSec = startTimeInSec;
		this.simTimeInSec = startTimeInSec;

		this.models = mainModel.getSubmodels();
		this.sourceControllerFactory = mainModel.getSourceControllerFactory();

		// TODO [priority=normal] [task=bugfix] - the attributesCar are missing in initialize' parameters
		this.dynamicElementFactory = mainModel;

		this.processorManager = processorManager;
		this.passiveCallbacks = passiveCallbacks;
		this.topographyController = new TopographyController(topography, dynamicElementFactory);

        IPotentialFieldTarget pft = null;
        if(mainModel instanceof PotentialFieldModel) {
            pft = ((PotentialFieldModel) mainModel).getPotentialFieldTarget();
        }

		for (PassiveCallback pc : this.passiveCallbacks) {
			pc.setTopography(topography);
            if(pft != null) {
                pc.setPotentialFieldTarget(pft);
            }
		}

		// create source and target controllers
		for (Source source : topography.getSources()) {
			SourceController sc = this.sourceControllerFactory
					.create(topography, source, dynamicElementFactory, attributesAgent, random);
			sourceControllers.add(sc);
		}
		for (Target target : topography.getTargets()) {
			targetControllers.add(new TargetController(topography, target));
		}

		if (topography.hasTeleporter()) {
			this.teleporterController = new TeleporterController(
					topography.getTeleporter(), topography);
		}
	}

	private void preLoop() {
		if (topographyController == null) {
			logger.error("No topography loaded.");
			return;
		}

		simulationState = initialSimulationState();
		topographyController.preLoop(simTimeInSec);
		runSimulation = true;
		simTimeInSec = startTimeInSec;

		for (Model m : models) {
			m.preLoop(simTimeInSec);
		}

		for (PassiveCallback c : passiveCallbacks) {
			c.preLoop(simTimeInSec);
		}

		if (attributesSimulation.isWriteSimulationData()) {
			processorManager.preLoop(this.simulationState);
		}
	}

	private void postLoop() {
		simulationState = new SimulationState(name, topography, scenarioStore, simTimeInSec, step, mainModel);

		for (Model m : models) {
			m.postLoop(simTimeInSec);
		}

		for (PassiveCallback c : passiveCallbacks) {
			c.postLoop(simTimeInSec);
		}

		if (attributesSimulation.isWriteSimulationData()) {
			processorManager.postLoop(this.simulationState);
		}
		topographyController.postLoop(this.simTimeInSec);
	}

	/**
	 * Starts simulation and runs main loop until stopSimulation flag is set.
	 */
	public void run() {
		try {
			if (attributesSimulation.isWriteSimulationData()) {
				processorManager.setMainModel(mainModel);
				processorManager.initOutputFiles();
			}

			preLoop();

			while (runSimulation) {
				synchronized (this) {
					while (paused) {
						try {
							wait();
						} catch (Exception e) {
							paused = false;
							Thread.currentThread().interrupt();
							logger.warn("interrupt while paused.");
						}
					}
				}

				if (attributesSimulation.isVisualizationEnabled()) {
					sleepTillStartOfNextFrame();
				}

				for (PassiveCallback c : passiveCallbacks) {
					c.preUpdate(simTimeInSec);
				}

				assert assertAllPedestrianInBounds();
				updateCallbacks(simTimeInSec);
				updateWriters(simTimeInSec);

				if (attributesSimulation.isWriteSimulationData()) {
					processorManager.update(this.simulationState);
				}


				for (PassiveCallback c : passiveCallbacks) {
					c.postUpdate(simTimeInSec);
				}

				if (runTimeInSec + startTimeInSec > simTimeInSec + 1e-7) {
					simTimeInSec += Math.min(attributesSimulation.getSimTimeStepLength(), runTimeInSec + startTimeInSec - simTimeInSec);
				} else {
					runSimulation = false;
				}


				//remove comment to fasten simulation for evacuation simulations
				//if (topography.getElements(Pedestrian.class).size() == 0){
				//	runSimulation = false;
				//}

				if (Thread.interrupted()) {
					runSimulation = false;
					logger.info("Simulation interrupted.");
				}
			}
		} finally {
			// this is necessary to free the resources (files), the SimulationWriter and processor are writing in!
			postLoop();

			if (attributesSimulation.isWriteSimulationData()) {
				processorManager.writeOutput();
			}
			logger.info("Finished writing all output files");
		}
	}

	private boolean assertAllPedestrianInBounds() {
		Rectangle2D.Double bounds = topography.getBounds();
		Collection<Pedestrian> peds = topography.getElements(Pedestrian.class);
		return peds.stream().map(ped -> ped.getPosition()).allMatch(pos -> bounds.contains(pos.getX(), pos.getY()));
	}

	private SimulationState initialSimulationState() {
		SimulationState state =
				new SimulationState(name, topography.clone(), scenarioStore, simTimeInSec, step, mainModel);

		return state;
	}

	private void updateWriters(double simTimeInSec) {

		SimulationState simulationState =
				new SimulationState(name, topography, scenarioStore, simTimeInSec, step, mainModel);

		this.simulationState = simulationState;
	}

	private void updateCallbacks(double simTimeInSec) {

		this.targetControllers.clear();
		for (Target target : this.topographyController.getTopography().getTargets()) {
			targetControllers.add(new TargetController(this.topographyController.getTopography(), target));
		}

		for (SourceController sourceController : this.sourceControllers) {
			sourceController.update(simTimeInSec);
		}

		for (TargetController targetController : this.targetControllers) {
			targetController.update(simTimeInSec);
		}

		topographyController.update(simTimeInSec);
		step++;

		for (Model m : models) {
			m.update(simTimeInSec);
		}

		if (topographyController.getTopography().hasTeleporter()) {
			teleporterController.update(simTimeInSec);
		}
	}

	public synchronized void pause() {
		paused = true;
	}

	public synchronized boolean isPaused() {
		return paused;
	}

	public synchronized boolean isRunning() {
		return runSimulation && !isPaused();
	}

	public synchronized void resume() {
		paused = false;
		notify();
	}

	/**
	 * If visualization is enabled, wait until time elapsed since last frame
	 * matches a given time span. This ensures that the speed of the simulation
	 * is not mainly determined by hardware performance.
	 */
	private void sleepTillStartOfNextFrame() {

		// Preferred time span between two frames.
		long desireDeltaTimeInMs = (long) (attributesSimulation
				.getSimTimeStepLength()
				* attributesSimulation.getRealTimeSimTimeRatio() * 1000.0);
		// Remaining time until next simulation step has to be started.
		long waitTime = desireDeltaTimeInMs
				- (System.currentTimeMillis() - lastFrameInMs);

		lastFrameInMs = System.currentTimeMillis();

		if (waitTime > 0) {
			try {
				Thread.sleep(waitTime);
			} catch (InterruptedException e) {
				runSimulation = false;
				logger.info("Simulation interrupted.");
			}
		}
	}

	public double getCurrentTime() {
		return this.simTimeInSec;
	}

	public void setStartTimeInSec(double startTimeInSec) {
		this.startTimeInSec = startTimeInSec;
	}
}
