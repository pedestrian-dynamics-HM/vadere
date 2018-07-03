package org.vadere.simulator.models.osm;

import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.models.MainModel;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.models.SpeedAdjuster;
import org.vadere.simulator.models.SubModelBuilder;
import org.vadere.simulator.models.groups.CentroidGroupModel;
import org.vadere.simulator.models.groups.CentroidGroupPotential;
import org.vadere.simulator.models.groups.CentroidGroupSpeedAdjuster;
import org.vadere.simulator.models.osm.optimization.StepCircleOptimizer;
import org.vadere.simulator.models.osm.optimization.StepCircleOptimizerBrent;
import org.vadere.simulator.models.osm.optimization.StepCircleOptimizerDiscrete;
import org.vadere.simulator.models.osm.optimization.StepCircleOptimizerEvolStrat;
import org.vadere.simulator.models.osm.optimization.StepCircleOptimizerGradient;
import org.vadere.simulator.models.osm.optimization.StepCircleOptimizerNelderMead;
import org.vadere.simulator.models.osm.optimization.StepCircleOptimizerPowell;
import org.vadere.simulator.models.osm.updateScheme.ParallelWorkerOSM;
import org.vadere.simulator.models.osm.updateScheme.UpdateSchemeOSM.CallMethod;
import org.vadere.simulator.models.potential.PotentialFieldModel;
import org.vadere.simulator.models.potential.fields.IPotentialFieldTargetGrid;
import org.vadere.simulator.models.potential.fields.PotentialFieldAgent;
import org.vadere.simulator.models.potential.fields.PotentialFieldObstacle;
import org.vadere.simulator.models.potential.fields.IPotentialFieldTarget;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesOSM;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.DynamicElement;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.state.types.OptimizationType;
import org.vadere.state.types.UpdateType;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.io.ListUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@ModelClass(isMainModel = true)
public class OptimalStepsModel implements MainModel, PotentialFieldModel {

	/**
	 * Compares the time of the next possible move.
	 */
	private class ComparatorPedestrianOSM implements Comparator<PedestrianOSM> {

		@Override
		public int compare(PedestrianOSM ped1, PedestrianOSM ped2) {
			// TODO [priority=low] [task=refactoring] use Double.compare() oder compareTo()
			if (ped1.getTimeOfNextStep() < ped2.getTimeOfNextStep()) {
				return -1;
			} else {
				return 1;
			}
		}
	}

	private AttributesOSM attributesOSM;
	private AttributesAgent attributesPedestrian;
	private Random random;
	private StepCircleOptimizer stepCircleOptimizer;
	private IPotentialFieldTarget potentialFieldTarget;
	private PotentialFieldObstacle potentialFieldObstacle;
	private PotentialFieldAgent potentialFieldPedestrian;
	private List<SpeedAdjuster> speedAdjusters;
	private Topography topography;
	private double lastSimTimeInSec;
	private int pedestrianIdCounter;
	private PriorityQueue<PedestrianOSM> pedestrianEventsQueue;

	private ExecutorService executorService;
	private List<Model> models = new LinkedList<>();

	public OptimalStepsModel() {
		this.pedestrianIdCounter = 0;
		this.speedAdjusters = new LinkedList<>();
	}

	@Override
	public void initialize(List<Attributes> modelAttributesList, Topography topography,
			AttributesAgent attributesPedestrian, Random random) {

		this.attributesOSM = Model.findAttributes(modelAttributesList, AttributesOSM.class);
		this.topography = topography;
		this.random = random;
		this.attributesPedestrian = attributesPedestrian;

		final SubModelBuilder subModelBuilder = new SubModelBuilder(modelAttributesList, topography,
				attributesPedestrian, random);
		subModelBuilder.buildSubModels(attributesOSM.getSubmodels());
		subModelBuilder.addBuildedSubModelsToList(models);

		IPotentialFieldTargetGrid iPotentialTargetGrid = IPotentialFieldTargetGrid.createPotentialField(
				modelAttributesList, topography, attributesPedestrian, attributesOSM.getTargetPotentialModel());

		this.potentialFieldTarget = iPotentialTargetGrid;
		models.add(iPotentialTargetGrid);

		this.potentialFieldObstacle = PotentialFieldObstacle.createPotentialField(modelAttributesList, topography, attributesPedestrian, random, attributesOSM.getObstaclePotentialModel());
		this.potentialFieldPedestrian = PotentialFieldAgent.createPotentialField(modelAttributesList, topography, attributesPedestrian, random, attributesOSM.getPedestrianPotentialModel());
		
		Optional<CentroidGroupModel> opCentroidGroupModel = models.stream().
			filter(ac -> ac instanceof CentroidGroupModel).map(ac -> (CentroidGroupModel)ac).findAny();
		
		if (opCentroidGroupModel.isPresent()) {
			
			CentroidGroupModel centroidGroupModel = opCentroidGroupModel.get();
			centroidGroupModel.setPotentialFieldTarget(iPotentialTargetGrid);
			
			this.potentialFieldPedestrian =
					new CentroidGroupPotential(centroidGroupModel,
							potentialFieldPedestrian, centroidGroupModel.getAttributesCGM());
			
			SpeedAdjuster speedAdjusterCGM = new CentroidGroupSpeedAdjuster(centroidGroupModel);
			this.speedAdjusters.add(speedAdjusterCGM);
		}

		this.stepCircleOptimizer = createStepCircleOptimizer(
				attributesOSM, random, topography, iPotentialTargetGrid);

		if (attributesPedestrian.isDensityDependentSpeed()) {
			this.speedAdjusters.add(new SpeedAdjusterWeidmann());
		}

		if (attributesOSM.getUpdateType() == UpdateType.EVENT_DRIVEN) {
			this.pedestrianEventsQueue = new PriorityQueue<>(100,
					new ComparatorPedestrianOSM());
		} else {
			// not needed and should not be used in this case
			this.pedestrianEventsQueue = null;
		}

		if (attributesOSM.getUpdateType() == UpdateType.PARALLEL) {
			this.executorService = Executors.newFixedThreadPool(8);
		} else {
			this.executorService = null;
		}

		models.add(this);
	}

