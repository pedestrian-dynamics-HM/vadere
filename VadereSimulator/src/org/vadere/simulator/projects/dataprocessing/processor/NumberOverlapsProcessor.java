package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.NoDataKey;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.state.attributes.processor.AttributesEvacuationTimeProcessor;
import org.vadere.state.attributes.processor.AttributesNumberOverlapsProcessor;
import org.vadere.state.attributes.processor.AttributesPedestrianOverlapProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VPoint;
import javax.swing.JOptionPane;

import javax.print.attribute.IntegerSyntax;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This processor counts the number of overlaps during a simulation run.
 * The is commented code that can be used to show a info box if overlaps occured.
 * 
 * @author Marion Gödel
 */


@DataProcessorClass()
public class NumberOverlapsProcessor extends DataProcessor<NoDataKey, Long> {
	private PedestrianOverlapProcessor pedOverlapProc;


	public NumberOverlapsProcessor() {
		super("nr_overlaps");
		setAttributes(new AttributesNumberOverlapsProcessor());
	}

	@Override
	protected void doUpdate(final SimulationState state) {
		//ensure that all required DataProcessors are updated.
		this.pedOverlapProc.doUpdate(state);
	}

	@Override
	public void postLoop(final SimulationState state) {
		this.pedOverlapProc.postLoop(state);

		long numberOverlaps = this.pedOverlapProc.getValues().stream().mapToInt(val -> val.intValue()).sum() / 2;


		/* // Uncomment this code if you want to get the info box with the number of overlaps
		if (numberOverlaps > 0 ) {
			NumberOverlapsProcessor.infoBox(numberOverlaps + " Overlaps have occured during the simulation!", "Number Overlaps");
			System.out.println("* CAREFUL *: " + numberOverlaps + " Overlaps have occured during the simulation!");
		}else{
			NumberOverlapsProcessor.infoBox("No Overlaps have occured during the simulation :)", "Number Overlaps");

		}
		*/


		this.putValue(NoDataKey.key(), numberOverlaps);
	}

	/*
	// Uncomment this code if you want to get the info box with the number of overlaps (if > 0)
	public static void infoBox(String infoMessage, String titleBar)
	{
		JOptionPane.showMessageDialog(null, infoMessage, titleBar, JOptionPane.INFORMATION_MESSAGE);
	}*/

	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);
		AttributesNumberOverlapsProcessor att = (AttributesNumberOverlapsProcessor) this.getAttributes();
		this.pedOverlapProc = (PedestrianOverlapProcessor) manager.getProcessor(att.getPedestrianOverlapProcessorId());

	}


	@Override
	public AttributesProcessor getAttributes() {
		if (super.getAttributes() == null) {
			setAttributes(new AttributesNumberOverlapsProcessor());
		}

		return super.getAttributes();
	}


}
