package org.vadere.meshing.mesh.gen;

import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The A pointer based version of {@link IVertex}.
 *
 * @author Benedikt Zoennchen
 * @param <P> the type of the points (containers)
 */
public class PVertex<P extends IPoint> implements IVertex<P> {

	private Lock lock;
	private P point;
	private PVertex<P> down;
	private PHalfEdge<P> halfEdge;
	private boolean destroyed;

	public PVertex(final P point) {
		this.point = point;
		this.destroyed = false;
		this.down = null;
		this.lock = new ReentrantLock();
	}

	public void setPoint(P point) {
		this.point = point;
	}

	@Override
	public P getPoint() {
		return point;
	}

	public PHalfEdge<P> getEdge() {
		return halfEdge;
	}

	public void setEdge(final PHalfEdge<P> halfEdge) {
		this.halfEdge = halfEdge;
	}

	public PVertex<P> getDown() {
		return down;
	}

	public void setDown(final PVertex<P> down) {
		this.down = down;
	}

	public boolean isDestroyed() {
		return destroyed;
	}

	public void destroy() {
		destroyed = true;
	}

	public Lock getLock() {
		return lock;
	}

	@Override
	public String toString() {
		return point.toString();
	}


	// TODO: make it protected since it is a non-deep copy. Therefore the IVertex should maybe not be a IPoint!?

	@Override
	public IPoint add(double x, double y) {
		return point.add(x, y);
	}

	@Override
	public IPoint norm(double len) {
		return point.norm(len);
	}

	@Override
	public double distanceSq(IPoint other) {
		return point.distanceSq(other);
	}

	@Override
	public double distanceSq(double x, double y) {
		return point.distanceSq(x, y);
	}

	/**
	 * Returns a deep clone of the vertex.
	 *
	 * @return a deep clone of the vertex.
	 */
    @Override
    public PVertex<P> clone() {
	    try {
		    PVertex<P> clone = (PVertex<P>)super.clone();
		    clone.point = (P)point.clone();
		    clone.lock = new ReentrantLock();
		    return clone;
	    } catch (CloneNotSupportedException e) {
		    throw new InternalError(e.getMessage());
	    }
    }
}
