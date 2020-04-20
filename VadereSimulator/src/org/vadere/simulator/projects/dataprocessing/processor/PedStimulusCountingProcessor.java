package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepKey;
import org.vadere.state.attributes.processor.AttributesPedStimulusCountingProcessor;
import org.vadere.state.scenario.Pedestrian;

import java.util.Collection;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@DataProcessorClass(label = "PedStimulusCountingProcessor")
public class PedStimulusCountingProcessor extends DataProcessor<TimestepKey, Long> {

	private Predicate<Pedestrian> filter_by_stimuli;
	private Pattern filter_pattern = null;

	public PedStimulusCountingProcessor(){
		setAttributes(new AttributesPedStimulusCountingProcessor());
	}

	@Override
	public void init(ProcessorManager manager) {
		super.init(manager);
		// setup filter
		AttributesPedStimulusCountingProcessor  attr = getAttributes();

		if (attr.isRegexFilter()){
			filter_pattern = Pattern.compile(attr.getInformationFilter());
			filter_by_stimuli = ped -> ped.getKnowledgeBase().knows_about(filter_pattern);
		} else {
			filter_by_stimuli = ped -> ped.getKnowledgeBase().knows_about(attr.getInformationFilter());
		}
	}

	@Override
	protected void doUpdate(SimulationState state) {
		Collection<Pedestrian> peds = state.getTopography().getElements(Pedestrian.class);

		long percentageInformed = peds.stream().filter(p -> filter_by_stimuli.test(p)).count() / peds.size();

		this.putValue(new TimestepKey(state.getStep()), percentageInformed);

	}

	@Override
	public AttributesPedStimulusCountingProcessor getAttributes() {
		if(super.getAttributes() == null) {
			setAttributes(new AttributesPedStimulusCountingProcessor());
		}
		return (AttributesPedStimulusCountingProcessor)super.getAttributes();
	}


}
