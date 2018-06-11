package org.vadere.util.geometry.mesh.gen;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Benedikt Zoennchen
 */
public class AVertex<P extends IPoint> implements IVertex<P>, Cloneable {
    private final Lock lock;
    private P point;
    private int down;
    private int halfEdge;
    private int id;
    private boolean destroyed;


	public AVertex(@NotNull final int id, @NotNull final P point) {
		this.point = point;
		this.id = id;
		this.lock = new ReentrantLock();
		this.destroyed = false;
	}

	protected void setPoint(@NotNull final P point) {
		this.point = point;
	}

	@Override
	public P getPoint() {
		return point;
	}

	public int getEdge() {
		return halfEdge;
	}

	public void setEdge(final int halfEdge) {
		this.halfEdge = halfEdge;
	}

	public int getDown() {
		return down;
	}

	public void setDown(final int down) {
		this.down = down;
	}

	public int getId() {
	    return id;
    }

	/**
	 * This method should only be called by the garbage collector in AMesh.
	 * @param id
	 */
	void setId(@NotNull final int id) {
		this.id = id;
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
		return id+"";
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

	@Override
    public AVertex<P> clone() {
        try {
            AVertex<P> clone = (AVertex<P>)super.clone();
            clone.point = (P)point.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.getMessage());
        }
    }
}
