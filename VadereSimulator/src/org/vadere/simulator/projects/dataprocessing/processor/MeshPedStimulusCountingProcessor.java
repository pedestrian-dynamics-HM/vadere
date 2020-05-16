package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.state.attributes.processor.AttributesMeshPedStimulusCountingProcessor;
import org.vadere.state.scenario.Pedestrian;

import java.util.Collection;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@DataProcessorClass(label = "MeshPedStimulusCountingProcessor")
public class MeshPedStimulusCountingProcessor extends MeshDensityCountingProcessor {

	private Predicate<Pedestrian> filter_by_stimuli;
	private Pattern filter_pattern = null;

	public MeshPedStimulusCountingProcessor(){
		setAttributes(new AttributesMeshPedStimulusCountingProcessor());
	}

	@Override
	public void init(ProcessorManager manager) {
		super.init(manager);
		// setup filter
		if (getAttributes().isRegexFilter()){
			filter_pattern = Pattern.compile(getAttributes().getInformationFilter());
			filter_by_stimuli = ped -> ped.getKnowledgeBase().knowsAbout(filter_pattern);
		} else {
			filter_by_stimuli = ped -> ped.getKnowledgeBase().knowsAbout(getAttributes().getInformationFilter());
		}
	}

	@Override
	protected void doUpdate(SimulationState state) {
		Collection<Pedestrian> peds = state.getTopography().getElements(Pedestrian.class);

		reset_count();

		// compute count
		for(Pedestrian ped : peds) {
			// update knowledgeBase
			ped.getKnowledgeBase().updateObsolete(state.getSimTimeInSec());
			// filter by knowledge of pedestrian
			if (filter_by_stimuli.test(ped)) {
				doUpdateOnPed(ped);
			}
		}

		write_count(state);
	}

	@Override
	public AttributesMeshPedStimulusCountingProcessor getAttributes() {
		if(super.getAttributes() == null) {
			setAttributes(new AttributesMeshPedStimulusCountingProcessor());
		}
		return (AttributesMeshPedStimulusCountingProcessor)super.getAttributes();
	}
}
