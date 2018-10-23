package org.vadere.meshing.mesh.gen;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IPolyConnectivity;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;

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
