package org.vadere.util.geometry.shapes;

import java.util.stream.Stream;

public class MLine<P extends IPoint> {

    public final P p1;
    public final P p2;
    private VPoint velocity;

    public MLine(final P p1, final P p2) {
        this.p1 = p1;
        this.p2 = p2;
        this.velocity = new VPoint(0,0);
    }

    public double length() {
        return toVLine().length();
    }

    public VLine toVLine() {
        return new VLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
    }

    public double getX1() {
        return p1.getX();
    }

    public double getY1() {
        return p1.getY();
    }

    public double getX2() {
        return p2.getX();
    }

    public double getY2() {
        return p2.getY();
    }

    public P getP1() {
        return p1;
    }

    public P getP2() {
        return p2;
    }

    public void setVelocity(final VPoint velocity) {
        this.velocity = velocity;
    }

    public VPoint getVelocity() {
        return velocity;
    }

    public VPoint midPoint() {
        return new VPoint((p1.getX() + p2.getX()) / 2, (p1.getY() + p2.getY()) / 2);
    }

    public Stream<P> streamPoints() {
        return Stream.of(p1, p2);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MLine that = (MLine) o;

        return (p1.equals(that.p1) && p2.equals(that.p2)) || (p2.equals(that.p1) && p1.equals(that.p2));
    }

    @Override
    public int hashCode() {
        int result = p1.hashCode();
        result = 31 * result * p2.hashCode();
        return result;
    }
}
