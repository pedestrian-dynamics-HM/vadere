package org.vadere.simulator.control;

import java.awt.geom.Rectangle2D;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.distribution.RealDistribution;
import org.vadere.simulator.models.DynamicElementFactory;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.scenario.AttributesCar;
import org.vadere.state.attributes.scenario.AttributesDynamicElement;
import org.vadere.state.attributes.scenario.AttributesSource;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Car;
import org.vadere.state.scenario.ConstantDistribution;
import org.vadere.state.scenario.DistributionFactory;
import org.vadere.state.scenario.DynamicElement;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.state.scenario.Source;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.LinkedCellsGrid;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.math.MathUtil;

public class SourceController {

	protected final Source source;
	protected DynamicElementFactory dynamicElementFactory;
	protected final Topography topography;
	protected final Random random;
	private final AttributesSource sourceAttributes;
	private AttributesDynamicElement attributesDynamicElement;

	// TODO [priority=high] [task=refactoring] remove this from the SourceController and add a new attribute.
	// This is ONLY used for "useFreeSpaceOnly".
	private VCircle dynamicElementShape;

	private int dynamicElementsToCreate;
	private int dynamicElementsCreatedTotal;

	/** <code>null</code>, if there is no next event. */
	private Double timeOfNextEvent;
	private RealDistribution distribution;

	public SourceController(Topography scenario, Source source,
			DynamicElementFactory dynamicElementFactory,
			AttributesDynamicElement attributesDynamicElement, Random random) {
		this.source = source;
		this.sourceAttributes = source.getAttributes();
		this.dynamicElementFactory = dynamicElementFactory;
		this.topography = scenario;
		this.random = random;
		this.attributesDynamicElement = attributesDynamicElement;
		this.dynamicElementShape = getDynamicElementShape();


		dynamicElementsToCreate = 0;
		dynamicElementsCreatedTotal = 0;

		timeOfNextEvent = sourceAttributes.getStartTime();
		try {
			DistributionFactory factory = DistributionFactory
					.fromDistributionClassName(sourceAttributes.getInterSpawnTimeDistribution());
			distribution = factory.createDistribution(random, sourceAttributes.getDistributionParameters());
		} catch (Exception e) {
			throw new IllegalArgumentException("problem with scenario parameters for source: "
					+ "interSpawnTimeDistribution and/or distributionParameters. see causing excepion.", e);
		}
	}

	public void update(double simTimeInSec) {
		// both spawn algorithms should be equivalent for constant distributions.
		// the new algorithm passes all tests of the old one.

		if (distribution instanceof ConstantDistribution) {
			useSpawnDelayAlgorithm(simTimeInSec);
		} else {
			// new generic spawn algorithm using distributions
			useDistributionSpawnAlgorithm(simTimeInSec);
		}
	}

	/**
	 * Update computes the elapsed time since the last time sample
	 * exceeds/matches the interval and for the case that the current simulation
	 * time is in the range of startTimeInSec and endTimeInSec, the specified
	 * number of pedestrians are created and PedestrianAddListeners are
	 * notified. Each pedestrian target is requested to observe the pedestrian
	 * position and notify its PedestrianRemoveListeners when the pedestrian
	 * arrives at the target.
	 * 
	 * This is the older algorithm that only works for constant inter-spawn times.
	 */
	private void useSpawnDelayAlgorithm(double simTimeInSec) {

		double elapsedTimeSinceStart = simTimeInSec
				- this.source.getAttributes().getStartTime();

		int dynamicElementsCreated = 0;
		int numSpawnIntervals = (int) Math.max(1,
				(elapsedTimeSinceStart / sourceAttributes.getSpawnDelay()) + 1);
		int maxSpawnIntervals = (int) ((sourceAttributes.getEndTime() - sourceAttributes
				.getStartTime()) / sourceAttributes.getSpawnDelay()) + 1;

		numSpawnIntervals = Math.min(maxSpawnIntervals, numSpawnIntervals);

		if (elapsedTimeSinceStart < 0) {
			dynamicElementsToCreate = 0;
		} else {
			dynamicElementsToCreate = sourceAttributes.getSpawnNumber()
					* numSpawnIntervals - dynamicElementsCreatedTotal;
		}

		if (dynamicElementsCreatedTotal >= maxSpawnIntervals
				* sourceAttributes.getSpawnNumber()) {
			return;
		}

		LinkedList<VPoint> positions = getDynamicElementPositions(dynamicElementsToCreate);

		/*
		 * Run through all positions and create for each position a DynamicElement
		 * if allowed.
		 */
		for (VPoint position : positions) {
			if (isPositionWorkingForSpawn(position)) {
				create(position);
				++dynamicElementsCreated;
			}
		}

		dynamicElementsToCreate -= dynamicElementsCreated;
		dynamicElementsCreatedTotal += dynamicElementsCreated;

	}

	private boolean isPositionWorkingForSpawn(VPoint position) {
		if (sourceAttributes.isUseFreeSpaceOnly()) {
			// Verify if position is obstructed by other pedestrian.
			for (ScenarioElement neighbor : getDynElementsAtPosition(position, dynamicElementShape)) {
				if (neighbor.getShape().distance(position) < dynamicElementShape.getRadius() * 2) {
					// Position may be obstructed by other pedestrian. Hence, don't create pedestrian now but try again next frame.
					return false;
				}
			}
		}
		return true;
	}

	private VCircle getDynamicElementShape() {
		if (attributesDynamicElement instanceof AttributesAgent) {
			return new VCircle(((AttributesAgent) attributesDynamicElement).getRadius());
		}
		if (attributesDynamicElement instanceof AttributesCar) {
			return new VCircle(((AttributesCar) attributesDynamicElement).getRadius());
		}

		return new VCircle(0.2);
	}

