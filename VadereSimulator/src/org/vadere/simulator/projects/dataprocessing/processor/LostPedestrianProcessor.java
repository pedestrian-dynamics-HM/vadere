package org.vadere.simulator.projects.dataprocessing.processor;

import org.apache.commons.lang3.tuple.Pair;
import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.models.groups.cgm.CentroidGroup;
import org.vadere.simulator.models.groups.cgm.CentroidGroupModel;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.simulator.projects.dataprocessing.processor.util.ModelFilter;
import org.vadere.state.attributes.processor.AttributesLostPedestrianProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;
import org.vadere.state.attributes.processor.AttributesRelevantPedestriansProcessor;
import org.vadere.state.scenario.Pedestrian;

import java.util.Collection;
import java.util.Map;

/**
 * @author Manuel Hertle
 *
 */

@DataProcessorClass()
public class LostPedestrianProcessor extends DataProcessor<TimestepPedestrianIdKey, Pair<Boolean, Double>> implements ModelFilter {

	public LostPedestrianProcessor() {
		super("lost", "desired_speed");
	}

	@Override
	public void doUpdate(final SimulationState state) {
		Collection<Pedestrian> pedestrians = state.getTopography().getElements(Pedestrian.class);
		CentroidGroupModel groupModel = (CentroidGroupModel) getModel(state, CentroidGroupModel.class).get();
		Map<Integer, CentroidGroup> allGroups = groupModel.getGroupsById();

		for (Pedestrian ped : pedestrians) {
				if (ped instanceof PedestrianOSM) {
					boolean lost = allGroups.get(ped.getGroupIds().getFirst()).getLostMembers().contains(ped);
					this.putValue(new TimestepPedestrianIdKey(state.getStep(), ped.getId()), Pair.of(lost, ((PedestrianOSM) ped).getDesiredSpeed()));
				}
			}
	}

	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);
		AttributesProcessor att = (AttributesLostPedestrianProcessor) this.getAttributes();
	}

	@Override
	public String[] toStrings(TimestepPedestrianIdKey key) {
			Pair<Boolean, Double> data = this.getValue(key);
			if (data == null) {
				System.out.println("teeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeest");
				return new String[]{"NA", "NA"};
			}
			return new String[]{Boolean.toString(data.getLeft()), Double.toString(data.getRight())};
	}

    @Override
    public AttributesProcessor getAttributes() {
        if(super.getAttributes() == null) {
            setAttributes(new AttributesLostPedestrianProcessor());
        }

        return super.getAttributes();
    }
}
