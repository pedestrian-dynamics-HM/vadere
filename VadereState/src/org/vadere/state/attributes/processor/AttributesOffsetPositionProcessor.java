package org.vadere.state.attributes.processor;

import org.vadere.util.geometry.shapes.VPoint;

public class AttributesOffsetPositionProcessor extends AttributesProcessor {
	private VPoint offset = new VPoint(0,0);


	public VPoint getOffset() {
		return offset;
	}


	public void setOffset(VPoint offset) {
		checkSealed();
		this.offset = offset;
	}

}
