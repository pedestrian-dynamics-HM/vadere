package org.vadere.simulator.projects.dataprocessing.processor;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPositionKey;
import org.vadere.state.attributes.processor.AttributesFloorFieldProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;

/**
 * @author Mario Teixeira Parente
 */

public class TargetFloorFieldProcessor extends DataProcessor<TimestepPositionKey, Double> {
	private static Logger logger = LogManager.getLogger(TargetFloorFieldProcessor.class);
	private AttributesFloorFieldProcessor att;
	private int targetId;

    public TargetFloorFieldProcessor() {
        super("potential");
    }

    @Override
    protected void doUpdate(final SimulationState state) {
	    throw new UnsupportedOperationException("not jet implemented.");

	    /*Optional<MainModel> optMainModel = state.getMainModel();

	    if(optMainModel.isPresent() && optMainModel.get() instanceof PotentialFieldModel) {
		    PotentialFieldTarget pft = ((PotentialFieldModel) optMainModel.get()).getPotentialFieldTarget();
		    Rectangle.Double bound = state.getTopography().getBounds();

		    // First try, TODO: Implementation
		    for (double x = bound.x; x < bound.x + bound.width; x += att.getResolution()) {
			    for (double y = bound.y; y < bound.y + bound.height; y += att.getResolution()) {
				    this.setValue(new TimestepPositionKey(state.getStep(), new VPoint(x, y)), 0.0);
			    }
		    }

	    }
	    else {
		    logger.warn("could not process, main model is missing or is not the instance of " + PotentialFieldModel.class.getName());
	    }*/
    }

    @Override
    public void init(final ProcessorManager manager) {
        this.att = (AttributesFloorFieldProcessor) this.getAttributes();
        this.targetId = att.getTargetId();
    }

    @Override
    public AttributesProcessor getAttributes() {
        if(super.getAttributes() == null) {
            setAttributes(new AttributesFloorFieldProcessor());
        }

        return super.getAttributes();
    }
}
