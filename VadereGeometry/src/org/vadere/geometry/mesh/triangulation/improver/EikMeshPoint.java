package org.vadere.geometry.mesh.triangulation.improver;

import org.vadere.geometry.shapes.IPoint;
import org.vadere.geometry.shapes.MPoint;
import org.vadere.geometry.shapes.VPoint;

public class EikMeshPoint extends MPoint implements Cloneable {
	private boolean fixPoint;
	private IPoint velocity;
	private double absoluteForce;
	private double maxTraveldistance;
	private IPoint lastPosition;

	public EikMeshPoint(final double x, final double y, boolean fixPoint){
		super(x, y);
		this.fixPoint = fixPoint;
		this.velocity = new VPoint(0,0);
		this.maxTraveldistance = 0;
		this.lastPosition = new VPoint(x, y);
	}

	public void setFixPoint(boolean fixPoint) {
		this.fixPoint = fixPoint;
	}

	public EikMeshPoint(final IPoint point, boolean fixPoint){
		this(point.getX(), point.getY(), fixPoint);
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

	public double getAbsoluteForce() {
		return absoluteForce;
	}

	public void increaseAbsoluteForce(double force) {
		this.absoluteForce += force;
	}

	public void decreaseAbsoluteForce(double force) {
		this.absoluteForce -= force;
	}

	public void setVelocity(final VPoint velocity) {
		this.velocity = velocity;
	}

	public void setAbsoluteForce(double force) {
		this.absoluteForce = force;
	}

	public IPoint getVelocity() {
		return velocity;
	}

	public boolean isFixPoint() {
		return fixPoint;
	}

	public void setLastPosition(final IPoint lastPosition) {
		this.lastPosition = lastPosition;
	}

	public IPoint getLastPosition() {
		return lastPosition;
	}

	public void setMaxTraveldistance(double maxTraveldistance) {
		this.maxTraveldistance = maxTraveldistance;
	}

	public double getMaxTraveldistance() {
		return maxTraveldistance;
	}

	@Override
	public String toString() {
		return super.toString() + "/" + fixPoint + "/" + velocity;
	}

	@Override
	public EikMeshPoint clone() {
		EikMeshPoint clone = (EikMeshPoint) super.clone();
		clone.lastPosition = lastPosition.clone();
		clone.velocity = velocity.clone();
		return clone;
	}
}
