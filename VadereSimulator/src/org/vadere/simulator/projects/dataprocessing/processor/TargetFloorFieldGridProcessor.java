package org.vadere.simulator.projects.dataprocessing.processor;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.models.MainModel;
import org.vadere.simulator.models.potential.PotentialFieldModel;
import org.vadere.simulator.models.potential.fields.IPotentialFieldTarget;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepRowKey;
import org.vadere.state.attributes.processor.AttributesFloorFieldProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.data.FloorFieldGridRow;
import org.vadere.util.geometry.shapes.VPoint;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TargetFloorFieldGridProcessor extends DataProcessor<TimestepRowKey, FloorFieldGridRow> {
	private static Logger logger = LogManager.getLogger(TargetFloorFieldProcessor.class);
	private AttributesFloorFieldProcessor att;
	private List<Integer> targetIds;
	private boolean hasOnceProcessed = false;

	public TargetFloorFieldGridProcessor() {
		super("potential");
		setAttributes(new AttributesFloorFieldProcessor());
	}

	@Override
	protected void doUpdate(final SimulationState state) {
		Optional<MainModel> optMainModel = state.getMainModel();

		if(optMainModel.isPresent() && optMainModel.get() instanceof PotentialFieldModel) {
			IPotentialFieldTarget pft = ((PotentialFieldModel) optMainModel.get()).getPotentialFieldTarget();
			Rectangle.Double bound = state.getTopography().getBounds();

			/**
			 * If the floor field is static we do not have to process it twice.
			 */
			if(!hasOnceProcessed || pft.needsUpdate()) {
				/**
				 * We assume that all pedestrian navigate to a specific target using the same floor field. This is not always true.
				 * For example in the cooperative and competitive queueing model, pedestrians use different floor fields.
				 */
				Optional<Pedestrian> optPed = state.getTopography().getPedestrianDynamicElements().getElements().stream().findAny();

				if(optPed.isPresent()) {
					int row = 0;
					for (double y = bound.y; y < bound.y + bound.height; y += att.getResolution()) {
						FloorFieldGridRow floorFieldGridRow = new FloorFieldGridRow((int)Math.floor(bound.width / att.getResolution()));
						int col = 0;
						for (double x = bound.x; x < bound.x + bound.width; x += att.getResolution()) {
							floorFieldGridRow.setValue(col++, pft.getPotential(new VPoint(x, y), optPed.get()));
						}
						this.putValue(new TimestepRowKey(state.getStep(), row++), floorFieldGridRow);
					}
					hasOnceProcessed = true;
				}
			}

		}
		else {
			logger.warn("could not process, main model is missing or is not the instance of " + PotentialFieldModel.class.getName());
		}
	}

	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);
		this.att = (AttributesFloorFieldProcessor) this.getAttributes();
		this.targetIds = new ArrayList<>();
		this.targetIds.add(att.getTargetId());
		this.hasOnceProcessed = false;
	}

    @Override
    public String[] toStrings(TimestepRowKey key) {
        return this.getValue(key).toStrings();
    }

    @Override
    public AttributesProcessor getAttributes() {
        if(super.getAttributes() == null) {
            setAttributes(new AttributesFloorFieldProcessor());
        }

        return super.getAttributes();
    }
}
