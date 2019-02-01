package org.vadere.state.attributes.processor;

import org.vadere.util.geometry.shapes.VPoint;

public class AttributesPedestrianLineCrossProcessor extends AttributesProcessor {
	private VPoint p1 = new VPoint(0,0);
	private VPoint p2 = new VPoint(1, 0);

	public VPoint getP1() {
		return p1;
	}

	public VPoint getP2() {
		return p2;
	}

	public void setP1(VPoint p1) {
		checkSealed();
		this.p1 = p1;
	}

	public void setP2(VPoint p2) {
		checkSealed();
		this.p2 = p2;
	}
}
