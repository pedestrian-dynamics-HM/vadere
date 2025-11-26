package org.vadere.geometry.mesh;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.*;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.elements.AFace;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.elements.AHalfEdge;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.elements.AVertex;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.triangles.ATriangleMeshBuilder;
import org.vadere.meshing.mesh.inter.mesh.IMesh;
import org.vadere.meshing.mesh.inter.mesh.IMeshWithDataStorage;
import org.vadere.meshing.mesh.inter.mesh.builder.ITriangleMeshBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static  org.junit.jupiter.api.Assertions.assertEquals;


public class TestAFace {

    /**
     * Building a geometry containing 2 triangles
     * xyz and wyx
     */
    private IMesh<AVertex, AHalfEdge, AFace> mesh;
    private AFace face1;
    private AFace face2;
    private AFace border;
    private AVertex x, y, z, w;
    private AHalfEdge zx ;
    private AHalfEdge xy;
    private AHalfEdge yz;

    private AHalfEdge wx;
    private AHalfEdge xz;
    private AHalfEdge yw;
    private AHalfEdge zy;

    @BeforeEach
    public void setUp() throws Exception {
        ITriangleMeshBuilder<AVertex, AHalfEdge, AFace> meshBuilder = new ATriangleMeshBuilder();
        mesh = meshBuilder.getMesh();
        border = meshBuilder.faces().createAndInsertHole();

        // first triangle xyz
        face1 = meshBuilder.faces().createAndInsert();
        x = meshBuilder.vertices().createAndInsert(0, 0);
        y = meshBuilder.vertices().createAndInsert(3, 0);
        z = meshBuilder.vertices().createAndInsert(1.5,3.0);

        zx = meshBuilder.edges().createAndInsert(x, face1);
        meshBuilder.vertices().setEdge(x, zx);
        xy = meshBuilder.edges().createAndInsert(y, face1);
        meshBuilder.vertices().setEdge(y, xy);
        yz = meshBuilder.edges().createAndInsert(z, face1);
        meshBuilder.vertices().setEdge(z, yz);

        meshBuilder.edges().setNext(zx, xy);
        meshBuilder.edges().setNext(xy, yz);
        meshBuilder.edges().setNext(yz, zx);

        meshBuilder.faces().setEdge(face1, xy);

        // second triangle yxw
        face2 = meshBuilder.faces().createAndInsert();
        w = meshBuilder.vertices().createAndInsert(1.5,-1.5);

        AHalfEdge yx = meshBuilder.edges().createAndInsert(x, face2);
        AHalfEdge xw = meshBuilder.edges().createAndInsert(w, face2);
        AHalfEdge wy = meshBuilder.edges().createAndInsert(y, face2);

        meshBuilder.edges().setNext(yx, xw);
        meshBuilder.edges().setNext(xw, wy);
        meshBuilder.edges().setNext(wy, yx);

        meshBuilder.faces().setEdge(face2, yx);

        meshBuilder.edges().setTwin(xy, yx);

        // border twins
        zy = meshBuilder.edges().createAndInsert(y, border);
        xz = meshBuilder.edges().createAndInsert(z, border);

        meshBuilder.edges().setTwin(yz, zy);
        meshBuilder.edges().setTwin(zx, xz);

        wx = meshBuilder.edges().createAndInsert(x, border);
        yw = meshBuilder.edges().createAndInsert(w, border);
        meshBuilder.vertices().setEdge(w, yw);

        meshBuilder.faces().setEdge(border, wx);
        meshBuilder.edges().setTwin(xw, wx);
        meshBuilder.edges().setTwin(wy, yw);


        meshBuilder.edges().setNext(zy, yw);
        meshBuilder.edges().setNext(yw, wx);
        meshBuilder.edges().setNext(wx, xz);
        meshBuilder.edges().setNext(xz, zy);
    }

    @Test
    public void testFaceIterator() {
        mesh.faces().getAdjacentFacesIt(xy);
        List<AFace> incidentFaces = mesh.faces().getAdjacentOf(xy);
        assertEquals(incidentFaces.size(), 3);
    }


    @Test
    public void testPointIterator() {
        assertEquals(new ArrayList(Arrays.asList(y, z, x)), mesh.vertices().getAllOf(face1));
    }

    @Test
    public void testEdgeOfVertex() {
        mesh.edges().stream().forEach(
                edge -> assertEquals(
                        mesh.vertices().getEndOf(edge),
                        mesh.vertices().getEndOf(mesh.edges().getOf(mesh.vertices().getEndOf(edge)))));
    }

    @Test
    public void testEdgeIterator() {
        List<AVertex> adjacentVertices = mesh.vertices().getAdjacentVertices(zx);
        Set<AVertex> neighbours = new HashSet<>(adjacentVertices);
        Set<AVertex> expectedNeighbours = new HashSet<>();
        expectedNeighbours.add(z);
        expectedNeighbours.add(y);
        expectedNeighbours.add(w);
        assertEquals(expectedNeighbours, neighbours);
    }
}
