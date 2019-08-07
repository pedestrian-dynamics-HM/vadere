package org.vadere.simulator.models.osm;

import org.jetbrains.annotations.NotNull;
import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.control.factory.GroupSourceControllerFactory;
import org.vadere.simulator.control.factory.SingleSourceControllerFactory;
import org.vadere.simulator.control.factory.SourceControllerFactory;
import org.vadere.simulator.models.MainModel;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.models.SpeedAdjuster;
import org.vadere.simulator.models.StepSizeAdjuster;
import org.vadere.simulator.models.SubModelBuilder;
import org.vadere.simulator.models.groups.cgm.CentroidGroupModel;
import org.vadere.simulator.models.groups.cgm.CentroidGroupPotential;
import org.vadere.simulator.models.groups.cgm.CentroidGroupSpeedAdjuster;
import org.vadere.simulator.models.osm.optimization.ParticleSwarmOptimizer;
import org.vadere.simulator.models.osm.optimization.PatternSearchOptimizer;
import org.vadere.simulator.models.osm.optimization.StepCircleOptimizer;
import org.vadere.simulator.models.osm.optimization.StepCircleOptimizerBrent;
import org.vadere.simulator.models.osm.optimization.StepCircleOptimizerCircleNelderMead;
import org.vadere.simulator.models.osm.optimization.StepCircleOptimizerDiscrete;
import org.vadere.simulator.models.osm.optimization.StepCircleOptimizerEvolStrat;
import org.vadere.simulator.models.osm.optimization.StepCircleOptimizerGradient;
import org.vadere.simulator.models.osm.optimization.StepCircleOptimizerNelderMead;
import org.vadere.simulator.models.osm.optimization.StepCircleOptimizerPowell;
import org.vadere.simulator.models.osm.updateScheme.UpdateSchemeOSM;
import org.vadere.simulator.models.potential.PotentialFieldModel;
import org.vadere.simulator.models.potential.fields.IPotentialFieldTarget;
import org.vadere.simulator.models.potential.fields.IPotentialFieldTargetGrid;
import org.vadere.simulator.models.potential.fields.PotentialFieldAgent;
import org.vadere.simulator.models.potential.fields.PotentialFieldObstacle;
import org.vadere.simulator.models.potential.solver.calculators.cartesian.GridEikonalSolver;
import org.vadere.simulator.utils.cache.ScenarioCache;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.attributes.models.AttributesOSM;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.events.types.ElapsedTimeEvent;
import org.vadere.state.events.types.Event;
import org.vadere.state.events.types.WaitEvent;
import org.vadere.state.scenario.DynamicElement;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.state.types.OptimizationType;
import org.vadere.state.types.UpdateType;
import org.vadere.util.data.cellgrid.CellGrid;
import org.vadere.util.data.cellgrid.CellState;
import org.vadere.util.data.cellgrid.IPotentialPoint;
import org.vadere.util.data.cellgrid.PathFindingTag;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.logging.Logger;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.vadere.state.types.UpdateType.PARALLEL_OPEN_CL;

@ModelClass(isMainModel = true)
public class OptimalStepsModel implements MainModel, PotentialFieldModel {

	private final static Logger logger = Logger.getLogger(OptimalStepsModel.class);

	private UpdateSchemeOSM updateSchemeOSM;
	private AttributesOSM attributesOSM;
	private AttributesAgent attributesPedestrian;
	private Random random;
	private StepCircleOptimizer stepCircleOptimizer;
	private IPotentialFieldTarget potentialFieldTarget;
	private PotentialFieldObstacle potentialFieldObstacle;
	private PotentialFieldAgent potentialFieldPedestrian;
	private List<SpeedAdjuster> speedAdjusters;
	private List<StepSizeAdjuster> stepSizeAdjusters;
	private Topography topography;
	private double lastSimTimeInSec;
	private ExecutorService executorService;
	private List<Model> models = new LinkedList<>();

	public OptimalStepsModel() {
		this.speedAdjusters = new LinkedList<>();
		this.stepSizeAdjusters = new LinkedList<>();
	}

