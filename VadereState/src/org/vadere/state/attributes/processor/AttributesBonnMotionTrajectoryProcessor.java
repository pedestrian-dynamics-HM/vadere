package org.vadere.state.attributes.processor;

import org.vadere.util.geometry.shapes.VPoint;

public class AttributesBonnMotionTrajectoryProcessor extends AttributesProcessor {

	/**
	 * use the data from this processor to minimize memory footprint.
	 * Note: applyOffset and 'origin' translation happens first!
	 */
	private int pedestrianPositionProcessorId;

	/** use scale (1.0, -1.0) to flip the y-axis. This is useful if some other tool has the
	 * coordinate origin in the top right corner.
	 */ 
	private VPoint scale = new VPoint(1.0, 1.0);

	/** use translate(0.0, Y) to move the coordinates up Y units.This is useful if some other tool has the
	  * coordinate origin in the top right corner.
	  * Note: applyOffset and 'origin' translation happens first!
	  */
	private VPoint translate = new VPoint(0.0, 0.0);

	/**
	 * apply simulation offset to the position logged in this data processor.
	 * If no reference coordinate system is set this will be skipped quietly
	 */
	private boolean applyOffset = false;

	/**
	 * set origin location to one of ["lower left", "upper left"]
	 * "lower left" is default and identical with the Vadere coordinate system.
	 * 
	 */
	private String origin = "lower left";


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

	public void setApplyOffset(boolean val){
		this.applyOffset = val;
	}

	public boolean isApplyOffset(){
		return this.applyOffset;
	}

	public String getOrigin() {
		return origin;
	}

	public void setOrigin(String origin) {
		this.origin = origin;
	}
}
