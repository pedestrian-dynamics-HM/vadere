package org.vadere.meshing.mesh.triangulation.improver;

import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PFace;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PHalfEdge;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PVertex;

/**
 *
 * @author Benedikt Zoennchen
 *
 */
public interface IPMeshImprover extends IMeshImprover<PVertex, PHalfEdge, PFace> {}
