package org.vadere.meshing.mesh.gen;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IPolyConnectivity;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.Iterator;

public class PolyConnectivity<V extends IVertex, E extends IHalfEdge, F extends IFace> implements IPolyConnectivity<V, E, F> {

	private final IMesh<V, E, F> mesh;

	public PolyConnectivity(@NotNull final IMesh<V, E, F> mesh) {
		this.mesh = mesh;
	}

	@Override
	public IMesh<V, E, F> getMesh() {
		return mesh;
	}

	@NotNull
	@Override
	public Iterator<F> iterator() {
		return mesh.getFaces().iterator();
	}
}
