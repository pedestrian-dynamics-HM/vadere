package org.vadere.simulator.models.potential.solver;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IMeshSupplier;
import org.vadere.meshing.mesh.triangulation.edgeLengthFunctions.IEdgeLengthFunction;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.gen.GenEikMesh;
import org.vadere.meshing.utils.io.tex.TexGraphGenerator;
import org.vadere.simulator.models.potential.solver.calculators.EikonalSolver;
import org.vadere.simulator.models.potential.solver.calculators.mesh.MeshEikonalSolverFMM;
import org.vadere.simulator.models.potential.solver.timecost.UnitTimeCostFunction;
import org.vadere.util.data.cellgrid.CellGrid;
import org.vadere.util.data.cellgrid.CellState;
import org.vadere.util.data.cellgrid.PathFindingTag;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.IDistanceFunction;

import java.awt.*;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class TestFFMNonUniformTriangulation {

    private static Logger log = Logger.getLogger(TestFFMNonUniformTriangulation.class);
    private int width;
    private int height;
    private VRectangle bbox;
    private IIncrementalTriangulation<PVertex, PHalfEdge, PFace> triangulation;
    private IDistanceFunction distanceFunc;

    @Before
    public void setUp() {
        //IDistanceFunction distanceFunc = p -> Math.abs(7 - Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY())) - 3;
        distanceFunc = p -> Math.abs(6 - Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY())) - 4;

        //distanceFunc = p -> -10+Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY());
        //IDistanceFunction distanceFunc = p -> Math.abs(7 - Math.max(Math.abs(p.getX()), Math.abs(p.getY()))) - 3;
        //IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + p.distanceToOrigin()*10;
        //IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + Math.abs(distanceFunc.apply(p));
        //IEdgeLengthFunction edgeLengthFunc = p -> 1.0;
        //IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + p.distanceToOrigin();
        //IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + Math.abs(distanceFunc.apply(p));


        bbox = new VRectangle(-12, -12, 24, 24);
    }

    private GenEikMesh<PVertex, PHalfEdge, PFace> createEikMesh(
    		@NotNull final IEdgeLengthFunction edgeLengthFunc,
		    final double initialEdgeLen) {
	    IMeshSupplier<PVertex, PHalfEdge, PFace> meshSupplier = () -> new PMesh();
	    GenEikMesh<PVertex, PHalfEdge, PFace> eikMesh = new GenEikMesh<>(
			    distanceFunc,
			    edgeLengthFunc,
			    initialEdgeLen,
			    bbox,
			    new ArrayList<>(),
			    meshSupplier);

	    return eikMesh;
    }

    @Ignore
    @Test
    public void testTriangulationFMM() {

        //IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + 0.5*Math.min(Math.abs(distanceFunc.apply(p) + 4), Math.abs(distanceFunc.apply(p)));
        IEdgeLengthFunction edgeLengthFunc = p -> 1.0;
        //IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + Math.abs(distanceFunc.apply(p)) * 0.5;
        List<VRectangle> targetAreas = new ArrayList<>();
        List<IPoint> targetPoints = new ArrayList<>();

	    /**
	     * We use the pointer based implementation
	     */
	    GenEikMesh<PVertex, PHalfEdge, PFace> meshGenerator = createEikMesh(edgeLengthFunc, 0.6);
        // () -> new PMesh<>((x, y) -> new EikMeshPoint(x, y, false))
	    meshGenerator.generate();
        triangulation = meshGenerator.getTriangulation();



	    Predicate<PFace> nonAccute = f -> triangulation.getMesh().toTriangle(f).isNonAcute();
	    //MeshPanel meshPanel = new MeshPanel(meshGenerator.getMesh(), nonAccute, 1000, 1000, bbox);
	    //meshPanel.display();

	    //targetPoints.add(new MeshPoint(0, 0, false));


        VRectangle rect = new VRectangle(width / 2, height / 2, 100, 100);
        targetAreas.add(rect);

	    List<PVertex> targetVertices = triangulation.getMesh().getBoundaryVertices().stream().collect(Collectors.toList());

        EikonalSolver solver = new MeshEikonalSolverFMM(
                new UnitTimeCostFunction(),
                triangulation,
		        targetVertices,
                distanceFunc);
		long ms = System.currentTimeMillis();
        log.info("start FFM");
        solver.solve();
        log.info("FFM finished");
        log.info("time: " + (System.currentTimeMillis() - ms));
        double maxError = 0;
        double sum = 0;
        int counter = 0;
        try {
            //System.out.println(getClass().getClassLoader().getResource("./potentialField.csv").getFile());
	        Date timestamp = new Date();

            FileWriter potentialFieldWriter = new FileWriter("./output/" + timestamp.getTime() + "potentialField_adapt_0_7.csv");
	        FileWriter meshWriter = new FileWriter("./output/"+ timestamp.getTime() + "mesh.tex");
	        meshWriter.write(TexGraphGenerator.toTikz(triangulation.getMesh(), true));

            for(double y = bbox.getMinY()+2; y <= bbox.getMaxY()-2; y += 0.1) {
                for(double x = bbox.getMinX()+2; x < bbox.getMaxX()-2; x += 0.1) {
                    double val = solver.getPotential(x ,y);
                    if(val >= 0.0 && val < Double.MAX_VALUE) {
                        double side = Math.min((new VPoint(x, y).distanceToOrigin()-2.0), (10 - new VPoint(x, y).distanceToOrigin()));
                        side = Math.max(side, 0.0);
                        maxError = Math.max(maxError, Math.abs(val - side));
                        sum +=  Math.abs(val - side) *  Math.abs(val - side);
                        counter++;
                    }
                    potentialFieldWriter.write(""+solver.getPotential(x ,y) + " ");
                }
                potentialFieldWriter.write("\n");
            }
            potentialFieldWriter.flush();
            potentialFieldWriter.close();
	        meshWriter.flush();
			meshWriter.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        log.info(triangulation.getMesh().getVertices().size());
        log.info("max edge length: " + triangulation.getMesh().streamEdges().map(e -> triangulation.getMesh().toLine(e).length()).max(Comparator.comparingDouble(d -> d)));
        log.info("min edge length: " +triangulation.getMesh().streamEdges().map(e -> triangulation.getMesh().toLine(e).length()).min(Comparator.comparingDouble(d -> d)));

        log.info("max distance to boundary: " + triangulation.getMesh().getBoundaryVertices().stream().map(p -> Math.abs(distanceFunc.apply(p))).max(Comparator.comparingDouble(d -> d)));
        //log.info("L2-Error: " + computeL2Error(triangulation, distanceFunc));
        log.info("max error: " + maxError);
        log.info("max error-2: " + triangulation.getMesh().getVertices().stream().map(p -> Math.abs(Math.abs(triangulation.getMesh().getDoubleData(p, MeshEikonalSolverFMM.namePotential) + distanceFunc.apply(p)))).max(Comparator.comparingDouble(d -> d)));

        log.info("L2-error: " + Math.sqrt(sum / counter));
        log.info("L2-error-2: " + Math.sqrt(triangulation.getMesh().getVertices().stream()
                .map(p -> Math.abs(Math.abs(triangulation.getMesh().getDoubleData(p, MeshEikonalSolverFMM.namePotential) + distanceFunc.apply(p))))
                .map(val -> val * val)
                .reduce(0.0, (d1, d2) -> d1 + d2) / triangulation.getMesh().getNumberOfVertices()));
        //assertTrue(0.0 == solver.getValue(5, 5));
        //assertTrue(0.0 < solver.getValue(1, 7));
    }

	@Ignore
    @Test
    public void testTriangulationFMMCase3() {

        //IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + Math.min(Math.abs(distanceFunc.apply(p) + 4) * 2, Math.abs(distanceFunc.apply(p)) * 2);
        //IEdgeLengthFunction unifromEdgeLengthFunc = p -> 1.0;
        IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + Math.abs(distanceFunc.apply(p)*0.5);
        List<VRectangle> targetAreas = new ArrayList<>();
	    GenEikMesh<PVertex, PHalfEdge, PFace> meshGenerator = createEikMesh(edgeLengthFunc, 0.6);
        meshGenerator.generate();
        triangulation = meshGenerator.getTriangulation();

        //targetPoints.add(new MeshPoint(0, 0, false));


        VRectangle rect = new VRectangle(width / 2, height / 2, 100, 100);
        targetAreas.add(rect);

        List<PVertex> targetPoints = triangulation.getMesh().getVertices().stream()
                .filter(v -> triangulation.getMesh().isAtBoundary(v))
                .filter(p->  (Math.abs(new VPoint(p.getX(), p.getY()).distanceToOrigin()-2.0)) < 2).collect(Collectors.toList());

        log.info(targetPoints);

        EikonalSolver solver = new MeshEikonalSolverFMM(
                new UnitTimeCostFunction(),
                triangulation,
                targetPoints,
                p -> -(new VPoint(p.getX(), p.getY()).distanceToOrigin()-2.0));

        log.info("start FFM");
        solver.solve();
        log.info("FFM finished");
        double maxError = 0;
        double sum = 0;
        int counter = 0;
        try {
            //System.out.println(getClass().getClassLoader().getResource("./potentialField.csv").getFile());
            FileWriter writer = new FileWriter("./potentialField_uniform_1_0_s.csv");

            for(double y = bbox.getMinY()+2; y <= bbox.getMaxY()-2; y += 0.1) {
                for(double x = bbox.getMinX()+2; x < bbox.getMaxX()-2; x += 0.1) {
                    double val = solver.getPotential(x ,y);
                    if(val >= 0.0) {
                        double side = new VPoint(x, y).distanceToOrigin()-2.0;
                        side = Math.max(side, 0.0);
                        maxError = Math.max(maxError, Math.abs(val - side));
                        sum +=  Math.abs(val - side) *  Math.abs(val - side);
                        counter++;
                    }
                    writer.write(""+solver.getPotential(x ,y) + " ");
                }
                writer.write("\n");
            }
            writer.flush();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        log.info(triangulation.getMesh().getVertices().size());
        log.info("max edge length: " + triangulation.getMesh().streamEdges().map(e -> triangulation.getMesh().toLine(e).length()).max(Comparator.comparingDouble(d -> d)));
        log.info("min edge length: " +triangulation.getMesh().streamEdges().map(e -> triangulation.getMesh().toLine(e).length()).min(Comparator.comparingDouble(d -> d)));
        //log.info("L2-Error: " + computeL2Error(triangulation, distanceFunc));
        log.info("max error: " + maxError);
        log.info("L2-error: " + Math.sqrt(sum / counter));
        log.info("max distance to boundary: " + triangulation.getMesh().getBoundaryVertices().stream().map(p -> Math.abs(distanceFunc.apply(p))).max(Comparator.comparingDouble(d -> d)));
        //assertTrue(0.0 == solver.getValue(5, 5));
        //assertTrue(0.0 < solver.getValue(1, 7));
    }

	@Ignore
    @Test
    public void testTriangulationFMMCase2() {

        //IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + Math.min(Math.abs(distanceFunc.apply(p) + 4) * 2, Math.abs(distanceFunc.apply(p)) * 2);
        //IEdgeLengthFunction unifromEdgeLengthFunc = p -> 1.0;
        IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + Math.abs(distanceFunc.apply(p)*0.5);
        List<VRectangle> targetAreas = new ArrayList<>();
	    GenEikMesh<PVertex, PHalfEdge, PFace> meshGenerator = createEikMesh(edgeLengthFunc, 0.6);
        meshGenerator.generate();
        triangulation = meshGenerator.getTriangulation();

        //targetPoints.add(new MeshPoint(0, 0, false));


        VRectangle rect = new VRectangle(width / 2, height / 2, 100, 100);
        targetAreas.add(rect);

        List<PVertex> targetPoints = triangulation.getMesh().getVertices().stream()
                .filter(v -> triangulation.getMesh().isAtBoundary(v))
                //.filter(p->  (Math.abs(new VPoint(p.getX(), p.getY()).distanceToOrigin()-2.0)) < 2)
		        .collect(Collectors.toList());

        log.info(targetPoints);

        EikonalSolver solver = new MeshEikonalSolverFMM(
                new UnitTimeCostFunction(),
                triangulation,
                targetPoints,
                p -> -(new VPoint(p.getX(), p.getY()).distanceToOrigin()-2.0));

        log.info("start FFM");
        solver.solve();
        log.info("FFM finished");
        double maxError = 0;
        double sum = 0;
        int counter = 0;
        try {
            //System.out.println(getClass().getClassLoader().getResource("./potentialField.csv").getFile());
            FileWriter writer = new FileWriter("./potentialField_uniform_1_0_s.csv");

            for(double y = bbox.getMinY()+2; y <= bbox.getMaxY()-2; y += 0.1) {
                for(double x = bbox.getMinX()+2; x < bbox.getMaxX()-2; x += 0.1) {
                    double val = solver.getPotential(x ,y);
                    if(val >= 0.0) {
                        double side = new VPoint(x, y).distanceToOrigin()-2.0;
                        side = Math.max(side, 0.0);
                        maxError = Math.max(maxError, Math.abs(val - side));
                        sum +=  Math.abs(val - side) *  Math.abs(val - side);
                        counter++;
                    }
                    writer.write(""+solver.getPotential(x ,y) + " ");
                }
                writer.write("\n");
            }
            writer.flush();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        log.info(triangulation.getMesh().getVertices().size());
        log.info("max edge length: " + triangulation.getMesh().streamEdges().map(e -> triangulation.getMesh().toLine(e).length()).max(Comparator.comparingDouble(d -> d)));
        log.info("min edge length: " +triangulation.getMesh().streamEdges().map(e -> triangulation.getMesh().toLine(e).length()).min(Comparator.comparingDouble(d -> d)));
        //log.info("L2-Error: " + computeL2Error(triangulation, distanceFunc));
        log.info("max error: " + maxError);
        log.info("L2-error: " + Math.sqrt(sum / counter));
        log.info("max distance to boundary: " + triangulation.getMesh().getBoundaryVertices().stream().map(p -> Math.abs(distanceFunc.apply(p))).max(Comparator.comparingDouble(d -> d)));
        //assertTrue(0.0 == solver.getValue(5, 5));
        //assertTrue(0.0 < solver.getValue(1, 7));
    }


    private double computeL2Error(@NotNull final IIncrementalTriangulation<PVertex, PHalfEdge, PFace> triangulation, final IDistanceFunction distanceFunc) {
        double sum = 0.0;
        for(PVertex vertex : triangulation.getMesh().getVertices()) {
            double diff = triangulation.getMesh().getDoubleData(vertex, MeshEikonalSolverFMM.namePotential) + distanceFunc.apply(vertex);
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    @Test
    public void testRegularFMM() {

        double resolution = 0.1;
        double rad = 2.0;
        double xMin = -10.0;
        double yMin = -10.0;
        CellGrid cellGrid = new CellGrid(bbox.getWidth()-3, bbox.getHeight()-3,resolution, new CellState(), xMin, yMin);
        cellGrid.pointStream()
                .filter(p -> distanceFunc.apply(cellGrid.pointToCoord(p)) >= 0)
                .forEach(p -> cellGrid.setValue(p, new CellState(0.0, PathFindingTag.Target)));

        EikonalSolver solver = new org.vadere.simulator.models.potential.solver.calculators.cartesian.EikonalSolverFMM(cellGrid, distanceFunc, false, new UnitTimeCostFunction(), 0.1, 1.0);


        log.info("start FFM");
        solver.solve();
        log.info("FFM finished");
        double maxError = 0;
        double sum = 0.0;
        int counter = 0;
        try {
            //System.out.println(getClass().getClassLoader().getResource("./potentialField.csv").getFile());
            Path p = Paths.get("target/classes").toAbsolutePath();
            p.toFile().mkdirs();
            FileWriter writer = new FileWriter(p.resolve("./potentialField_reg_0_4.csv").toAbsolutePath().toString());
            for(double y = yMin; y < bbox.getMaxY()-2.1; y += 0.1) {
                for(double x = xMin; x < bbox.getMaxX()-2.1; x += 0.1) {
                    double val = solver.getPotential(x ,y);
                    if(val > 0.0) {
                        double side = Math.min((new VPoint(x, y).distanceToOrigin()-2.0), (10 - new VPoint(x, y).distanceToOrigin()));
                        side = Math.max(side, 0.0);
                        sum +=  Math.abs(val - side) *  Math.abs(val - side);
                        //double distance = (new VPoint(x, y).distance(new VPoint(10, 10)))-rad;
                        //maxError = Math.max(maxError, Math.abs((distance >= 0.0 ? val-distance : val)));
                        maxError = Math.max(maxError, Math.abs(val - side));
                        counter++;
                    }
                    writer.write(""+solver.getPotential(x,y) + " ");
                }
                writer.write("\n");
            }


            writer.flush();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.info("max error: " + maxError);
        Stream<Point> resultPoints = cellGrid.pointStream().filter(p -> cellGrid.getValue(p).tag != PathFindingTag.Obstacle);
        double n = resultPoints.count();

        resultPoints = cellGrid.pointStream().filter(p -> cellGrid.getValue(p).tag != PathFindingTag.Obstacle);
        log.info("max error-2: " + resultPoints
                .map(p -> Math.abs(cellGrid.getValue(p).potential - distanceFunc.apply(cellGrid.pointToCoord(p))))
                .max(Comparator.comparingDouble(d -> d)));
        log.info("L2-error: " + Math.sqrt(sum /  counter));

        resultPoints = cellGrid.pointStream().filter(p -> cellGrid.getValue(p).tag != PathFindingTag.Obstacle);
        log.info("max error-2: " + Math.sqrt(resultPoints
                .map(p -> Math.abs(cellGrid.getValue(p).potential - distanceFunc.apply(cellGrid.pointToCoord(p))))
                .map(val -> val * val)
                .reduce(0.0, (d1, d2) -> d1 + d2) / n));

        //assertTrue(0.0 == solver.getValue(5, 5));
        //assertTrue(0.0 < solver.getValue(1, 7));
    }

    @Ignore
    @Test
    public void testRegularFMMCase2() {

        double resolution = 0.1;
        double rad = 2.0;
        double xMin = -10.0;
        double yMin = -10.0;
        CellGrid cellGrid = new CellGrid(bbox.getWidth()-3, bbox.getHeight()-3,resolution, new CellState(), xMin, yMin);

        // 1. Define the target points
        cellGrid.pointStream()
                .filter(p -> (cellGrid.pointToCoord(p).distanceToOrigin()-2.0) <= 0)
                .forEach(p -> cellGrid.setValue(p, new CellState(0.0, PathFindingTag.Target)));

        // 2. Define the obstacle points
        cellGrid.pointStream()
                .filter(p -> (cellGrid.pointToCoord(p).distanceToOrigin()) > 10)
                .forEach(p -> cellGrid.setValue(p, new CellState(Double.MAX_VALUE, PathFindingTag.Obstacle)));

        EikonalSolver solver = new org.vadere.simulator.models.potential.solver.calculators.cartesian.EikonalSolverFMM(
                cellGrid, p -> -(new VPoint(p.getX(), p.getY()).distanceToOrigin()-2.0),
                false, new UnitTimeCostFunction(), 0.1, 1.0);


        log.info("start FFM");
        solver.solve();
        log.info("FFM finished");
        double maxError = 0;
        double sum = 0.0;
        double maxVal = 0.0;
        int counter = 0;
        try {
            //System.out.println(getClass().getClassLoader().getResource("./potentialField.csv").getFile());
            FileWriter writer = new FileWriter("./potentialField_reg_0_6.csv");
            for(double y = yMin; y < bbox.getMaxY()-2; y += 0.1) {
                for(double x = xMin; x < bbox.getMaxY()-2; x += 0.1) {
                    double val = solver.getPotential(x ,y);
                    if(val < Double.MAX_VALUE) {
                        double side = Math.min(new VPoint(x, y).distanceToOrigin()-2.0, 8);
                        maxVal = Math.max(maxVal, val);
                        //log.info(maxVal);
                        side = Math.max(side, 0.0);
                        sum +=  Math.abs(val - side) *  Math.abs(val - side);
                        //double distance = (new VPoint(x, y).distance(new VPoint(10, 10)))-rad;
                        //maxError = Math.max(maxError, Math.abs((distance >= 0.0 ? val-distance : val)));
                        maxError = Math.max(maxError, Math.abs(val - side));
                        counter++;
                    }
                    writer.write(""+solver.getPotential(x,y) + " ");
                }
                writer.write("\n");
            }
            writer.flush();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.info("max error: " + maxError);
        log.info("L2-error: " + Math.sqrt(sum /  counter));
        //assertTrue(0.0 == solver.getValue(5, 5));
        //assertTrue(0.0 < solver.getValue(1, 7));
    }
}
