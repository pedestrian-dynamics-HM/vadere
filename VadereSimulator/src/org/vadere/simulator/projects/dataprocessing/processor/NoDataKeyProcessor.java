package org.vadere.simulator.projects.dataprocessing.processor;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.SimulationResult;
import org.vadere.simulator.projects.dataprocessing.datakey.NoDataKey;

/**
 * @author Benedikt Zoennchen
 * @param <V>
 */
public abstract class NoDataKeyProcessor<V> extends DataProcessor<NoDataKey, V> {

	private String header;

	protected NoDataKeyProcessor() {
		super();
	}

	protected NoDataKeyProcessor(final String header) {
		super(new String[]{header});
		this.header = header;
	}

	public String getSimulationResultHeader() {
		return header;
	}

	@Override
	public void postLoopAddResultInfo(@NotNull final SimulationState state, @NotNull final SimulationResult result){
		result.addData(getSimulationResultHeader() + " (PID" + getId() + ")", getValue(NoDataKey.key()));
	}

}
