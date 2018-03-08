package org.vadere.simulator.models.potential.fields;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.simulator.models.potential.timeCostFunction.TimeCostFunctionFactory;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.scenario.Topography;
import org.vadere.state.types.EikonalSolverType;
import org.vadere.util.geometry.mesh.gen.PFace;
import org.vadere.util.geometry.mesh.gen.PHalfEdge;
import org.vadere.util.geometry.mesh.gen.PVertex;
import org.vadere.util.geometry.mesh.inter.ITriangulation;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.potential.CellGrid;
import org.vadere.util.potential.CellState;
import org.vadere.util.potential.FloorDiscretizer;
import org.vadere.util.potential.PathFindingTag;
import org.vadere.util.potential.calculators.EikonalSolver;
import org.vadere.util.potential.calculators.PotentialFieldCalculatorNone;
import org.vadere.util.potential.calculators.cartesian.EikonalSolverFIM;
import org.vadere.util.potential.calculators.cartesian.EikonalSolverFMM;
import org.vadere.util.potential.calculators.cartesian.EikonalSolverFSM;
import org.vadere.util.potential.calculators.mesh.EikonalSolverFMMTriangulation;
import org.vadere.util.potential.timecost.ITimeCostFunction;
import org.vadere.util.triangulation.adaptive.DistanceFunction;
import org.vadere.util.triangulation.adaptive.IDistanceFunction;
import org.vadere.util.triangulation.adaptive.MeshPoint;
import org.vadere.util.triangulation.improver.PPSMeshing;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A potential field for some agents: ((x,y), agent) -> potential.
 *
 * @author Benedikt Zoennchen
 */
@FunctionalInterface
public interface IPotentialField {

    /**
     * Returns a potential at pos for the agent. This can be any potential:
     * ((x,y), agent) -> potential
     *
     * @param pos   the position for which the potential will be evaluated
     * @param agent the agent for which the potential will be evaluated
     * @return a potential at pos for the agent
     */
    double getPotential(final VPoint pos, final Agent agent);


    Logger logger = LogManager.getLogger(IPotentialField.class);

    /**
     * Factory method to construct an EikonalSolver for agents of target defined by targetShapes and targetId.
     * This method will also generate the underlying mesh/grid which discretise the spacial domain.
     *
     * @param topography            the topography
     * @param targetId              the
     * @param targetShapes          the area where T = 0
     * @param attributesPedestrian  pedestrian configuration
     * @param attributesPotential   potential field configuration (dynamic or static, parameters and so on...)
     * @return an EikonalSolver for a specific target
     */
    static EikonalSolver create(
            final Topography topography,
            final int targetId,
            final List<VShape> targetShapes,
            final AttributesAgent attributesPedestrian,
            final AttributesFloorField attributesPotential) {

        EikonalSolverType createMethod = attributesPotential.getCreateMethod();

        Rectangle2D.Double bounds = topography.getBounds();
	    EikonalSolver eikonalSolver;

	    /**
	     * Use a regular grid based method.
	     */
        if(createMethod.isUsingCellGrid()) {
	        CellGrid cellGrid = new CellGrid(bounds.getWidth(), bounds.getHeight(),
			        attributesPotential.getPotentialFieldResolution(), new CellState());

	        if (createMethod != EikonalSolverType.NONE) {
		        for (VShape shape : targetShapes) {
			        FloorDiscretizer.setGridValuesForShapeCentered(cellGrid, shape,
					        new CellState(0.0, PathFindingTag.Target));
		        }

		        for (Obstacle obstacle : topography.getObstacles()) {
			        FloorDiscretizer.setGridValuesForShapeCentered(cellGrid, obstacle.getShape(),
					        new CellState(Double.MAX_VALUE, PathFindingTag.Obstacle));
		        }
	        }

	        boolean isHighAccuracyFM = createMethod.isHighAccuracy();

	        ITimeCostFunction timeCost = TimeCostFunctionFactory.create(
			        attributesPotential.getTimeCostAttributes(),
			        attributesPedestrian,
			        topography,
			        targetId, 1.0 / cellGrid.getResolution());

	        switch (createMethod) {
		        case NONE:
			        eikonalSolver = new PotentialFieldCalculatorNone();
			        break;
		        case FAST_ITERATIVE_METHOD:
			        eikonalSolver = new EikonalSolverFIM(cellGrid, targetShapes, isHighAccuracyFM, timeCost, attributesPotential.getObstacleGridPenalty(), attributesPotential.getTargetAttractionStrength());
			        break;
		        case FAST_SWEEPING_METHOD:
			        eikonalSolver = new EikonalSolverFSM(cellGrid, targetShapes, isHighAccuracyFM, timeCost, attributesPotential.getObstacleGridPenalty(), attributesPotential.getTargetAttractionStrength());
			        break;
		        default:
			        eikonalSolver = new EikonalSolverFMM(cellGrid, targetShapes, isHighAccuracyFM, timeCost, attributesPotential.getObstacleGridPenalty(), attributesPotential.getTargetAttractionStrength());
	        }
        }
        /**
         * Use a mesh based method.
         */
        else {
	        //Collection<VShape> holes = Topography.createObstacleBoundary(topography).stream().map(obs -> obs.getShape()).collect(Collectors.toList());
	        Collection<VShape> holes = new ArrayList<>();

	        holes.addAll(topography.getObstacles().stream().map(obs -> obs.getShape()).collect(Collectors.toList()));
	        //holes.addAll(topography.getTargets(targetId).stream().map(target -> target.getShape()).collect(Collectors.toList()));
			VRectangle bbox = new VRectangle(bounds);

	        /**
	         * A default distance function which uses all shapes to compute the distance.
	         */
			IDistanceFunction distanceFunc = new DistanceFunction(bbox, holes);

	        /**
	         * Generate the mesh, we use the pointer based implementation here.
	         */
	        PPSMeshing meshGenerator = new PPSMeshing(distanceFunc, p -> 1.0, 3.0, bbox, holes);
	        meshGenerator.generate();

	        ITimeCostFunction timeCost = TimeCostFunctionFactory.create(
			        attributesPotential.getTimeCostAttributes(),
			        attributesPedestrian,
			        topography,
			        targetId,
			        //TODO [refactoring]: this attribute value is used in an not intuitive way, we should introduce an extra attribute value!
			        1.0 / attributesPotential.getPotentialFieldResolution());

	        ITriangulation<MeshPoint, PVertex<MeshPoint>, PHalfEdge<MeshPoint>, PFace<MeshPoint>> triangulation = meshGenerator.getTriangulation();

	        List<PVertex<MeshPoint>> targetVertices = triangulation.getMesh().getBoundaryVertices().stream().collect(Collectors.toList());

	        eikonalSolver = new EikonalSolverFMMTriangulation(
			        timeCost,
			        triangulation,
			        targetVertices,
			        distanceFunc);
        }

        long ms = System.currentTimeMillis();
        eikonalSolver.initialize();
        logger.info("floor field initialization time:" + (System.currentTimeMillis() - ms + "[ms]"));
        return eikonalSolver;
    }
}
