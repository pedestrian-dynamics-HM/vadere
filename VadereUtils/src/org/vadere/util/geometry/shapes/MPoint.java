package org.vadere.util.geometry.shapes;

public class MPoint implements org.vadere.util.geometry.shapes.IPoint, Cloneable{

    private VPoint point;
    private int hashCode = -1;

    public MPoint(final double x, final double y){
        this.point = new VPoint(x, y);
    }

    public VPoint toVPoint() {
        return new VPoint(getX(), getY());
    }

    @Override
    public double getX() {
        return point.getX();
    }

    @Override
    public double getY() {
        return point.getY();
    }

    public void set(double x, double y) {
        point = new VPoint(x, y);
    }

    @Override
    public MPoint add(final IPoint point) {
        this.point = this.point.add(point);
        hashCode = -1;
        return this;
    }

	@Override
	public IPoint add(double x, double y) {
		this.point = this.point.add(x, y);
		hashCode = -1;
		return this;
	}

	@Override
    public MPoint addPrecise(final IPoint point) {
        this.point = this.point.addPrecise(point);
	    hashCode = -1;
        return this;
    }

    @Override
    public MPoint subtract(IPoint point) {
        this.point = this.point.subtract(point);
	    hashCode = -1;
        return this;
    }

    @Override
    public MPoint multiply(IPoint point) {
        this.point = this.point.multiply(point);
	    hashCode = -1;
        return this;
    }

    @Override
    public MPoint scalarMultiply(double factor) {
        this.point = this.point.scalarMultiply(factor);
	    hashCode = -1;
        return this;
    }

    @Override
    public MPoint rotate(double radAngle) {
        this.point = this.point.rotate(radAngle);
	    hashCode = -1;
        return this;
    }

    @Override
    public double scalarProduct(IPoint point) {
        return this.point.scalarProduct(point);
    }

    @Override
    public MPoint norm() {
        this.point = this.point.norm();
	    hashCode = -1;
        return this;
    }

	@Override
	public IPoint norm(double len) {
		return point.norm(len);
	}

	@Override
    public MPoint normZeroSafe() {
        this.point = this.point.normZeroSafe();
		hashCode = -1;
        return this;
    }

    @Override
    public int hashCode() {
    	if(hashCode == -1) {
		    // hashCode of java.awt.geom.Point2D
		    long bits = java.lang.Double.doubleToLongBits(getX());
		    bits ^= java.lang.Double.doubleToLongBits(getY()) * 31;
		    hashCode = (((int) bits) ^ ((int) (bits >> 32)));
	    }
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof IPoint))
            return false;

        IPoint other = (IPoint) obj;

        if (this.getX() != other.getX())
            return false;
        if (this.getY() != other.getY())
            return false;

        return true;
    }


    @Override
    public String toString() {
        return point.toString();
    }

    @Override
    public double distance(IPoint other) {
        return point.distance(other);
    }

    @Override
    public double distance(double x, double y) {
        return point.distance(x, y);
    }

	@Override
	public double distanceSq(IPoint other) {
		return point.distanceSq(other);
	}

	@Override
	public double distanceSq(double x, double y) {
		return point.distanceSq(x, y);
	}

	@Override
    public double distanceToOrigin() {
        return this.point.distanceToOrigin();
    }

    @Override
    public MPoint clone() {
        try {
            MPoint clone = (MPoint)super.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
