package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.NoDataKey;
import org.vadere.state.attributes.processor.AttributesMaxOverlapProcessor;
import org.vadere.state.attributes.processor.AttributesNumberOverlapsProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;

import javax.swing.*;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * This processor saves the largest overlap (2*pedRadius - distance between the pedestrian's centers) for one simulation.
 * The processor depends on the PedestrianOverlapDistProcessor. It only works if all pedestrians in the simulation have
 * the same radius.
 * 
 * @author Marion Gödel
 */


@DataProcessorClass()
public class MaxOverlapProcessor extends DataProcessor<NoDataKey, Double> {
	private PedestrianOverlapDistProcessor pedOverlapProc;


	public MaxOverlapProcessor() {
		super("max_size_overlap");
		setAttributes(new AttributesMaxOverlapProcessor());

	}

	@Override
	protected void doUpdate(final SimulationState state) {
		//ensure that all required DataProcessors are updated.
		this.pedOverlapProc.doUpdate(state);
	}

	@Override
	public void postLoop(final SimulationState state) {
		this.pedOverlapProc.postLoop(state);

		OptionalDouble maximumOverlap = this.pedOverlapProc.getValues().stream().filter(val -> val > 0).mapToDouble(val -> val.doubleValue()).max();

		if(maximumOverlap.isPresent()){
			this.putValue(NoDataKey.key(),maximumOverlap.getAsDouble());

			/* // Uncomment  if you want a info box to inform you about the maximum overlap
			MaxOverlapProcessor.infoBox("Minimum distance between centers: " + maximumOverlap + " meters" , "Maximum Overlap");
			*/

		}else{
			this.putValue(NoDataKey.key(), null);
		}
	}


	/* // Uncomment  if you want a info box to inform you about the maximum overlap
	public static void infoBox(String infoMessage, String titleBar)
	{
		JOptionPane.showMessageDialog(null, infoMessage, titleBar, JOptionPane.INFORMATION_MESSAGE);
	}
	*/

	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);
		AttributesMaxOverlapProcessor att = (AttributesMaxOverlapProcessor) this.getAttributes();
		this.pedOverlapProc = (PedestrianOverlapDistProcessor) manager.getProcessor(att.getPedestrianMaxOverlapProcessorId());

	}


	@Override
	public AttributesProcessor getAttributes() {
		if (super.getAttributes() == null) {
			setAttributes(new AttributesMaxOverlapProcessor());
		}

		return super.getAttributes();
	}


}
