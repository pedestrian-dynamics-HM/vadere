package org.vadere.simulator.models.bhm;

import org.jetbrains.annotations.NotNull;
import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.models.MainModel;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.models.potential.fields.IPotentialFieldTarget;
import org.vadere.simulator.models.potential.fields.IPotentialFieldTargetGrid;
import org.vadere.simulator.models.potential.fields.PotentialFieldTargetGrid;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.exceptions.AttributesNotFoundException;
import org.vadere.state.attributes.models.AttributesBHM;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.DynamicElement;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

@ModelClass(isMainModel = true)
public class BehaviouralHeuristicsModel implements MainModel {

	private IPotentialFieldTarget potentialFieldTarget;

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
	private PriorityQueue<PedestrianBHM> pedestrianEventsQueue;

	public BehaviouralHeuristicsModel() {
		this.pedestrianEventsQueue = new PriorityQueue<>(100, new ComparatorPedestrianBHM());
	}

	public IPotentialFieldTarget getPotentialFieldTarget() {
		return potentialFieldTarget;
	}

	@Override
	public void initialize(List<Attributes> modelAttributesList, Domain domain,
	                       AttributesAgent attributesPedestrian, Random random) {

		try {
			potentialFieldTarget = IPotentialFieldTargetGrid.createPotentialField(
					modelAttributesList, domain, attributesPedestrian, PotentialFieldTargetGrid.class.getCanonicalName());
			this.models.add(potentialFieldTarget);
		} catch (AttributesNotFoundException e) {
			potentialFieldTarget = null;
		}

		this.attributesBHM = Model.findAttributes(modelAttributesList, AttributesBHM.class);
		this.attributesPedestrian = attributesPedestrian;
		this.topography = domain.getTopography();
		this.random = random;
		this.models.add(this);
	}

	@Override
	public <T extends DynamicElement> PedestrianBHM createElement(VPoint position, int id, Class<T> type) {
		return createElement(position, id, this.attributesPedestrian, type);
	}

	@Override
	public <T extends DynamicElement> PedestrianBHM createElement(VPoint position, int id, Attributes attr, Class<T> type) {

		AttributesAgent aAttr = (AttributesAgent)attr;

		if (!Pedestrian.class.isAssignableFrom(type))
			throw new IllegalArgumentException("BHM cannot initialize " + type.getCanonicalName());

		AttributesAgent pedAttributes = new AttributesAgent(
				aAttr, registerDynamicElementId(topography, id));

		PedestrianBHM pedestrian = createElement(position, pedAttributes);
		pedestrian.setPosition(position);
		this.pedestrianEventsQueue.add(pedestrian);
		return pedestrian;
	}

	private PedestrianBHM createElement(VPoint position, @NotNull final AttributesAgent pedAttributes) {
		PedestrianBHM pedestrian = new PedestrianBHM(topography, pedAttributes, attributesBHM, random, potentialFieldTarget);
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

		// all those foot steps are done in previous sim time steps
		for(PedestrianBHM ped : pedestrianEventsQueue) {
			ped.clearFootSteps();
		}

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
		topography.setRecomputeCells(true);
	}

	@Override
	public List<Model> getSubmodels() {
		return models;
	}

}
