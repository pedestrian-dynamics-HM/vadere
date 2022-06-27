package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.state.attributes.processor.AttributesProcessor;
import org.vadere.state.attributes.processor.AttributesRelevantPedestriansProcessor;
import org.vadere.state.scenario.Pedestrian;

import java.util.Collection;

/**
 * @author Manuel Hertle
 *
 */

@DataProcessorClass()
public class RelevantPedestriansProcessor extends DataProcessor<TimestepPedestrianIdKey, Integer> {

	public RelevantPedestriansProcessor() {
		super("relevant_count");
	}

	@Override
	public void doUpdate(final SimulationState state) {
		Collection<Pedestrian> pedestrians = state.getTopography().getElements(Pedestrian.class);

		for (Pedestrian ped : pedestrians) {
			if (ped instanceof PedestrianOSM) {
				this.putValue(new TimestepPedestrianIdKey(state.getStep(), ped.getId()), ((PedestrianOSM) ped).getRelevantPedestrians().size());
			}
		}
	}

	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);
		AttributesRelevantPedestriansProcessor att = (AttributesRelevantPedestriansProcessor) this.getAttributes();
	}

	@Override
	public String[] toStrings(TimestepPedestrianIdKey key) {
		Integer count = this.getValue(key);
		if(count == null) {
			return new String[]{Integer.toString(-1)};
		}
		else {
			return new String[]{Integer.toString(count)};
		}
	}

    @Override
    public AttributesProcessor getAttributes() {
        if(super.getAttributes() == null) {
            setAttributes(new AttributesRelevantPedestriansProcessor());
        }

        return super.getAttributes();
    }
}
