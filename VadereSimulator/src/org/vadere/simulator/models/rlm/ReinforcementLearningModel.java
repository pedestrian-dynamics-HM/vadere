package org.vadere.simulator.models.rlm;

import org.jetbrains.annotations.NotNull;
import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.models.MainModel;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.models.bmm.PedestrianBMM;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesBHM;
import org.vadere.state.attributes.models.AttributesRLM;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.psychology.perception.exceptions.UnsupportedStimulusException;
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

@ModelClass(isMainModel = true)
public class ReinforcementLearningModel implements MainModel {

	private List<Model> models = new LinkedList<>();

	private AttributesRLM attributesRLM;
	private AttributesBHM attributesBHM;
	private AttributesAgent attributesPedestrian;
	private Random random;
	private Topography topography;
	private List<PedestrianRLM> pedestriansRLM;
	protected double lastSimTimeInSec;

	public ReinforcementLearningModel() {
		this.pedestriansRLM = new LinkedList<>();
	}

	@Override
	public void initialize(List<Attributes> modelAttributesList, Topography topography,
						   AttributesAgent attributesPedestrian, Random random) {
		this.attributesRLM = Model.findAttributes(modelAttributesList, AttributesRLM.class);
		this.attributesRLM = Model.findAttributes(modelAttributesList, AttributesRLM.class);
		this.attributesPedestrian = attributesPedestrian;
		this.topography = topography;
		this.random = random;
		this.models.add(this);
	}

	@Override
	public <T extends DynamicElement> PedestrianRLM createElement(VPoint position, int id, Class<T> type) {
		if (!Pedestrian.class.isAssignableFrom(type))
			throw new IllegalArgumentException("RLM cannot initialize " + type.getCanonicalName());
		AttributesAgent pedAttributes = new AttributesAgent(
				attributesPedestrian, registerDynamicElementId(topography, id));

		PedestrianRLM pedestrian = createElement(position, pedAttributes);
		this.pedestriansRLM.add(pedestrian);

		return pedestrian;
	}

	private PedestrianRLM createElement(@NotNull final VPoint position, @NotNull final AttributesAgent pedAttributes) {
		PedestrianRLM pedestrian = new PedestrianRLM(position, topography, pedAttributes, attributesRLM, attributesBHM, random);
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

		List<VPoint> positions = pedestriansRLM.stream().map(ped -> ped.getPosition()).collect(Collectors.toList());

		UnsupportedStimulusException.throwIfNotElapsedTimeEvent(pedestriansRLM, this.getClass());

		for (PedestrianRLM agent : pedestriansRLM) {
			agent.update(simTimeInSec, deltaTime);
		}

		for (PedestrianRLM agent : pedestriansRLM) {
			agent.move(simTimeInSec, deltaTime);
		}

		for (PedestrianRLM agent : pedestriansRLM) {
			agent.reverseCollisions();
		}

		for(int i = 0; i < pedestriansRLM.size(); i++) {
			PedestrianRLM agent = pedestriansRLM.get(i);
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
