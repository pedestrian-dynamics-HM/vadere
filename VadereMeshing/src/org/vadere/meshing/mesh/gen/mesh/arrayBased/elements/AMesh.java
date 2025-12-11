package org.vadere.meshing.mesh.gen.mesh.arrayBased.elements;
import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.mesh.*;
import org.vadere.util.logging.Logger;


/**
 * An array-based implementation of {@link IMesh}.
 *
 * Original author: Benedikt Zoennchen
 * Refactored by: Hayato Hess
 */
public class AMesh extends ArrayBasedMesh<AMeshVertices, AMeshEdges, AMeshFaces> implements IMesh<AVertex, AHalfEdge, AFace>{
	final static Logger log = Logger.getLogger(AMesh.class);

	public AMesh() {
		super(  // parent is null as it will be set in super ctor
				new AMeshVertices(null),
				new AMeshEdges(null),
				new AMeshFaces(null));
	}

	/**
	 * Copy constructor
	 */
	public AMesh(AMesh meshToCopy) {
		super(meshToCopy,
				// parent is null as it will be set in super ctor
				new AMeshVertices(null, meshToCopy.vertices()),
				new AMeshEdges(null, meshToCopy.edges()),
				new AMeshFaces(null, meshToCopy.faces()));
	}

	@Override
	public AMesh copy() {
		return new AMesh(this);
	}
}