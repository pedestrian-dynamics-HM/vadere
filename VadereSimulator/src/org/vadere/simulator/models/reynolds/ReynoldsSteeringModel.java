package org.vadere.simulator.models.reynolds;

import org.jetbrains.annotations.NotNull;
import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.models.MainModel;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.models.reynolds.behaviour.CollisionAvoidance;
import org.vadere.simulator.models.reynolds.behaviour.Containment;
import org.vadere.simulator.models.reynolds.behaviour.Seek;
import org.vadere.simulator.models.reynolds.behaviour.Separation;
import org.vadere.simulator.models.reynolds.behaviour.WallAvoidance;
import org.vadere.simulator.models.reynolds.behaviour.Wander;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesReynolds;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.psychology.perception.exceptions.UnsupportedStimulusException;
import org.vadere.state.scenario.DynamicElement;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.geometry.shapes.Vector2D;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

@ModelClass(isMainModel = true)
public class ReynoldsSteeringModel implements MainModel {

	private AttributesReynolds attributesReynolds;
	private AttributesAgent attributesPedestrian;
	private Random random;
	private Topography topography;

	private Seek bSeek;
	private Separation bSeparation;
	private Containment bContainment;
	private CollisionAvoidance bCollisionAvoidance;
	private WallAvoidance bWallAvoidance;
	private Wander bWander;
	private List<Model> submodels;

	public ReynoldsSteeringModel() {
		this.bSeek = new Seek(this);
		this.bSeparation = new Separation(this);
		this.bContainment = new Containment(this);
		this.bCollisionAvoidance = new CollisionAvoidance(this);
		this.bWallAvoidance = new WallAvoidance(this);
		this.bWander = new Wander(this);
	}

	@Override
	public void initialize(List<Attributes> modelAttributesList, Topography topography,
						   AttributesAgent attributesPedestrian, Random random) {

		this.attributesReynolds = Model.findAttributes(modelAttributesList, AttributesReynolds.class);
		this.attributesPedestrian = attributesPedestrian;
		this.topography = topography;
		this.random = random;

		submodels = Collections.singletonList(this);

	}

	@Override
	public void preLoop(final double simTimeInSec) {}

	@Override
	public void postLoop(final double simTimeInSec) {}

	@Override
	public void update(final double simTimeInSec) {
		Collection<Pedestrian> pedestrians = topography.getElements(Pedestrian.class);

		UnsupportedStimulusException.throwIfNotElapsedTimeEvent(pedestrians, this.getClass());

		Iterator<Pedestrian> it = pedestrians.iterator();
		double maxSpeed = 3;

		for (; it.hasNext();) {
			PedestrianReynolds ped = (PedestrianReynolds) it.next();
			Vector2D mov = new Vector2D(0, 0);

			mov = mov.add(bSeek.nextStep(simTimeInSec, mov, ped));
			mov = mov.add(bWander.nextStep(simTimeInSec, mov, ped));
			mov = mov.add(bCollisionAvoidance.nextStep(simTimeInSec, mov, ped));
			mov = mov.add(bWallAvoidance.nextStep(simTimeInSec, mov, ped));
			mov = mov.add(bSeparation.nextStep(simTimeInSec, mov, ped));
			mov = mov.add(bContainment.nextStep(simTimeInSec, mov, ped));

			// if movement is faster than max speed,
			// no normal movement is available, skip this turn.
			if (mov.getLength() > maxSpeed) {
				mov = new Vector2D(0, 0);
			}

			ped.move(simTimeInSec, mov);
		}
	}

	public Topography getScenario() {
		return this.topography;
	}

	public AttributesAgent getAttributesPedestrian() {
		return this.attributesPedestrian;
	}

	public AttributesReynolds getAttributesReynolds() {
		return this.attributesReynolds;
	}

	@Override
	public <T extends DynamicElement> Pedestrian createElement(VPoint position, int id, Class<T> type) {
		if (!Pedestrian.class.isAssignableFrom(type))
			throw new IllegalArgumentException("RSM cannot initialize " + type.getCanonicalName());
		AttributesAgent pedAttributes = new AttributesAgent(
				attributesPedestrian, registerDynamicElementId(topography, id));
		Pedestrian result = create(position, pedAttributes);
		return result;
	}

	private Pedestrian create(@NotNull final VPoint position, @NotNull final AttributesAgent attributesAgent) {
		Pedestrian pedestrian = new PedestrianReynolds(attributesAgent, random);
		pedestrian.setPosition(position);
		return pedestrian;
	}

	@Override
	public VShape getDynamicElementRequiredPlace(@NotNull final VPoint position) {
		return create(position, new AttributesAgent(attributesPedestrian, -1)).getShape();
	}

	@Override
	public List<Model> getSubmodels() {
		return submodels;
	}

}
