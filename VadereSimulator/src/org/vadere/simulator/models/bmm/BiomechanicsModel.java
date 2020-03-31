package org.vadere.simulator.models.bmm;

import org.jetbrains.annotations.NotNull;
import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.models.MainModel;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesBHM;
import org.vadere.state.attributes.models.AttributesBMM;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.psychology.cognition.UnsupportedSelfCategoryException;
import org.vadere.state.scenario.DynamicElement;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.state.simulation.FootStep;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 
 * MainModel READY!
 * 
 *
 */
@ModelClass(isMainModel = true)
public class BiomechanicsModel implements MainModel {

	private List<Model> models = new LinkedList<>();

	private AttributesBMM attributesBMM;
	private AttributesBHM attributesBHM;
	private AttributesAgent attributesPedestrian;
	private Random random;
	private Topography topography;
	private List<PedestrianBMM> pedestriansBMM;
	protected double lastSimTimeInSec;

	public BiomechanicsModel() {
		this.pedestriansBMM = new LinkedList<>();
	}

	@Override
	public void initialize(List<Attributes> modelAttributesList, Domain domain,
	                       AttributesAgent attributesPedestrian, Random random) {
		this.attributesBHM = Model.findAttributes(modelAttributesList, AttributesBHM.class);
		this.attributesBMM = Model.findAttributes(modelAttributesList, AttributesBMM.class);
		this.attributesPedestrian = attributesPedestrian;
		this.topography = domain.getTopography();
		this.random = random;
		this.models.add(this);
	}

	@Override
	public <T extends DynamicElement> PedestrianBMM createElement(VPoint position, int id, Class<T> type) {
		return createElement(position, id, this.attributesPedestrian, type);
	}

	@Override
	public <T extends DynamicElement> PedestrianBMM createElement(VPoint position, int id, Attributes attr, Class<T> type) {

		AttributesAgent aAttr = (AttributesAgent)attr;

		if (!Pedestrian.class.isAssignableFrom(type))
			throw new IllegalArgumentException("BMM cannot initialize " + type.getCanonicalName());
		AttributesAgent pedAttributes = new AttributesAgent(
				aAttr, registerDynamicElementId(topography, id));

		PedestrianBMM pedestrian = createElement(position, pedAttributes);
		this.pedestriansBMM.add(pedestrian);

		return pedestrian;
	}

	private PedestrianBMM createElement(@NotNull final VPoint position, @NotNull final AttributesAgent pedAttributes) {
		PedestrianBMM pedestrian = new PedestrianBMM(position, topography, pedAttributes, attributesBMM, attributesBHM, random);
		return pedestrian;
	}

	@Override
	public VShape getDynamicElementRequiredPlace(@NotNull final VPoint position) {
		return createElement(position, new AttributesAgent(attributesPedestrian, -1)).getShape();
	}

	@Override
	public void preLoop(final double simTimeInSec) {
		this.lastSimTimeInSec = simTimeInSec;
	}


	@Override
	public void postLoop(double simTimeInSec) {}

	@Override
	public void update(final double simTimeInSec) {
		double deltaTime = simTimeInSec - lastSimTimeInSec;

		List<VPoint> positions = pedestriansBMM.stream().map(ped -> ped.getPosition()).collect(Collectors.toList());

		UnsupportedSelfCategoryException.throwIfPedestriansNotTargetOrientied(pedestriansBMM, this.getClass());

		for (PedestrianBMM agent : pedestriansBMM) {
			agent.update(simTimeInSec, deltaTime);
		}

		for (PedestrianBMM agent : pedestriansBMM) {
			agent.move(simTimeInSec, deltaTime);
		}

		for (PedestrianBMM agent : pedestriansBMM) {
			agent.reverseCollisions();
		}

		for(int i = 0; i < pedestriansBMM.size(); i++) {
			PedestrianBMM agent = pedestriansBMM.get(i);
			agent.clearFootSteps();

			FootStep currentFootstep = new FootStep(positions.get(i), agent.getPosition(), lastSimTimeInSec, simTimeInSec);
			agent.getTrajectory().add(currentFootstep);
			agent.getFootstepHistory().add(currentFootstep);
		}

		this.lastSimTimeInSec = simTimeInSec;
	}

	@Override
	public List<Model> getSubmodels() {
		return models;
	}

}
