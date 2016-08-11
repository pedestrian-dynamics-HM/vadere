package org.vadere.simulator.projects.dataprocessing.processors;

import java.util.Map;
import java.util.Optional;

import com.google.gson.annotations.Expose;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.simulator.control.OutputGenerator;
import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.models.osm.OptimalStepsModel;
import org.vadere.state.attributes.models.AttributesOSM;
import org.vadere.util.data.Table;

public class StrideLengthProcessor extends AbstractProcessor {

	@Expose
	private static Logger logger = LogManager.getLogger(StrideLengthProcessor.class);

	public StrideLengthProcessor() {
		super(new Table("step", "time", "id", "strideLength", "strideTime", "x", "y"));
	}

	@Override
	public Table postUpdate(final SimulationState state) {
		Table table = getTable();
		Optional<OutputGenerator> optGenerator = state.getOutputGenerator(OptimalStepsModel.class);
		if (optGenerator.isPresent()) {
			OutputGenerator generator = optGenerator.get();
			Map<String, Table> strideTables = generator.getOutputTables();

			int lastStep = -1;

			if (table.isEmpty() || state.getStep() != lastStep) {
				table.clear();

				for (Map.Entry<String, Table> e : strideTables.entrySet()) {

					Table strides = e.getValue();

					for (int i = 0; i < strides.size(); i++) {
						table.addRow();
						table.addColumnEntry("step", state.getStep());
						table.addColumnEntry("time", state.getSimTimeInSec());
						table.addColumnEntry("id", e.getKey());
						table.addColumnEntry("strideLength", strides.getEntry("strideLength", i));
						table.addColumnEntry("strideTime", strides.getEntry("strideTime", i));

						int pedestrianId = Integer.parseInt(e.getKey().toString());

						table.addColumnEntry("x", state.getPedestrianPosition(pedestrianId).x);
						table.addColumnEntry("y", state.getPedestrianPosition(pedestrianId).y);
					}

				}
			}
			lastStep = state.getStep();
		} else {
			logger.warn("missing OutputGenerator for " + AttributesOSM.class);
		}
		return table;
	}

}
