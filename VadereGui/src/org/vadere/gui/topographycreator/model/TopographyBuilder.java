package org.vadere.gui.topographycreator.model;

import java.awt.geom.Rectangle2D;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.LinkedList;

import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.scenario.AttributesTopography;
import org.vadere.state.scenario.*;
import org.vadere.util.geometry.shapes.VPoint;

/**
 * A TopographyBuilder builds a Topography-Object step by step. After the Topography-Object is build
 * it can
 * no longer modified but the TopographyBuilder can be modified (there is no possibility to
 * remove/set members).
 * To solve this problem, use this TopographyBuilder but only in the topographycreator-package! Each
 * build Topography
 * is a new Object. The references of the members of two Topography-Object can be the same because
 * they wont be cloned.
 * 
 *
 */
public class TopographyBuilder implements Iterable<ScenarioElement> {
	// TopographyElements
	private LinkedList<AgentWrapper> pedestrians;
	private LinkedList<Obstacle> obstacles;
	private LinkedList<Stairs> stairs;
	private LinkedList<Source> sources;
	private LinkedList<Target> targets;
	private Teleporter teleporter;
	private LinkedList<ScenarioElement> topographyElements;
	private AttributesTopography attributes;
	private AttributesAgent attributesPedestrian;

	/**
	 * Default-Constructor that initialize an empty TopographyBuilder.
	 */
	public TopographyBuilder() {
		pedestrians = new LinkedList<>();
		obstacles = new LinkedList<>();
		stairs = new LinkedList<>();
		sources = new LinkedList<>();
		targets = new LinkedList<>();
		topographyElements = new LinkedList<>();
		attributes = new AttributesTopography();
	}

	/**
	 * Initial a new TopgraphyBuilder with members of a Topography by using reflection.
	 * 
	 * @param topography the topography that member-references will be copied.
	 */
	public TopographyBuilder(final Topography topography) {
		try {
			obstacles = new LinkedList<>(topography.getObstacles());
			stairs = new LinkedList<>(topography.getStairs());
			LinkedList<Pedestrian> pedStores = new LinkedList<>(topography.getInitialElements(Pedestrian.class));
			pedestrians = new LinkedList<>();
			for (Pedestrian pedStore : pedStores) {
				pedestrians.add(new AgentWrapper(pedStore));
			}
			sources = new LinkedList<>(topography.getSources());
			targets = new LinkedList<>(topography.getTargets());
			teleporter = topography.getTeleporter();
		} catch (SecurityException | IllegalArgumentException e) {
			e.printStackTrace();
		}
		attributes = topography.getAttributes();
		attributesPedestrian = topography.getAttributesPedestrian();
		topographyElements = new LinkedList<>();
		topographyElements.addAll(obstacles);
		topographyElements.addAll(stairs);
		topographyElements.addAll(pedestrians);
		topographyElements.addAll(sources);
		topographyElements.addAll(targets);
	}

	/**
	 * Copy-Constructor (all objects will be copied, not only the references!).
	 * 
	 * @param builder the orign
	 */
	public TopographyBuilder(final TopographyBuilder builder) {
		// this is not a efficient but a secure way
		this(builder.build().clone());
	}

	@Override
	protected TopographyBuilder clone() throws CloneNotSupportedException {
		return new TopographyBuilder(this);
	}

	private static Object getPrivateFieldValueFromTopography(final String fieldName, Topography topography)
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		Field field = topography.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		return field.get(topography);
	}

	public AttributesTopography getAttributes() {
		return attributes;
	}

	public Topography build() {
		Topography topography = new Topography(attributes, attributesPedestrian);

		for (Obstacle obstacle : obstacles)
			topography.addObstacle(obstacle);

		for (Stairs stairs : this.stairs)
			topography.addStairs(stairs);

		for (Source source : sources)
			topography.addSource(source);

		for (Target target : targets)
			topography.addTarget(target);

		for (AgentWrapper pedestrian : pedestrians)
			topography.addInitialElement(pedestrian.getAgentInitialStore());

		topography.setTeleporter(teleporter);

		return topography;
	}

	public ScenarioElement selectElement(final VPoint position) {
		for (ScenarioElement element : topographyElements)
			if (element.getShape().intersects(new Rectangle2D.Double(position.x - 0.1, position.y - 0.1, 0.2, 0.2)))
				return element;
		return null;
	}

	public boolean removeElement(final ScenarioElement element) {
		this.topographyElements.remove(element);

		switch (element.getType()) {
			case OBSTACLE:
				return obstacles.remove(element);
			case STAIRS:
				return stairs.remove(element);
			case PEDESTRIAN:
				return pedestrians.remove(element);
			case TARGET:
				return targets.remove(element);
			case SOURCE:
				return sources.remove(element);
			default:
				return false;
		}
	}

	// setter, getter, adder, remover, iterators
	public Teleporter getTeleporter() {
		return teleporter;
	}

	public void setAttributes(AttributesTopography attributes) {
		this.attributes = attributes;
	}

	public void setTeleporter(Teleporter teleporter) {
		this.teleporter = teleporter;
	}

	public void addPedestrian(final AgentWrapper pedWrappper) {
		this.topographyElements.add(pedWrappper);
		this.pedestrians.add(pedWrappper);
	}

	public void addObstacle(final Obstacle obstacle) {
		this.topographyElements.add(obstacle);
		this.obstacles.add(obstacle);
	}

	public void addStairs(final Stairs stairs) {
		this.topographyElements.add(stairs);
		this.stairs.add(stairs);
	}

	public void addSource(final Source source) {
		this.topographyElements.add(source);
		this.sources.add(source);
	}

	public void addTarget(final Target target) {
		this.topographyElements.add(target);
		this.targets.add(target);
	}

	public Target removeLastTarget() {
		Target target = targets.removeLast();
		topographyElements.remove(target);
		return target;
	}

	public Source removeLastSource() {
		Source source = sources.removeLast();
		topographyElements.remove(source);
		return source;
	}

	public Obstacle removeLastObstacle() {
		Obstacle obstacle = obstacles.removeLast();
		topographyElements.remove(obstacle);
		return obstacle;
	}

	public Stairs removeLastStairs() {
		Stairs stairs = this.stairs.removeLast();
		topographyElements.remove(stairs);
		return stairs;
	}

	public AgentWrapper removeLastPedestrian() {
		AgentWrapper pedestrian = pedestrians.removeLast();
		topographyElements.remove(pedestrian);
		return pedestrian;
	}

	public Teleporter removeTeleporter() {
		Teleporter teleporter = this.teleporter;
		setTeleporter(null);
		return teleporter;
	}

	public Iterator<Obstacle> getObstacleIterator() {
		return obstacles.iterator();
	}

	public Iterator<Stairs> getStairsIterator() {
		return stairs.iterator();
	}

	public Iterator<Target> getTargetIterator() {
		return targets.iterator();
	}

	public Iterator<Source> getSourceIterator() {
		return sources.iterator();
	}

	public Iterator<AgentWrapper> getPedestrianIterator() {
		return pedestrians.iterator();
	}

	@Override
	public Iterator<ScenarioElement> iterator() {
		return topographyElements.iterator();
	}
}
