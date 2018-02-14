package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;
import org.vadere.simulator.projects.dataprocessing.writer.VadereWriterFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class PedestrianWaitingEndTimeProcessorEnv extends ProcessorTestEnv<PedestrianIdKey, Double> {

	PedestrianWaitingEndTimeProcessorEnv(){
		testedProcessor = processorFactory.createDataProcessor(PedestrianWaitingEndTimeProcessor.class);
		testedProcessor.setId(nextProcessorId());

		outputFile = outputFileFactory.createDefaultOutputfileByDataKey(
				PedestrianIdKey.class,
				testedProcessor.getId());
		outputFile.setVadereWriterFactory(VadereWriterFactory.getStringWriterFactory());
	}


	@Override
	public void loadDefaultSimulationStateMocks() {

	}

	@Override
	List<String> getExpectedOutputAsList() {
		List<String> outputList = new ArrayList<>();
		expectedOutput.entrySet()
				.stream()
				.sorted(Comparator.comparing(Map.Entry::getKey))
				.forEach(e ->{
					StringJoiner sj = new StringJoiner(getDelimiter());
					sj.add(Integer.toString(e.getKey().getPedestrianId()))
							.add(Double.toString(e.getValue()));
					outputList.add(sj.toString());
				});
		return outputList;
	}
}