	private StepCircleOptimizer createStepCircleOptimizer(
			AttributesOSM attributesOSM, Random random, Topography topography,
			IPotentialFieldTargetGrid potentialFieldTarget) {

		StepCircleOptimizer result;
		double movementThreshold = attributesOSM.getMovementThreshold();

		OptimizationType type = attributesOSM.getOptimizationType();
		if (type == null) {
			type = OptimizationType.DISCRETE;
		}

		switch (type) {
			case BRENT:
				result = new StepCircleOptimizerBrent(random);
				break;
			case EVOLUTION_STRATEGY:
				result = new StepCircleOptimizerEvolStrat();
				break;
			case NELDER_MEAD:
				result = new StepCircleOptimizerNelderMead(random);
				break;
			case POWELL:
				result = new StepCircleOptimizerPowell(random);
				break;
			case GRADIENT:
				result = new StepCircleOptimizerGradient(topography,
						potentialFieldTarget, attributesOSM);
				break;
			case DISCRETE:
			case NONE:
			default:
				result = new StepCircleOptimizerDiscrete(movementThreshold, random);
				break;
		}

		return result;
	}

	@Override
	public void preLoop(final double simTimeInSec) {
		this.lastSimTimeInSec = simTimeInSec;
	}

	@Override
	public void postLoop(final double simTimeInSec) {}

	@Override
	public void update(final double simTimeInSec) {

		final double timeStepInSec = simTimeInSec - this.lastSimTimeInSec;
		// event driven update
		if (attributesOSM.getUpdateType() == UpdateType.EVENT_DRIVEN
				&& !pedestrianEventsQueue.isEmpty()) {
			while (pedestrianEventsQueue.peek().getTimeOfNextStep() < simTimeInSec) {
				PedestrianOSM ped = pedestrianEventsQueue.poll();
				ped.update(timeStepInSec, simTimeInSec, CallMethod.EVENT_DRIVEN);
				pedestrianEventsQueue.add(ped);
			}

		} else {
			// time step length


			// parallel update
			if (attributesOSM.getUpdateType() == UpdateType.PARALLEL) {
				parallelCall(timeStepInSec);
			} else {
				List<PedestrianOSM> pedestrians = ListUtils.select(
						topography.getElements(Pedestrian.class), PedestrianOSM.class);

				// random shuffle update
				if (attributesOSM.getUpdateType() == UpdateType.SHUFFLE) {
					Collections.shuffle(pedestrians, this.random);
				}

				// default is fixed order sequential update
				for (PedestrianOSM pedestrian : pedestrians) {
					pedestrian.update(timeStepInSec, -1, CallMethod.SEQUENTIAL);

				}
			}

			this.lastSimTimeInSec = simTimeInSec;
		}
	}

	private void parallelCall(double timeStepInSec) {
		CallMethod[] callMethods = {CallMethod.SEEK, CallMethod.MOVE, CallMethod.CONFLICTS, CallMethod.STEPS};
		List<Future<?>> futures;

		for (CallMethod callMethod : callMethods) {
			futures = new LinkedList<>();
			for (final PedestrianOSM pedestrian : ListUtils.select(
					topography.getElements(Pedestrian.class), PedestrianOSM.class)) {
				Runnable worker = new ParallelWorkerOSM(callMethod, pedestrian,
						timeStepInSec);
				futures.add(executorService.submit(worker));
			}
			collectFutures(futures);
		}
	}

	private void collectFutures(final List<Future<?>> futures) {
		try {
			for (Future<?> future : futures) {
				future.get();
			}
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		// restore interruption in order to stop simulation
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/*
	 * At the moment all pedestrians also the initalPedestrians get this.attributesPedestrain!!!
	 */
	@Override
	public <T extends DynamicElement> PedestrianOSM createElement(VPoint position, int id, Class<T> type) {
		if (!Pedestrian.class.isAssignableFrom(type))
			throw new IllegalArgumentException("OSM cannot initialize " + type.getCanonicalName());

		pedestrianIdCounter++;
		AttributesAgent pedAttributes = new AttributesAgent(
				this.attributesPedestrian, id > 0 ? id : pedestrianIdCounter);

		PedestrianOSM pedestrian = new PedestrianOSM(attributesOSM,
				pedAttributes, topography, random, potentialFieldTarget,
				potentialFieldObstacle.copy(), potentialFieldPedestrian,
				speedAdjusters, stepCircleOptimizer.clone());

		pedestrian.setPosition(position);

		if (attributesOSM.getUpdateType() == UpdateType.EVENT_DRIVEN) {
			this.pedestrianEventsQueue.add(pedestrian);
		}

		return pedestrian;
	}

	@Override
	public List<Model> getSubmodels() {
		return models;
	}

	@Override
	public IPotentialFieldTarget getPotentialFieldTarget() {
		return potentialFieldTarget;
	}

	@Override
	public PotentialFieldObstacle getPotentialFieldObstacle() {
		return potentialFieldObstacle;
	}

	@Override
	public PotentialFieldAgent getPotentialFieldAgent() {
		return potentialFieldPedestrian;
	}
}
