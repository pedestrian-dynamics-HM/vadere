package org.vadere.simulator.projects.dataprocessing.processor;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.models.MainModel;
import org.vadere.simulator.models.groups.CentroidGroupModel;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;

import java.util.Optional;

@DataProcessorClass
public class PedestrianGroupSizeProcessor extends DataProcessor<TimestepPedestrianIdKey, Integer>{

	private static Logger logger = LogManager.getLogger(PedestrianGroupIDProcessor.class);

	public PedestrianGroupSizeProcessor() {
			super("groupSize");
	}

	@Override
	protected void doUpdate(SimulationState state) {
		Integer timeStep = state.getStep();
		Optional<MainModel> mainModel =  state.getMainModel();
		if (mainModel.isPresent()){
			Optional<CentroidGroupModel> model = mainModel.get()
					.getSubmodels().stream()
					.filter(m -> m instanceof CentroidGroupModel)
					.map(m -> (CentroidGroupModel) m).findAny();

			if (model.isPresent()){
				model.get().getPedestrianGroupData().entrySet().forEach(entry ->{
					this.putValue(new TimestepPedestrianIdKey(timeStep, entry.getKey().getId()), entry.getValue().getSize());
				});
			}
		}
	}

	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);
	}

	public String[] toStrings(TimestepPedestrianIdKey key){
		Integer i = this.getValue(key);
		if (i == null) {
			logger.warn(String.format("PedestrianGroupSizeProcessor does not has Data for Key: %s",
					key.toString()));
			i = -1;
		}

		return new String[]{Integer.toString(i)};
	}
}
