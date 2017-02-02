package org.vadere.util.triangulation.adaptive;

import org.vadere.util.geometry.shapes.DPoint;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.MPoint;
import org.vadere.util.geometry.shapes.VPoint;

public class MeshPoint extends MPoint {
	private boolean fixPoint;
	private IPoint velocity;
	private double potential;

	public MeshPoint(final double x, final double y, boolean fixPoint){
		super(x, y);
		this.fixPoint = fixPoint;
		this.velocity = new VPoint(0,0);
	}

	public VPoint toVPoint() {
		return new VPoint(getX(), getY());
	}

	public void increaseVelocity(final VPoint increase) {
		this.velocity = this.velocity.add(increase);
	}

	public void decreaseVelocity(final VPoint decrease) {
		this.velocity = this.velocity.subtract(decrease);
	}

	public void setVelocity(final VPoint velocity) {
		this.velocity = velocity;
	}

	public IPoint getVelocity() {
		return velocity;
	}

	public boolean isFixPoint() {
		return fixPoint;
	}
}
