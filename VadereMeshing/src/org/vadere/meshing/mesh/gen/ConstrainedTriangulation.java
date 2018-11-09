package org.vadere.meshing.mesh.gen;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.*;

/**
 * unfinished!
 * @author Benedikt Zoennchen
 */
public class ConstrainedTriangulation<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> extends IncrementalTriangulation<P, V, E, F> {

    private static Logger log = LogManager.getLogger(ConstrainedTriangulation.class);

    private Set<E> constrainedHE;
    private Collection<VLine> constrains;

    // TODO: not finished
    public ConstrainedTriangulation(final Collection<VLine> constrains, final Collection<P> points){
        super(null, null, points);
        this.constrainedHE = new HashSet<>();
        this.constrains = constrains;
    }

    @Override
    public boolean isIllegal(E edge, V p) {
        return !constrainedHE.contains(edge) && super.isIllegal(edge, p);
    }

    @Override
    public void finish() {
        // remove super triangles
        super.finish();

        // add all constrained lines
        constrains.stream().forEach(line -> addConstrain(line));
    }

    private void addConstrain(final VLine line) {
        //
        Optional<F> optFace = getPointLocator().locate(line.x1, line.y1);

        if(optFace.isPresent()) {
            F startFace = optFace.get();
            boolean startFaceIsInvalid = faceIntersectsLine(startFace, new VPoint(line.x1, line.y1), new VPoint(line.x2, line.y2));
            LinkedList<E> invalidFaces = straightGatherWalk2D(line.x2, line.y2, startFace);

            if(!startFaceIsInvalid) {
                invalidFaces.removeFirst();
            }

            if(invalidFaces.stream().anyMatch(face -> getMesh().isBoundary(face))) {
                log.error("constrained edge crosses hole or outer boundary which is not allowed");
                throw new IllegalArgumentException("constrained edge crosses hole or outer boundary which is not allowed");
            }


        }

    }

    /**
     * Returns true if and only if the line (p1, p2) intersects any edge of the face.
     * @param face
     * @param p1
     * @param p2
     * @return
     */
    private boolean faceIntersectsLine(final F face, IPoint p1, IPoint p2) {
        return getMesh().streamEdges(face).anyMatch(edge -> GeometryUtils.intersectLine(p1, p2, getMesh().getPoint(edge), getMesh().getPoint(getMesh().getPrev(edge))));
    }
}
