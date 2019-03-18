package org.vadere.meshing.mesh.gen;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.ITriConnectivity;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class BackgroundMesh<P extends IPoint, CE, CF, V extends IVertex<P>, E extends IHalfEdge<CE>, F extends IFace<CF>> {

	private ITriConnectivity<P, CE, CF, V, E, F> triConnectivity;
	private Map<Object, F> cache;

	public BackgroundMesh(@NotNull final ITriConnectivity<P, CE, CF, V, E, F> triConnectivity) {
		this.triConnectivity = triConnectivity;
		this.cache = new HashMap<>();
	}

	public Optional<F> locate(@NotNull final Object obj, @NotNull final P point) {
		return locate(obj, point.getX(), point.getY());
	}

	public Optional<F> locate(@NotNull final Object obj, final double x, final double y) {
		if(cache.containsKey(obj)) {
			F startFace = cache.get(obj);
			return triConnectivity.locateFace(x, y, startFace);
		}
		else {
			Optional<F> optionalF = triConnectivity.locateFace(x, y);
			if(optionalF.isPresent()) {
				cache.put(obj, optionalF.get());
			}
			return optionalF;
		}
	}

}
