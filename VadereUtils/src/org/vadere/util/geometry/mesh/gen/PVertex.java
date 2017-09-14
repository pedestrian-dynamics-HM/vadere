package org.vadere.util.geometry.mesh.gen;

import org.vadere.util.geometry.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Benedikt Zoennchen
 * @param <P>
 */
public class PVertex<P extends IPoint> implements IVertex<P> {

	private final Lock lock;
	private final P point;
	private PVertex<P> down;
	private PHalfEdge<P> halfEdge;
	private boolean destroyed;

	public PVertex(final P point) {
		this.point = point;
		this.destroyed = false;
		this.down = null;
		this.lock = new ReentrantLock();
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

	@Override
	public boolean equals(Object obj) {
		if(obj == null) {
			return false;
		}

		if(obj.getClass() != this.getClass()) {
			return false;
		}

		return point.equals(((PVertex<P>)obj).getPoint());
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
	public int hashCode() {
		return point.hashCode();
	}

	@Override
	public String toString() {
		return point.toString();
	}
}
