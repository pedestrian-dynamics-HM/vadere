package org.vadere.util.geometry.mesh.impl;

import org.vadere.util.geometry.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;

/**
 * @author Benedikt Zoennchen
 * @param <P>
 */
public class PVertex<P extends IPoint> implements IVertex<P> {

	private final P point;
	private PVertex<P> down;
	private PHalfEdge<P> halfEdge;

	public PVertex(final P point) {
		this.point = point;
		this.down = null;
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

	@Override
	public int hashCode() {
		return point.hashCode();
	}

	@Override
	public String toString() {
		return point.toString();
	}
}
