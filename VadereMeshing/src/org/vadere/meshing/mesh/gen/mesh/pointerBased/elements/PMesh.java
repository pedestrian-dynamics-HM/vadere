package org.vadere.meshing.mesh.gen.mesh.pointerBased.elements;

import org.vadere.meshing.mesh.gen.mesh.arrayBased.elements.AMesh;
import org.vadere.meshing.mesh.inter.mesh.IMesh;
import org.vadere.util.logging.Logger;

/**
 * A Pointer based mesh implementation. More straight forward than the array based implementation {@link AMesh}
 * but mainly used for debugging and testing.
 * @author Benedikt Zoennchen
 */
public class PMesh extends PointerBasedMesh<PMeshVertices, PMeshEdges, PMeshFaces> implements IMesh<PVertex, PHalfEdge, PFace> {
	private static Logger log = Logger.getLogger(PMesh.class);

	public PMesh() {
		super(  // parent is null as it will be set in super ctor
				new PMeshVertices(null),
				new PMeshEdges(null),
				new PMeshFaces(null));
	}

	public PMesh(PMesh meshToCopy) {
		super(meshToCopy,
				// parent is null as it will be set in super ctor
				new PMeshVertices(null),
				new PMeshEdges(null),
				new PMeshFaces(null));
	}

	@Override
	public PMesh copy() {
		return new PMesh(this);
	}
}
