package org.vadere.meshing.mesh.triangulation.improver;

import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.util.geometry.shapes.IPoint;

/**
 *
 * @author Benedikt Zoennchen
 *
 * @param <P> the type of the points (containers)
 */
public interface IPMeshImprover<P extends IPoint> extends IMeshImprover<P, Object, Object, PVertex<P, Object, Object>, PHalfEdge<P, Object, Object>, PFace<P, Object, Object>> {}
