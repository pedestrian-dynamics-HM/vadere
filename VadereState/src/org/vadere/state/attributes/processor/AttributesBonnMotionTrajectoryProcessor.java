package org.vadere.state.attributes.processor;

import org.vadere.util.geometry.shapes.VPoint;

public class AttributesBonnMotionTrajectoryProcessor extends AttributesProcessor {

	// use the data from this processor to minimize memory footprint.
	private int pedestrianPositionProcessorId;

	// use scale (1.0, -1.0) to flip the y-axis. This is useful if some other tool has the
	// coordinate origin in the top right corner.
	private VPoint scale = new VPoint(1.0, 1.0);

	// use translate(0.0, Y) to move the coordinates up Y units.This is useful if some other tool has the
	// coordinate origin in the top right corner.
	private VPoint translate = new VPoint(0.0, 0.0);


	public int getPedestrianPositionProcessorId() {
		return pedestrianPositionProcessorId;
	}

	public void setPedestrianPositionProcessorId(int pedestrianPositionProcessorId) {
		checkSealed();
		this.pedestrianPositionProcessorId = pedestrianPositionProcessorId;
	}

	public VPoint getScale() {
		return scale;
	}

	public void setScale(VPoint scale) {
		this.scale = scale;
	}

	public VPoint getTranslate() {
		return translate;
	}

	public void setTranslate(VPoint translate) {
		this.translate = translate;
	}
}