	private List<DynamicElement> getDynElementsAtPosition(VPoint sourcePosition, VCircle dynElementShape) {
		LinkedCellsGrid<DynamicElement> dynElements = topography.getSpatialMap(DynamicElement.class);
		return dynElements.getObjects(sourcePosition, dynElementShape.getRadius() * 3);
	}

	private void useDistributionSpawnAlgorithm(double simTimeInSec) {
		if (!isSourceFinished(simTimeInSec)) {
			if (hasNextEvent()) {
				determineNumberOfSpawnsAndNextEvent(simTimeInSec);
			}
			tryToSpawnOutstandingDynamicElements();
		}
	}

	private boolean isSourceFinished(double simTimeInSec) {
		if (isMaximumNumberOfSpawnedElementsReached()) {
			return true;
		}
		if (isSourceWithOneSingleSpawnEvent()) {
			return dynamicElementsCreatedTotal == sourceAttributes.getSpawnNumber();
		}
		return isAfterSourceEndTime(simTimeInSec) && dynamicElementsToCreate == 0;
	}

	private boolean isMaximumNumberOfSpawnedElementsReached() {
		final int maxNumber = sourceAttributes.getMaxSpawnNumberTotal();
		return maxNumber != AttributesSource.NO_MAX_SPAWN_NUMBER_TOTAL
				&& dynamicElementsCreatedTotal >= maxNumber;
	}

	private boolean isSourceWithOneSingleSpawnEvent() {
		return sourceAttributes.getStartTime() == sourceAttributes.getEndTime();
	}

	private void determineNumberOfSpawnsAndNextEvent(double simTimeInSec) {
		if (simTimeInSec >= timeOfNextEvent) {
			dynamicElementsToCreate += sourceAttributes.getSpawnNumber();

			if (isSourceWithOneSingleSpawnEvent()) {
				timeOfNextEvent = null;
				return;
			}

			// sample() could yield negative results. but that is a problem of the distribution.
			timeOfNextEvent += distribution.sample();

			if (isAfterSourceEndTime(timeOfNextEvent)) {
				timeOfNextEvent = null;
				return;
			}

			// if still timeOfNextEvent still behind current time -> call this function recursively
			// (condition is checked above)
			determineNumberOfSpawnsAndNextEvent(simTimeInSec);
		}
	}

	private boolean hasNextEvent() {
		return timeOfNextEvent != null;
	}

	private boolean isAfterSourceEndTime(double time) {
		return time > sourceAttributes.getEndTime();
	}

	private void tryToSpawnOutstandingDynamicElements() {
		for (VPoint position : getDynamicElementPositions(dynamicElementsToCreate)) {
			if (!isMaximumNumberOfSpawnedElementsReached() && isPositionWorkingForSpawn(position)) {
				create(position);
				dynamicElementsToCreate--;
				dynamicElementsCreatedTotal++;
			}
		}
	}

	/**
	 * note that most models create their own pedestrians and ignore the attributes given here.
	 * the source is mostly used to set the position and target ids, not the attributes.
	 */
	protected DynamicElement create(final VPoint position) {
		Agent newElement = null;

		switch (this.source.getAttributes().getDynamicElementType()) {
			case PEDESTRIAN:
				newElement = (Pedestrian) dynamicElementFactory.createElement(position, 0, Pedestrian.class);
				break;

			case CAR:
				newElement = (Car) dynamicElementFactory.createElement(position, 0, Car.class);
				break;

			default:
				throw new IllegalArgumentException("The source of the controller has the wrong element type: "
						+ this.source.getAttributes().getDynamicElementType());
		}

		// TODO [priority=high] [task=refactoring] this is bad programming. Why is the target list added later?
		// What if Pedestrian does something with the target list before it is stored?

		// if the pedestrian itself has no targets, add the targets from the source
		// TODO [priority=high] [task=refactoring] why only if he has no targets? because the createElement method
		// might add some.
		if (newElement.getTargets().isEmpty()) {
			newElement.setTargets(new LinkedList<>(source.getAttributes().getTargetIds()));
		}

		// TODO [priority=high] [task=refactoring] why do we need to add this HERE, and not in the calling method?
		topography.addElement(newElement);

		return newElement;
	}

	private LinkedList<VPoint> getDynamicElementPositions(int numDynamicElements) {

		LinkedList<VPoint> positions = new LinkedList<>();
		Rectangle2D rect = source.getShape().getBounds2D();
		int numPedX = numDynamicElements;
		double ds = 0;

		if (rect.getHeight() == 0 && rect.getWidth() == 0) {
			rect = new Rectangle2D.Double(rect.getMinX(), rect.getMinY(), 0.2,
					0.2);
		}

		ds = Math.sqrt((rect.getWidth() * rect.getHeight()) / numDynamicElements);
		numPedX = (int) Math.ceil(rect.getWidth() / ds);

		double[][] quasiRandoms = null;

		if (sourceAttributes.isSpawnAtRandomPositions()) {
			quasiRandoms = MathUtil.quasiRandom2D(random, numDynamicElements,
					rect.getWidth(), rect.getHeight(), 1.0);
		}

		for (int iDE = 0; iDE < numDynamicElements; ++iDE) {
			VPoint pos;
			if (sourceAttributes.isSpawnAtRandomPositions()) {
				pos = new VPoint(quasiRandoms[iDE][0] + rect.getMinX(),
						quasiRandoms[iDE][1] + rect.getMinY());
			} else {
				pos = new VPoint(rect.getMinX() + (iDE % numPedX) * ds,
						rect.getMinY() + (iDE / numPedX) * ds);
			}
			positions.add(pos);
		}

		return positions;
	}
}
