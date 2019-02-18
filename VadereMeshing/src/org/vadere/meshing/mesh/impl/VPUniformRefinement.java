package org.vadere.meshing.mesh.impl;

import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.inter.ITriangulationSupplier;
import org.vadere.meshing.mesh.triangulation.triangulator.gen.GenUniformRefinementTriangulator;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.meshing.mesh.triangulation.IEdgeLengthFunction;

import java.util.Collection;

/**
 * @author Benedikt Zoennchen
 */
public class VPUniformRefinement extends GenUniformRefinementTriangulator<VPoint, Object, Object, PVertex<VPoint, Object, Object>, PHalfEdge<VPoint, Object, Object>, PFace<VPoint, Object, Object>> {
	public VPUniformRefinement(
			final ITriangulationSupplier<VPoint, Object, Object, PVertex<VPoint, Object, Object>, PHalfEdge<VPoint, Object, Object>, PFace<VPoint, Object, Object>> supplier,
			final VRectangle bound,
			final Collection<VShape> boundary,
			final IEdgeLengthFunction lenFunc) {
		super(supplier, bound, boundary, lenFunc, p -> -1.0);
	}
}
