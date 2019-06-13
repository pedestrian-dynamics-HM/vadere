package org.vadere.simulator.control;

import org.vadere.simulator.control.cognition.EventCognition;
import org.vadere.simulator.control.cognition.SalientBehaviorCognition;
import org.vadere.simulator.control.events.EventController;
import org.vadere.simulator.control.factory.SourceControllerFactory;
import org.vadere.simulator.models.DynamicElementFactory;
import org.vadere.simulator.models.MainModel;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.models.bhm.BehaviouralHeuristicsModel;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.simulator.models.potential.PotentialFieldModel;
import org.vadere.simulator.models.potential.fields.IPotentialField;
import org.vadere.simulator.models.potential.fields.IPotentialFieldTarget;
import org.vadere.simulator.projects.ScenarioStore;
import org.vadere.simulator.projects.SimulationResult;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.state.attributes.AttributesSimulation;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.events.types.Event;
import org.vadere.state.scenario.AbsorbingArea;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Source;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;
import org.vadere.util.logging.Logger;

import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class Simulation {

	private static Logger logger = Logger.getLogger(Simulation.class);

	private final AttributesSimulation attributesSimulation;
	private final AttributesAgent attributesAgent;

	private final Collection<SourceController> sourceControllers;
	private final Collection<TargetController> targetControllers;
	private final Collection<AbsorbingAreaController> absorbingAreaControllers;
	private TeleporterController teleporterController;
	private TopographyController topographyController;
	private DynamicElementFactory dynamicElementFactory;

	private final List<PassiveCallback> passiveCallbacks;
	private final List<RemoteRunListener> remoteRunListeners;
	private List<Model> models;

	private boolean isRunSimulation = false;
	private boolean isPaused = false;
	private boolean singleStepMode; // constructor
	private boolean waitForSimCommand = false;
	private double simulateUntilInSec = -1;

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
	private SimulationResult simulationResult;
	private final EventController eventController;
	private final EventCognition eventCognition;
	private final SalientBehaviorCognition salientBehaviorCognition;

	public Simulation(MainModel mainModel, double startTimeInSec, final String name, ScenarioStore scenarioStore,
					  List<PassiveCallback> passiveCallbacks, Random random, ProcessorManager processorManager,
					  SimulationResult simulationResult, List<RemoteRunListener> remoteRunListeners, boolean singleStepMode) {

		this.name = name;
		this.mainModel = mainModel;
		this.scenarioStore = scenarioStore;
		this.attributesSimulation = scenarioStore.getAttributesSimulation();
		this.attributesAgent = scenarioStore.getTopography().getAttributesPedestrian();
		this.sourceControllers = new LinkedList<>();
		this.targetControllers = new LinkedList<>();
		this.absorbingAreaControllers = new LinkedList<>();
		this.topography = scenarioStore.getTopography();
		this.runTimeInSec = attributesSimulation.getFinishTime();
		this.startTimeInSec = startTimeInSec;
		this.simTimeInSec = startTimeInSec;
		this.simulationResult = simulationResult;

		this.models = mainModel.getSubmodels();
		this.sourceControllerFactory = mainModel.getSourceControllerFactory();

		// TODO [priority=normal] [task=bugfix] - the attributesCar are missing in initialize' parameters
		this.dynamicElementFactory = mainModel;

		this.processorManager = processorManager;
		this.passiveCallbacks = passiveCallbacks;
		this.remoteRunListeners = remoteRunListeners;
		this.singleStepMode = singleStepMode;

		// "eventController" is final. Therefore, create object here and not in helper method.
		this.eventController = new EventController(scenarioStore);
		this.eventCognition = new EventCognition();
		this.salientBehaviorCognition = new SalientBehaviorCognition(topography);

		createControllers(topography, mainModel, random);

		// ::start:: this code is to visualize the potential fields. It may be refactored later.
		if(attributesSimulation.isVisualizationEnabled()) {
			IPotentialFieldTarget pft = null;
			IPotentialField pt = null;
			if(mainModel instanceof PotentialFieldModel) {
				pft = ((PotentialFieldModel) mainModel).getPotentialFieldTarget();
			} else if(mainModel instanceof BehaviouralHeuristicsModel) {
				pft = ((BehaviouralHeuristicsModel) mainModel).getPotentialFieldTarget();
			}

			if(pft != null) {
				pt = (pos, agent) -> {
					if(agent instanceof PedestrianOSM) {
						return ((PedestrianOSM)agent).getPotential(pos);
					}
					else {
						return 0.0;
					}
				};
			}

			for (PassiveCallback pc : this.passiveCallbacks) {
				pc.setPotentialFieldTarget(pft);
				pc.setPotentialField(pt);
			}
		}
		// ::end::

		for (PassiveCallback pc : this.passiveCallbacks) {
			pc.setTopography(topography);
		}
	}

	private void createControllers(Topography topography, MainModel mainModel, Random random) {
		this.topographyController = new TopographyController(topography, mainModel);

		for (Source source : topography.getSources()) {
			SourceController sc = this.sourceControllerFactory
					.create(topography, source, dynamicElementFactory, attributesAgent, random);
			sourceControllers.add(sc);
		}

		for (Target target : topography.getTargets()) {
			targetControllers.add(new TargetController(topography, target));
		}

		for (AbsorbingArea absorbingArea : topography.getAbsorbingAreas()) {
			absorbingAreaControllers.add(new AbsorbingAreaController(topography, absorbingArea));
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
		isRunSimulation = true;
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
		topographyController.postLoop(this.simTimeInSec);

		for (Model m : models) {
			m.postLoop(simTimeInSec);
		}

		for (PassiveCallback c : passiveCallbacks) {
			c.postLoop(simTimeInSec);
		}

		if (attributesSimulation.isWriteSimulationData()) {
			processorManager.postLoop(this.simulationState);
		}

		// notify remoteManger that simulation ended. If a command waited for the next
		// simulation step notify it and execute command with current SimulationState.
		setWaitForSimCommand(true); // its save to read the state now.
		remoteRunListeners.forEach(RemoteRunListener::lastSimulationStepFinishedListener);
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

			while (isRunSimulation) {

				synchronized (this) {
					while (isPaused) {
						try {
							wait();
						} catch (Exception e) {
							isPaused = false;
							Thread.currentThread().interrupt();
							logger.warn("interrupt while isPaused.");
						}
					}
				}

				if (attributesSimulation.isVisualizationEnabled()) {
					sleepTillStartOfNextFrame();
				}

				for (PassiveCallback c : passiveCallbacks) {
					c.preUpdate(simTimeInSec);
				}

				assert assertAllPedestrianInBounds(): "Pedestrians are outside of topography bound.";
				updateCallbacks(simTimeInSec);
				updateWriters(simTimeInSec); // set SimulationState with Time!!!

				if (attributesSimulation.isWriteSimulationData()) {
					processorManager.update(this.simulationState);
				}

				for (PassiveCallback c : passiveCallbacks) {
					c.postUpdate(simTimeInSec);
				}


				// Single step hook
				// Remote Control Hook
				synchronized (this){
					if (singleStepMode){
						// check reached next simTime (-1 simulate one step)
						// round to long to ensure correct trap.
						boolean timeReached = Math.round(simTimeInSec) >= Math.round(simulateUntilInSec);
						if (timeReached || simulateUntilInSec == -1){
							logger.warnf("Simulated until: %.4f", simTimeInSec);

							setWaitForSimCommand(true);
							remoteRunListeners.forEach(RemoteRunListener::simulationStepFinishedListener);
							while (waitForSimCommand){
								logger.warn("wait for next SimCommand...");
								try {
									wait();
								} catch (InterruptedException e) {
									waitForSimCommand = false;
									Thread.currentThread().interrupt();
									logger.warn("interrupt while waitForSimCommand");
								}
							}
						}
					}
				}



				if (runTimeInSec + startTimeInSec > simTimeInSec + 1e-7) {
					simTimeInSec += Math.min(attributesSimulation.getSimTimeStepLength(), runTimeInSec + startTimeInSec - simTimeInSec);
				} else {
					isRunSimulation = false;
				}

				//remove comment to fasten simulation for evacuation simulations
				//if (topography.getElements(Pedestrian.class).size() == 0){
				// isRunSimulation = false;
				//}

				if (Thread.interrupted()) {
					isRunSimulation = false;
					simulationResult.setState("Simulation interrupted");
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
		List<Event> events = eventController.getEventsForTime(simTimeInSec);

		// TODO Why are target controllers readded in each simulation loop?
		// Maybe, Isabella's SIMA branch required this because pedestrians can act as targets there.
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

		for (AbsorbingAreaController absorbingAreaController : this.absorbingAreaControllers) {
			absorbingAreaController.update(simTimeInSec);
		}

		topographyController.update(simTimeInSec); //rebuild CellGrid
		step++;

		Collection<Pedestrian> pedestrians = topography.getElements(Pedestrian.class);

		eventCognition.prioritizeEventsForPedestrians(events, pedestrians);

		if (attributesSimulation.isUseSalientBehavior()) {
			salientBehaviorCognition.setSalientBehaviorForPedestrians(pedestrians, simTimeInSec);
		}

		for (Model m : models) {
			List<SourceController> stillSpawningSource = this.sourceControllers.stream().filter(s -> !s.isSourceFinished(simTimeInSec)).collect(Collectors.toList());
			int pedestriansInSimulation = this.simulationState.getTopography().getPedestrianDynamicElements().getElements().size();
			
			// Only update until there are pedestrians in the scenario or pedestrian to spawn
			if (!stillSpawningSource.isEmpty() || pedestriansInSimulation > 0 ) {
				m.update(simTimeInSec);

				if (topography.isRecomputeCells()) {
					// rebuild CellGrid if model does not manage the CellGrid state while updating
					topographyController.update(simTimeInSec); //rebuild CellGrid
				}
			}
		}

		if (topographyController.getTopography().hasTeleporter()) {
			teleporterController.update(simTimeInSec);
		}
	}

	synchronized void pause() {
		isPaused = true;
	}

	private synchronized boolean isPaused() {
		return isPaused;
	}

	public synchronized boolean isRunning() {
		return isRunSimulation && !isPaused() && !isWaitForSimCommand();
	}

	synchronized boolean isSingleStepMode(){ return singleStepMode;}

	void setSingleStepMode(boolean singleStepMode) {
		this.singleStepMode = singleStepMode;
	}

	boolean isWaitForSimCommand() {
		return waitForSimCommand;
	}

	private void setWaitForSimCommand(boolean waitForSimCommand) {
		this.waitForSimCommand = waitForSimCommand;
	}

	synchronized void nextSimCommand(double simulateUntilInSec){
		this.simulateUntilInSec = simulateUntilInSec;
		waitForSimCommand = false;
		isPaused = false;
		notify();
	}

	synchronized void resume() {
		isPaused = false;
		waitForSimCommand = false;
		singleStepMode = false;
		notify();
	}

	synchronized SimulationState getSimulationState(){
		return simulationState;
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
				isRunSimulation = false;
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
