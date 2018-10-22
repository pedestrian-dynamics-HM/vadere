package org.vadere.simulator.models.bhm;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

import org.jetbrains.annotations.NotNull;
import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.models.MainModel;
import org.vadere.simulator.models.Model;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesBHM;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.DynamicElement;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;
import org.vadere.geometry.shapes.VCircle;
import org.vadere.geometry.shapes.VPoint;
import org.vadere.geometry.shapes.VShape;

@ModelClass(isMainModel = true)
public class BehaviouralHeuristicsModel implements MainModel {

	/**
	 * Compares the time of the next possible move.
	 */
	private class ComparatorPedestrianBHM implements Comparator<PedestrianBHM> {

		@Override
		public int compare(PedestrianBHM ped1, PedestrianBHM ped2) {
			if (ped1.getTimeOfNextStep() < ped2.getTimeOfNextStep()) {
				return -1;
			} else {
				return 1;
			}
		}
	}
	
	private List<Model> models = new LinkedList<>();

	private AttributesBHM attributesBHM;
	private AttributesAgent attributesPedestrian;
	private Random random;
	private Topography topography;
	private double lastSimTimeInSec;
	private int pedestrianIdCounter;
	private PriorityQueue<PedestrianBHM> pedestrianEventsQueue;

	public BehaviouralHeuristicsModel() {
		this.pedestrianIdCounter = 0;
		this.pedestrianEventsQueue = new PriorityQueue<>(100, new ComparatorPedestrianBHM());
	}

	@Override
	public void initialize(List<Attributes> modelAttributesList, Topography topography,
			AttributesAgent attributesPedestrian, Random random) {
		this.attributesBHM = Model.findAttributes(modelAttributesList, AttributesBHM.class);
		this.attributesPedestrian = attributesPedestrian;
		this.topography = topography;
		this.random = random;
		this.models.add(this);
	}

	@Override
	public <T extends DynamicElement> PedestrianBHM createElement(VPoint position, int id, Class<T> type) {
		if (!Pedestrian.class.isAssignableFrom(type))
			throw new IllegalArgumentException("BHM cannot initialize " + type.getCanonicalName());

		pedestrianIdCounter++;
		AttributesAgent pedAttributes = new AttributesAgent(
				this.attributesPedestrian, id > 0 ? id : pedestrianIdCounter);

		PedestrianBHM pedestrian = createElement(position, pedAttributes);
		pedestrian.setPosition(position);
		this.pedestrianEventsQueue.add(pedestrian);
		return pedestrian;
	}

	private PedestrianBHM createElement(VPoint position, @NotNull final AttributesAgent pedAttributes) {
		PedestrianBHM pedestrian = new PedestrianBHM(topography, pedAttributes, attributesBHM, random);
		pedestrian.setPosition(position);
		return pedestrian;
	}

	@Override
	public VShape getDynamicElementRequiredPlace(@NotNull final VPoint position) {
		return new VCircle(position, new AttributesAgent(attributesPedestrian, -1).getRadius()+new AttributesBHM().getSpaceToKeep());
	}

	@Override
	public void preLoop(final double simTimeInSec) {
		this.lastSimTimeInSec = simTimeInSec;
	}


	@Override
	public void postLoop(double simTimeInSec) {}

	@Override
	public void update(final double simTimeInSec) {

		// event driven update
		if (!pedestrianEventsQueue.isEmpty()) {
			while (pedestrianEventsQueue.peek().getTimeOfNextStep() < simTimeInSec) {
				PedestrianBHM ped = pedestrianEventsQueue.poll();

				if (ped.hasNextTarget()) {

					ped.update(simTimeInSec);

					Target target = topography.getTarget(ped.getNextTargetId());

					if (!(target.getShape().contains(ped.getPosition()) && target.isAbsorbing())) {
						pedestrianEventsQueue.add(ped);
					}
				}

				if (pedestrianEventsQueue.isEmpty()) {
					break;
				}
			}
		}
	}

	@Override
	public List<Model> getSubmodels() {
		return models;
	}

}
