package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepKey;
import org.vadere.state.attributes.processor.AttributesProcessor;
import org.vadere.state.attributes.processor.AttributesQueueWidthProcessor;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 
 * @author Marion Gödel
 *
 */
@DataProcessorClass()
public class QueueWidthProcessor extends DataProcessor<TimestepKey, Double> {

	private VPoint referencePoint;
	private double maxDist;
	private VPoint direction; /* can be either in x-direction [1, 0] or [-1, 0] or in y-direction [0, 1], [0,-1] */


	public QueueWidthProcessor() {
		super("queueWidth");
		setAttributes(new AttributesQueueWidthProcessor());
	}

	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);
		AttributesQueueWidthProcessor att = (AttributesQueueWidthProcessor) this.getAttributes();

		this.referencePoint = att.getReferencePoint();
		this.maxDist = att.getMaxDist();
		this.direction = att.getDirection();
	}

	@Override
	protected void doUpdate(final SimulationState state) {

	if( Math.abs(direction.getX()) >  0 ) {
		double minX = Math.min(referencePoint.getX(), referencePoint.getX() + direction.getX()*maxDist);
		double maxX = Math.max(referencePoint.getX(), referencePoint.getX() + direction.getX()*maxDist);

		List<Pedestrian> pedQueue = state.getTopography().getElements(Pedestrian.class).stream().
				filter(ped -> (ped.getPosition().getX() <= maxX && ped.getPosition().getX() >= minX ) )
				.collect(Collectors.toList());

		if (pedQueue.size() == 0){
			this.putValue(new TimestepKey(state.getStep()), 0.0);
		}else{
			// according to seitz-2016c
			double queue_measure = 0;
			for (int i_ped = 0; i_ped < pedQueue.size(); i_ped++){
				double delta_y = Math.abs(pedQueue.get(i_ped).getPosition().getX() - referencePoint.getX()); // X and Y are interchanged - compare supplementary material
				double delta_x = Math.abs(pedQueue.get(i_ped).getPosition().getY() - referencePoint.getY());
				queue_measure += (delta_y)/(1+delta_x);
			}
			queue_measure = queue_measure/pedQueue.size();

			this.putValue(new TimestepKey(state.getStep()), queue_measure);
		}



	}

				/* .map(ped -> new PedestrianIdKey(ped.getId()))
				.forEach(key -> this.putValue(key, state.getSimTimeInSec() - pedStartTimeProc.getValue(key))); */
	}

	/* @Override
	public void postLoop(final SimulationState state) {
		state.getTopography().getElements(Pedestrian.class).stream()
				.map(ped -> new PedestrianIdKey(ped.getId()))
				.forEach(key -> this.putValue(key, Double.POSITIVE_INFINITY));
	}*/

    @Override
    public AttributesProcessor getAttributes() {
        if(super.getAttributes() == null) {
            setAttributes(new AttributesQueueWidthProcessor());
        }

        return super.getAttributes();
    }
}
