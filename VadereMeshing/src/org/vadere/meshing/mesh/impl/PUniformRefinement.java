package org.vadere.meshing.mesh.impl;

import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PFace;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PHalfEdge;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PVertex;
import org.vadere.meshing.mesh.inter.ITriangulationSupplier;
import org.vadere.meshing.mesh.triangulation.triangulator.gen.GenUniformRefinementTriangulator;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.meshing.mesh.triangulation.edgeLengthFunctions.IEdgeLengthFunction;

import java.util.Collection;

/**
 * @author Benedikt Zoennchen
 */
public class PUniformRefinement extends GenUniformRefinementTriangulator<PVertex, PHalfEdge, PFace> {
	public PUniformRefinement(
			final ITriangulationSupplier<PVertex, PHalfEdge, PFace> supplier,
			final VRectangle bound,
			final Collection<VShape> boundary,
			final IEdgeLengthFunction lenFunc) {
		super(supplier, bound, boundary, lenFunc, p -> -1.0);
	}
}
