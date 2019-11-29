package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;
import org.vadere.state.attributes.processor.AttributesPedestrianEvacuationTimeProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;
import org.vadere.state.scenario.Pedestrian;

/**
 * Problems with this class:
 * 
 * - evacuation time is saved on every update (inefficient)
 * - only works if agents are deleted at their targets
 * 
 * A more better way could be implemented using target listener. In this case it
 * is important to check that the target is the agent's final (last) target.
 * 
 * @author Mario Teixeira Parente
 * @author Jakob Schöttl
 *
 */
@DataProcessorClass()
public class PedestrianEvacuationTimeProcessor extends DataProcessor<PedestrianIdKey, Double> {
	private PedestrianStartTimeProcessor pedStartTimeProc;

	public PedestrianEvacuationTimeProcessor() {
		super("evacuationTime");
		setAttributes(new AttributesPedestrianEvacuationTimeProcessor());
	}
	
	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);
		AttributesPedestrianEvacuationTimeProcessor att = (AttributesPedestrianEvacuationTimeProcessor) this.getAttributes();
		pedStartTimeProc = (PedestrianStartTimeProcessor) manager.getProcessor(att.getPedestrianStartTimeProcessorId());
	}

//	@Override
//	public void preLoop(SimulationState state) {
//		final TargetListener targetListener = new TargetListener() {
//			@Override
//			public void reachedTarget(Target target, Agent agent) {
//				final PedestrianIdKey pedKey = new PedestrianIdKey(agent.getId());
//				final double timeUntilTargetReached = state.getSimTimeInSec() - pedStartTimeProc.getValue(pedKey);
//				System.out.println(timeUntilTargetReached);
//				setValue(pedKey, timeUntilTargetReached);
//			}
//		};
//
//		for (Target target : state.getTopography().getTargets()) {
//			target.addListener(targetListener);
//		}
//	}

	@Override
	protected void doUpdate(final SimulationState state) {
		pedStartTimeProc.update(state);

		state.getTopography().getElements(Pedestrian.class).stream()
				.map(ped -> new PedestrianIdKey(ped.getId()))
				.forEach(key -> this.putValue(key, state.getSimTimeInSec() - pedStartTimeProc.getValue(key)));
	}

	@Override
	public void postLoop(final SimulationState state) {
		state.getTopography().getElements(Pedestrian.class).stream()
				.map(ped -> new PedestrianIdKey(ped.getId()))
				.forEach(key -> this.putValue(key, Double.POSITIVE_INFINITY));
	}

    @Override
    public AttributesProcessor getAttributes() {
        if(super.getAttributes() == null) {
            setAttributes(new AttributesPedestrianEvacuationTimeProcessor());
        }

        return super.getAttributes();
    }
}
