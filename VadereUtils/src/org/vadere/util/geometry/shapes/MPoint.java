package org.vadere.util.geometry.shapes;

public class MPoint implements IPoint{
    private VPoint point;

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
        return this;
    }

    @Override
    public MPoint addPrecise(final IPoint point) {
        this.point = this.point.addPrecise(point);
        return this;
    }

    @Override
    public MPoint subtract(IPoint point) {
        this.point = this.point.subtract(point);
        return this;
    }

    @Override
    public MPoint multiply(IPoint point) {
        this.point = this.point.multiply(point);
        return this;
    }

    @Override
    public MPoint scalarMultiply(double factor) {
        this.point = this.point.scalarMultiply(factor);
        return this;
    }

    @Override
    public MPoint rotate(double radAngle) {
        this.point = this.point.rotate(radAngle);
        return this;
    }

    @Override
    public double scalarProduct(IPoint point) {
        return this.point.scalarProduct(point);
    }

    @Override
    public MPoint norm() {
        this.point = this.point.norm();
        return this;
    }

    @Override
    public MPoint normZeroSafe() {
        this.point = this.point.normZeroSafe();
        return this;
    }

    @Override
    public int hashCode() {
        // hashCode of java.awt.geom.Point2D
        long bits = java.lang.Double.doubleToLongBits(getX());
        bits ^= java.lang.Double.doubleToLongBits(getY()) * 31;
        return (((int) bits) ^ ((int) (bits >> 32)));
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
    public double distanceToOrigin() {
        return this.point.distanceToOrigin();
    }

    @Override
    public MPoint clone() {
        return new MPoint(getX(), getY());
    }
}