	@Override
	public void initialize(List<Attributes> modelAttributesList, Topography topography,
						   AttributesAgent attributesPedestrian, Random random) {

		logger.debug("initialize OSM");
		this.attributesOSM = Model.findAttributes(modelAttributesList, AttributesOSM.class);
		this.topography = topography;
		this.random = random;
		this.attributesPedestrian = attributesPedestrian;

		final SubModelBuilder subModelBuilder = new SubModelBuilder(modelAttributesList, topography,
				attributesPedestrian, random);
		logger.debug("build subModels");
		subModelBuilder.buildSubModels(attributesOSM.getSubmodels());
		subModelBuilder.addBuildedSubModelsToList(models);

		logger.debug("create Target potential field");
		IPotentialFieldTargetGrid iPotentialTargetGrid = IPotentialFieldTargetGrid.createPotentialField(
				modelAttributesList, topography, attributesPedestrian, attributesOSM.getTargetPotentialModel());

		this.potentialFieldTarget = iPotentialTargetGrid;
		models.add(iPotentialTargetGrid);

		this.potentialFieldObstacle = PotentialFieldObstacle.createPotentialField(
				modelAttributesList, topography, attributesPedestrian, random, attributesOSM.getObstaclePotentialModel());
		this.potentialFieldPedestrian = PotentialFieldAgent.createPotentialField(
				modelAttributesList, topography, attributesPedestrian, random, attributesOSM.getPedestrianPotentialModel());

		Optional<CentroidGroupModel> opCentroidGroupModel = models.stream().
				filter(ac -> ac instanceof CentroidGroupModel).map(ac -> (CentroidGroupModel) ac).findAny();

		if (opCentroidGroupModel.isPresent()) {

			CentroidGroupModel centroidGroupModel = opCentroidGroupModel.get();
			centroidGroupModel.setPotentialFieldTarget(iPotentialTargetGrid);

			this.potentialFieldPedestrian =
					new CentroidGroupPotential(centroidGroupModel,
							potentialFieldPedestrian, potentialFieldTarget, centroidGroupModel.getAttributesCGM());

			//this.stepSizeAdjusters.add(new CentroidGroupStepSizeAdjuster(centroidGroupModel));
			this.speedAdjusters.add(new CentroidGroupSpeedAdjuster(centroidGroupModel));
		}

		this.stepCircleOptimizer = createStepCircleOptimizer(
				attributesOSM, random, topography, iPotentialTargetGrid);

		// TODO implement a step speed adjuster for this!
		if (attributesPedestrian.isDensityDependentSpeed()) {
			throw new UnsupportedOperationException("densityDependentSpeed not jet implemented.");
			//this.speedAdjusters.add(new SpeedAdjusterWeidmann());
		}

		if (attributesOSM.getUpdateType() == UpdateType.PARALLEL) {
			this.executorService = Executors.newFixedThreadPool(8);
		} else {
			this.executorService = null;
		}

		this.updateSchemeOSM = createUpdateScheme(modelAttributesList, topography, attributesOSM);
		this.topography.addElementAddedListener(Pedestrian.class, updateSchemeOSM);
		this.topography.addElementRemovedListener(Pedestrian.class, updateSchemeOSM);

		models.add(this);
	}

