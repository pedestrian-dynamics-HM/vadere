package org.vadere.meshing.mesh.gen;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An array-based implementation of {@link IVertex}.
 *
 * @author Benedikt Zoennchen
 */
public class AVertex<P extends IPoint> implements IVertex<P>, Cloneable {

	/**
	 * A lock for flipping edges in parallel
	 */
	private final Lock lock;

	/**
	 * The point of the vertex
	 */
    private P point;

	/**
	 * The array-index of the down vertex. This is only required by if one uses the {@link DelaunayHierarchy} as the
	 * point location algorithm of the mesh / the triangulation.
	 */
	private int down;

	/**
	 * The array-index of the half-edge which ends in this vertex.
	 */
    private int halfEdge;

	/**
	 * The array-index of this vertex.
	 */
	private int id;

	/**
	 * Indicates that the vertex is destroyed and can be removed from the array-based data structure.
	 */
    private boolean destroyed;

	/**
	 * Default constructor.
	 *
	 * @param id        the array-index of this vertex
	 * @param point     the point / container of this vertex
	 */
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
	 * Sets the array-index of this vertex. Note that this method should
	 * only be called by the garbage collector in {@link AMesh} which
	 * adjust indices to remove destroyed base elements.
	 *
	 * @param id the new array-index of this face
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
