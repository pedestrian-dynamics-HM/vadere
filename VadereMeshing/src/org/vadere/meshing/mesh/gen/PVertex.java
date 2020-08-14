package org.vadere.meshing.mesh.gen;

import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The A pointer based version of {@link IVertex}.
 *
 * @author Benedikt Zoennchen
 */
public class PVertex implements IVertex {

	private Map<String, Object> propertyElements;
	private Lock lock;
	private IPoint point;
	private PVertex down;
	private PHalfEdge halfEdge;
	private boolean destroyed;

	public PVertex(final IPoint point) {
		this.point = point;
		this.destroyed = false;
		this.down = null;
		this.lock = new ReentrantLock();
		this.propertyElements = new HashMap<>();
	}

	public void setPoint(final IPoint point) {
		this.point = point;
	}

	@Override
	public IPoint getPoint() {
		return point;
	}

	public PHalfEdge getEdge() {
		return halfEdge;
	}

	public void setEdge(final PHalfEdge halfEdge) {
		this.halfEdge = halfEdge;
	}

	public PVertex getDown() {
		return down;
	}

	public void setDown(final PVertex down) {
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
		if(destroyed) {
			return "destroyed vertex";
		}
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
    public PVertex clone() {
	    try {
		    PVertex clone = (PVertex)super.clone();
		    clone.point = point.clone();
		    clone.lock = new ReentrantLock();
		    return clone;
	    } catch (CloneNotSupportedException e) {
		    throw new InternalError(e.getMessage());
	    }
    }

	<T> void setData(final String name, T data) {
		propertyElements.put(name, data);
	}

	<T> T getData(final String name, Class<T> clazz) {
		if (propertyElements.containsKey(name)) {
			return clazz.cast(propertyElements.get(name));
		} else {
			return null;
		}
	}
}
