package org.vadere.meshing.mesh.gen;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * TODO: remove all object references!
 *
 * An array-based implementation of {@link IVertex}.
 *
 *
 * @author Benedikt Zoennchen
 */
public class AVertex implements IVertex, Cloneable {

	/**
	 * A lock for flipping edges in parallel
	 */
	private final Lock lock;

	/**
	 * The point of the vertex
	 */
    private IPoint point;

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
	public AVertex(final int id, final IPoint point) {
		this.point = point;
		this.id = id;
		this.lock = new ReentrantLock();
		this.destroyed = false;
		this.halfEdge = -1;
	}

	protected void setPoint(@NotNull final IPoint point) {
		this.point = point;
	}

	@Override
	public IPoint getPoint() {
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
	void setId(final int id) {
		this.id = id;
	}

	public boolean isDestroyed() {
		return destroyed;
	}


	public void destroy() {
		destroyed = true;
	}

	Lock getLock() {
		return lock;
	}

	@Override
	public String toString() {
		return id+"";
	}

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

	@Override
    public AVertex clone() {
        try {
            AVertex clone = (AVertex)super.clone();
            clone.point = point.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.getMessage());
        }
    }
}