	// Dirty quick implementation to test it! TODO: refactoring!
	private UpdateSchemeOSM createUpdateScheme(
			@NotNull final List<Attributes> attributesList,
			@NotNull final Topography topography,
			@NotNull final AttributesOSM attributesOSM) {
		switch (attributesOSM.getUpdateType()) {
			case PARALLEL_OPEN_CL:
			case PARALLEL_CL:
			case PARALLEL_SHARED_MEM_CL:
			case EVENT_DRIVEN_CL: {
				//throw new UnsupportedOperationException("not jet implemented.");
				return UpdateSchemeOSM.createOpenCLUpdateScheme(
						topography,
						attributesOSM,
						Model.findAttributes(attributesList, AttributesFloorField.class),
						//3.0,

						new GridEikonalSolver() {
							CellGrid cellGrid = null;

							@Override
							public CellGrid getCellGrid() {
								if(cellGrid == null) {
									initialize();
								}
								return cellGrid;
							}

							@Override
							public void initialize() {
								potentialFieldTarget.preLoop(0.4);
								cellGrid = ((IPotentialFieldTargetGrid)potentialFieldTarget).getCellGrids().get(1);
							}

							@Override
							public Function<IPoint, Double> getPotentialField() {
								return getCellGrid().getInterpolationFunction();
							}

							@Override
							public double getPotential(double x, double y) {
								return getPotential(x, y, 0.1, 1.0);
							}

						},

						new GridEikonalSolver() {

							private CellGrid cellGrid = null;

							@Override
							public CellGrid getCellGrid() {
								if(cellGrid == null) {
									initialize();
								}
								return cellGrid;
							}

							@Override
							public void initialize() {
								double resolution = Model.findAttributes(attributesList, AttributesFloorField.class).getPotentialFieldResolution();
								cellGrid = new CellGrid(topography.getBounds().getWidth(), topography.getBounds().getHeight(), resolution, new CellState());
								cellGrid.pointStream().forEach(p -> {
									double distance = topography.distanceToObstacle(cellGrid.pointToCoord(p));
									PathFindingTag tag = distance >= 0 ? PathFindingTag.Reached : PathFindingTag.Obstacle;
									cellGrid.setValue(p, new CellState(distance, tag));
								});
							}

							@Override
							public Function<IPoint, Double> getPotentialField() {
								if(cellGrid == null) {
									initialize();
								}
								return cellGrid.getInterpolationFunction();
							}

							@Override
							public double getPotential(double x, double y) {
								return cellGrid.getInterpolatedValueAt(x, y).getLeft();
							}
						},attributesOSM.getUpdateType()
						);
			}
			default: return UpdateSchemeOSM.create(attributesOSM.getUpdateType(), topography, random);
		}
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
			case NELDER_MEAD_CIRCLE:
				result = new StepCircleOptimizerCircleNelderMead(random, attributesOSM);
				break;
			case POWELL:
				result = new StepCircleOptimizerPowell(random);
				break;
			case PSO:
				result = new ParticleSwarmOptimizer(movementThreshold, random);
				break;
			case PATTERN_SEARCH:
				result = new PatternSearchOptimizer(movementThreshold, attributesOSM, random);
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
	public void postLoop(final double simTimeInSec) {
	}

	@Override
	public void update(final double simTimeInSec) {
		double timeStepInSec = simTimeInSec - this.lastSimTimeInSec;
		updateSchemeOSM.update(timeStepInSec, simTimeInSec);
		lastSimTimeInSec = simTimeInSec;
	}

		/*
	 * At the moment all pedestrians also the initalPedestrians get this.attributesPedestrain!!!
	 */
	@Override
	public <T extends DynamicElement> PedestrianOSM createElement(VPoint position, int id, Class<T> type) {
		if (!Pedestrian.class.isAssignableFrom(type))
			throw new IllegalArgumentException("OSM cannot initialize " + type.getCanonicalName());

		AttributesAgent pedAttributes = new AttributesAgent(
				this.attributesPedestrian, registerDynamicElementId(topography, id));

		PedestrianOSM pedestrianOSM = createElement(position, pedAttributes);
		return pedestrianOSM;
	}

	@Override
	public VShape getDynamicElementRequiredPlace(@NotNull final VPoint position) {
		return createElement(position,  new AttributesAgent(attributesPedestrian, -1)).getShape();
	}

	private PedestrianOSM createElement(VPoint position, @NotNull final AttributesAgent attributesAgent) {
		PedestrianOSM pedestrian = new PedestrianOSM(attributesOSM,
				attributesAgent, topography, random, potentialFieldTarget,
				potentialFieldObstacle.copy(), potentialFieldPedestrian,
				speedAdjusters, stepCircleOptimizer.clone());
		pedestrian.setPosition(position);
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

	@Override
	public SourceControllerFactory getSourceControllerFactory() {
		Optional<CentroidGroupModel> opCentroidGroupModel = models.stream()
				.filter(ac -> ac instanceof CentroidGroupModel)
				.map(ac -> (CentroidGroupModel) ac).findAny();
		if (opCentroidGroupModel.isPresent()) {
			return new GroupSourceControllerFactory(opCentroidGroupModel.get());
		}

		return new SingleSourceControllerFactory();
	}

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
}
