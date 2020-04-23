package org.vadere.simulator.projects.dataprocessing.processor;


import org.jetbrains.annotations.NotNull;
import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.models.potential.PotentialFieldModel;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.state.attributes.processor.AttributesPedestrianPotentialProcessor;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This processor computes potentials at the position of pedestrians if and only if the {@link org.vadere.simulator.models.MainModel}
 * is in fact a {@link PotentialFieldModel}. The pedestrian position is determined by the registered {@link PedestrianPositionProcessor}.
 * Therefore, this can be, for example, the interpolated position.
 *
 * Note that computing the agent potential can be computational expensive.
 *
 * This processor can be used to compute the geodesic distance to a target if the target potential is a static potential using F(x) = 1 as
 * the traveling speed of the wave in the eikonal equation! By this one can compute the distance between agent with respect to the target.
 * If a targetId is specified i.e. it is not -1 then it will be used instead of the targetId of the agent's target.
 *
 * @author Benedikt Zoennchen
 */
@DataProcessorClass()
public class PedestrianPotentialProcessor extends DataProcessor<TimestepPedestrianIdKey, Double>{

	private PedestrianPositionProcessor pedestrianPositionProcessor;

	public PedestrianPotentialProcessor() {
		super("potential");
	}

	@Override
	protected void doUpdate(@NotNull final SimulationState state) {
		if(state.getMainModel().isPresent() && state.getMainModel().get() instanceof PotentialFieldModel) {
			pedestrianPositionProcessor.update(state);
			PotentialFieldModel model = (PotentialFieldModel) state.getMainModel().get();

			Collection<Pedestrian> pedestrians = state.getTopography().getElements(Pedestrian.class);
			List<Pedestrian> copy = new ArrayList<>(pedestrians);
			Integer timeStep = state.getStep();
			double potential;

			for (Pedestrian pedestrian : pedestrians){
				TimestepPedestrianIdKey key = new TimestepPedestrianIdKey(state.getStep(), pedestrian.getId());
				VPoint pos = pedestrianPositionProcessor.getValue(key);
				int targetId = getAttributes().getTargetId() == -1 ? pedestrian.getNextTargetId() : getAttributes().getTargetId();

				switch (getAttributes().getType()) {
					case TARGET: potential = model.getPotentialFieldTarget().getPotential(pos, targetId); break;
					case OBSTACLE: potential = model.getPotentialFieldObstacle().getObstaclePotential(pos, pedestrian); break;
					case PEDESTRIAN: {
						// TODO: at this point we do not know which agents are relevant so we take them all which might cause expensive computations!
						potential = model.getPotentialFieldAgent().getAgentPotential(pos, pedestrian, copy);
					} break;
					case ALL:
					default: {
						double targetPotential = model.getPotentialFieldTarget().getPotential(pos, targetId);
						double obstaclePotential = model.getPotentialFieldObstacle().getObstaclePotential(pos, pedestrian);
						double agentPotential = model.getPotentialFieldAgent().getAgentPotential(pos, pedestrian, copy);
						potential = targetPotential + obstaclePotential + agentPotential;
					} break;
				}

				this.putValue(new TimestepPedestrianIdKey(timeStep, pedestrian.getId()), potential);
			}
		}
	}

	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);
		this.pedestrianPositionProcessor = (PedestrianPositionProcessor) manager.getProcessor(getAttributes().getPedestrianPositionProcessorId());
	}

	@Override
	public AttributesPedestrianPotentialProcessor getAttributes() {
		if(super.getAttributes() == null) {
			setAttributes(new AttributesPedestrianPotentialProcessor());
		}
		return (AttributesPedestrianPotentialProcessor)super.getAttributes();
	}
}
