package org.vadere.simulator.control;

import org.apache.commons.math3.distribution.RealDistribution;
import org.vadere.simulator.models.DynamicElementFactory;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.scenario.AttributesDynamicElement;
import org.vadere.state.attributes.scenario.AttributesSource;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Car;
import org.vadere.state.scenario.DistributionFactory;
import org.vadere.state.scenario.DynamicElement;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Source;
import org.vadere.state.scenario.Topography;
import org.vadere.state.util.SpawnArray;
import org.vadere.util.geometry.LinkedCellsGrid;
import org.vadere.geometry.shapes.VCircle;
import org.vadere.geometry.shapes.VPoint;
import org.vadere.geometry.shapes.VRectangle;

import java.awt.geom.Rectangle2D;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public abstract class SourceController {

	protected final double NO_EVENT = Double.MAX_VALUE;
	public static final double SPAWN_BUFFER_SIZE = 0.03;

	protected final Source source;
	private final DynamicElementFactory dynamicElementFactory;

	private final Topography topography;
	protected final Random random;

	/** <code>null</code>, if there is no next event. */
	protected Double timeOfNextEvent;
	protected RealDistribution distribution;
	protected final AttributesSource sourceAttributes;
	protected final AttributesDynamicElement attributesDynamicElement;
	protected int dynamicElementsCreatedTotal;
	protected final SpawnArray spawnArray;


	public SourceController(Topography scenario, Source source,
							DynamicElementFactory dynamicElementFactory,
							AttributesDynamicElement attributesDynamicElement,
							Random random) {
		this.source = source;
		this.sourceAttributes = source.getAttributes();
		this.attributesDynamicElement = attributesDynamicElement;
		this.dynamicElementFactory = dynamicElementFactory;
		this.topography = scenario;
		this.random = random;

		VRectangle elementBound = new VRectangle(dynamicElementFactory.getDynamicElementRequiredPlace(new VPoint(0,0)).getBounds2D());

		this.spawnArray = new SpawnArray(new VRectangle(source.getShape().getBounds2D()),
				new VRectangle(0, 0,elementBound.getWidth() + SPAWN_BUFFER_SIZE, elementBound.getHeight() + SPAWN_BUFFER_SIZE));

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

	protected List<DynamicElement> getDynElementsAtSource() {
		Rectangle2D rec = source.getShape().getBounds2D();
		double maxDim = rec.getWidth() > rec.getHeight() ? rec.getWidth() : rec.getHeight();
		return getDynElementsAtPosition(source.getShape().getCentroid(), maxDim / 2);
	}

	protected List<DynamicElement> getDynElementsAtPosition(VPoint sourcePosition, double radius) {
		LinkedCellsGrid<DynamicElement> dynElements = topography.getSpatialMap(DynamicElement.class);
		return dynElements.getObjects(sourcePosition, radius);
	}

	abstract public void update(double simTimeInSec);


	protected boolean isSourceFinished(double simTimeInSec) {
		if (isMaximumNumberOfSpawnedElementsReached()) {
			return true;
		}
		if (isSourceWithOneSingleSpawnEvent()) {
			return dynamicElementsCreatedTotal == sourceAttributes.getSpawnNumber();
		}
		return isAfterSourceEndTime(simTimeInSec) && isQueueEmpty();
	}

	protected boolean isSourceWithOneSingleSpawnEvent() {
		return sourceAttributes.getStartTime() == sourceAttributes.getEndTime();
	}

	protected boolean isAfterSourceEndTime(double time) {
		return time > sourceAttributes.getEndTime();
	}

	protected boolean isMaximumNumberOfSpawnedElementsReached() {
		final int maxNumber = sourceAttributes.getMaxSpawnNumberTotal();
		return maxNumber != AttributesSource.NO_MAX_SPAWN_NUMBER_TOTAL
				&& dynamicElementsCreatedTotal >= maxNumber;
	}

	abstract protected boolean isQueueEmpty();

	abstract protected void determineNumberOfSpawnsAndNextEvent(double simTimeInSec);

	protected Topography getTopography() {
		return topography;
	}

	protected void createNextEvent() {
		if (isSourceWithOneSingleSpawnEvent()) {
			timeOfNextEvent = NO_EVENT;
			return;
		}

		// sample() could yield negative results. but that is a problem of the distribution.
		timeOfNextEvent += distribution.sample();

		if (isAfterSourceEndTime(timeOfNextEvent)) {
			timeOfNextEvent = NO_EVENT;
		}

	}

	/**
	 * note that most models create their own pedestrians and ignore the attributes given here.
	 * the source is mostly used to set the position and target ids, not the attributes.
	 */
	protected void addNewAgentToScenario(final List<VPoint> position) {
		position.forEach(p -> addNewAgentToScenario(p));
	}

	protected void addNewAgentToScenario(final VPoint position) {
		Agent newElement = (Agent) createDynamicElement(position);

		// TODO [priority=high] [task=refactoring] this is bad programming. Why is the target list added later?
		// What if Pedestrian does something with the target list before it is stored?

		// if the pedestrian itself has no targets, add the targets from the source
		// TODO [priority=high] [task=refactoring] why only if he has no targets? because the createElement method
		// might add some.
		if (newElement.getTargets().isEmpty()) {
			newElement.setTargets(new LinkedList<>(sourceAttributes.getTargetIds()));
		}

		topography.addElement(newElement);
	}

	private DynamicElement createDynamicElement(final VPoint position) {
		Agent result;
		switch (sourceAttributes.getDynamicElementType()) {
			case PEDESTRIAN:
				result = (Agent) dynamicElementFactory.createElement(position, 0, Pedestrian.class);
				break;
			case CAR:
				result = (Agent) dynamicElementFactory.createElement(position, 0, Car.class);
				break;
			default:
				throw new IllegalArgumentException("The controller's source has an unsupported element type: "
						+ sourceAttributes.getDynamicElementType());
		}
		result.setSource(source);
		return result;
	}
}
