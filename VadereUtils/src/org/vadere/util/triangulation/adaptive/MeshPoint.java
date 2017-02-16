package org.vadere.util.triangulation.adaptive;

import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.MPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.potential.PathFindingTag;
import org.vadere.util.potential.calculators.PotentialPoint;

public class MeshPoint extends MPoint implements PotentialPoint {
	private boolean fixPoint;
	private IPoint velocity;
	private double maxTraveldistance;
	private IPoint lastPosition;
	private double potential;
	private PathFindingTag tag;

	public MeshPoint(final double x, final double y, boolean fixPoint){
		super(x, y);
		this.fixPoint = fixPoint;
		this.potential = Double.MAX_VALUE;
		this.velocity = new VPoint(0,0);
		this.tag = PathFindingTag.Undefined;
		this.maxTraveldistance = 0;
		this.lastPosition = new VPoint(x, y);
	}

	public MeshPoint(final IPoint point, boolean fixPoint){
		this(point.getX(), point.getY(), fixPoint);
	}

	public MeshPoint(final IPoint point, boolean fixPoint){
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

	public void setVelocity(final VPoint velocity) {
		this.velocity = velocity;
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
	public double getPotential() {
		return potential;
	}

	@Override
	public void setPotential(double potential) {
		this.potential = potential;
	}


	@Override
	public void setPathFindingTag(final PathFindingTag tag) {
		this.tag = tag;
	}

	@Override
	public PathFindingTag getPathFindingTag() {
		return tag;
	}

	@Override
	public String toString() {
		return super.toString() + "/" + tag + "/" + potential;
	}
}
