package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.models.MainModel;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.models.groups.CentroidGroupModel;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;

import java.util.Optional;

@DataProcessorClass
public class PedestrianGroupIDProcessor extends DataProcessor<TimestepPedestrianIdKey, Integer>{

	public PedestrianGroupIDProcessor(){
		super("groupId");
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
					this.putValue(new TimestepPedestrianIdKey(timeStep, entry.getKey().getId()), entry.getValue().getID());
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
		return new String[]{Integer.toString(i)};
	}
}
