package org.vadere.geometry.mesh.gen;

import org.jetbrains.annotations.NotNull;
import org.vadere.geometry.mesh.inter.IFace;
import org.vadere.geometry.mesh.inter.IHalfEdge;
import org.vadere.geometry.mesh.inter.IMesh;
import org.vadere.geometry.mesh.inter.IPolyConnectivity;
import org.vadere.geometry.mesh.inter.IVertex;
import org.vadere.geometry.shapes.IPoint;

import java.util.Iterator;

public class PolyConnectivity<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> implements IPolyConnectivity<P, V, E, F> {

	private final IMesh<P, V, E, F> mesh;

	public PolyConnectivity(@NotNull final IMesh<P, V, E, F> mesh) {
		this.mesh = mesh;
	}

	@Override
	public IMesh<P, V, E, F> getMesh() {
		return mesh;
	}

	@NotNull
	@Override
	public Iterator<F> iterator() {
		return mesh.getFaces().iterator();
	}
}
