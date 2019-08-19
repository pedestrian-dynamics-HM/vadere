package org.vadere.meshing.mesh.triangulation.improver;

import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.util.geometry.shapes.IPoint;

/**
 *
 * @author Benedikt Zoennchen
 *
 */
public interface IPMeshImprover extends IMeshImprover<PVertex, PHalfEdge, PFace> {}
