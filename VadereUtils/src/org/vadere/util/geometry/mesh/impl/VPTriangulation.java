package org.vadere.util.geometry.mesh.impl;

import org.vadere.util.triangulation.IncrementalTriangulation;
import org.vadere.util.geometry.mesh.gen.PFace;
import org.vadere.util.geometry.mesh.gen.PHalfEdge;
import org.vadere.util.geometry.mesh.gen.PVertex;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;

/**
 * @author Benedikt Zoennchen
 *
 * Note: Use factory i.e. ITriangulation for the creation!
 */
public class VPTriangulation extends IncrementalTriangulation<VPoint, PVertex<VPoint>, PHalfEdge<VPoint>, PFace<VPoint>> {

	public VPTriangulation(final Collection<VPoint> points, final Predicate<PHalfEdge<VPoint>> illegalPredicate) {
		super(points, illegalPredicate);
	}

	public VPTriangulation(final Set<VPoint> points) {
		super(points);
	}

	public VPTriangulation(final VRectangle bound,
	                       final Predicate<PHalfEdge<VPoint>> illegalPredicate){
		super(bound, illegalPredicate);
	}

	public VPTriangulation(final VRectangle bound) {
		super(bound);
	}
}
