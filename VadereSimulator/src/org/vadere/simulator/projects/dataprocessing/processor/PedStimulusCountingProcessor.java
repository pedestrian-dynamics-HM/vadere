package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.EventtimePedestrianIdKey;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepKey;
import org.vadere.state.attributes.AttributesSimulation;
import org.vadere.state.attributes.processor.AttributesPedStimulusCountingProcessor;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.simulation.InformationDegree;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@DataProcessorClass(label = "PedStimulusCountingProcessor")
public class PedStimulusCountingProcessor extends DataProcessor<TimestepKey, InformationDegree> {

	private Predicate<Pedestrian> filter_by_stimuli;
	private Pattern filter_pattern = null;
	private double stopIfPercentageIsInformed = 0.95;

	public PedStimulusCountingProcessor() {
		super("numberPedsInformed", "numberPedsAll", "percentageInformed");
	}

	@Override
	public void init(ProcessorManager manager) {
		super.init(manager);
		// setup filter
		AttributesPedStimulusCountingProcessor  attr = getAttributes();

		if (getAttributes().isRegexFilter()){
			filter_pattern = Pattern.compile(getAttributes().getInformationFilter());
			filter_by_stimuli = ped -> ped.getKnowledgeBase().knowsAbout(filter_pattern);
		} else {
			filter_by_stimuli = ped -> ped.getKnowledgeBase().knowsAbout(getAttributes().getInformationFilter());
		}

		stopIfPercentageIsInformed = attr.getStopIfPercentageIsInformed();

	}

	@Override
	protected void doUpdate(SimulationState state) {
		Collection<Pedestrian> peds = state.getTopography().getElements(Pedestrian.class);

		int numberPedsInformed = (int) peds.stream().filter(p -> filter_by_stimuli.test(p)).count();
		int numberPedsAll = (int) peds.stream().filter(p-> p.getFootstepHistory().getFootSteps().size() > 1).count();

		numberPedsAll = Math.max(numberPedsAll,numberPedsInformed);
		InformationDegree informationDegree =  new InformationDegree(numberPedsInformed, numberPedsAll);

		// force stop before simulation time defined in json is reached.
		if (informationDegree.getPercentageInformed() >= stopIfPercentageIsInformed) {
			setStopSimBeforeSimFinish(true);
		}

		putValue(new TimestepKey(state.getStep()), informationDegree);


	}

	@Override
	public AttributesPedStimulusCountingProcessor getAttributes() {
		if(super.getAttributes() == null) {
			setAttributes(new AttributesPedStimulusCountingProcessor());
		}
		return (AttributesPedStimulusCountingProcessor)super.getAttributes();
	}


	@Override
	public String[] toStrings(TimestepKey key) {
		String[] informationDegrees = this.getValue(key).getValueString();
		return informationDegrees;
	}





}
