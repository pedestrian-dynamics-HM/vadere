package org.vadere.meshing.mesh.triangulation.improver;

import org.vadere.meshing.mesh.gen.mesh.pointerBased.PFace;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.PHalfEdge;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.PVertex;

/**
 *
 * @author Benedikt Zoennchen
 *
 */
public interface IPMeshImprover extends IMeshImprover<PVertex, PHalfEdge, PFace> {}
