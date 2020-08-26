package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepKey;
import org.vadere.state.attributes.processor.AttributesReadAndSetControllerInput;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;


/**
 * @author Christina Mayr
 * reads and sets controller input defined in an external file
 */

@DataProcessorClass()
public class ReadAndSetControllerInput extends DataProcessor<TimestepKey, Double> {

	private double[][] controllerInputs;


	public ReadAndSetControllerInput() {
		super("percentageLeftRealized");
		setAttributes(new AttributesReadAndSetControllerInput());
	}


	@Override
	public void preLoop(SimulationState state) {
		String fileName = getAttributes().getControllerInputFile();
		// fileName = "Scenarios/Demos/Density_controller/scenarios/TwoCorridors_forced_controller_input.csv";

		try {
			this.controllerInputs = Files.lines(Paths.get(fileName)).map(s -> s.split(" ")).map(s -> Arrays.stream(s).mapToDouble(Double::parseDouble).toArray()).toArray(double[][]::new);
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println( this.controllerInputs[0][0] );

		int i = 0;

	}

	@Override
	protected void doUpdate(SimulationState state) {

	}

	@Override
	public AttributesReadAndSetControllerInput getAttributes() {
		if (super.getAttributes() == null) {
			setAttributes(new AttributesReadAndSetControllerInput());
		}
		return (AttributesReadAndSetControllerInput)super.getAttributes();
	}


}
