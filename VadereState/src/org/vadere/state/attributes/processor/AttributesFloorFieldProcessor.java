package org.vadere.state.attributes.processor;

public class AttributesFloorFieldProcessor extends AttributesProcessor {
    private int targetId;
	private double resolution;

    public int getTargetId() {
        return this.targetId;
    }

	public double getResolution() {
		return resolution;
	}

	public void setTargetId(int targetId) {
    	checkSealed();
		this.targetId = targetId;
	}

	public void setResolution(double resolution) {
    	checkSealed();
		this.resolution = resolution;
	}
}
