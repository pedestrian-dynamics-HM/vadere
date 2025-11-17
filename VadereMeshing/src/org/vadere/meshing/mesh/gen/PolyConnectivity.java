package org.vadere.meshing.mesh.gen;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.mesh.*;
import org.vadere.meshing.mesh.inter.IPolyConnectivity;
import org.vadere.meshing.mesh.inter.mesh.data.IMeshDataStorage;

import java.util.Iterator;

public class PolyConnectivity<V extends IVertex, E extends IHalfEdge, F extends IFace> implements IPolyConnectivity<V, E, F> {

	private final IMeshWithDataStorage<V, E, F> meshWithDataStorage;

	public PolyConnectivity(@NotNull final IMeshWithDataStorage<V, E, F> meshWithDataStorage) {
		this.meshWithDataStorage = meshWithDataStorage;
	}

	@Override
	public IMeshWithDataStorage<V, E, F> getMeshWithDataStorage() {
		return meshWithDataStorage;
	}

	@Override
	public IMeshDataStorage<V, E, F> getMeshDataStorage() {
		return meshWithDataStorage.getDataStorage();
	}

	@NotNull
	@Override
	public Iterator<F> iterator() {
		return meshWithDataStorage.getMesh().getFaces().iterator();
	}
}
