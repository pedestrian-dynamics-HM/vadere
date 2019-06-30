package org.vadere.simulator.models.potential.fields;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.triangulation.IEdgeLengthFunction;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.gen.PEikMeshGen;
import org.vadere.simulator.models.potential.solver.calculators.EikonalSolver;
import org.vadere.simulator.models.potential.solver.calculators.PotentialFieldCalculatorNone;
import org.vadere.simulator.models.potential.solver.calculators.cartesian.EikonalSolverFIM;
import org.vadere.simulator.models.potential.solver.calculators.cartesian.EikonalSolverFMM;
import org.vadere.simulator.models.potential.solver.calculators.cartesian.EikonalSolverFSM;
import org.vadere.simulator.models.potential.solver.calculators.mesh.EikonalSolverFMMTriangulation;
import org.vadere.simulator.models.potential.solver.calculators.mesh.PotentialPoint;
import org.vadere.simulator.models.potential.solver.timecost.ITimeCostFunction;
import org.vadere.simulator.models.potential.timeCostFunction.TimeCostFunctionFactory;
import org.vadere.simulator.utils.cache.CacheLoader;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.scenario.Topography;
import org.vadere.state.types.EikonalSolverType;
import org.vadere.simulator.utils.cache.ScenarioCache;
import org.vadere.util.data.cellgrid.CellGrid;
import org.vadere.util.data.cellgrid.CellState;
import org.vadere.util.data.cellgrid.FloorDiscretizer;
import org.vadere.util.data.cellgrid.PathFindingTag;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.DistanceFunction;
import org.vadere.util.math.DistanceFunctionTarget;
import org.vadere.util.math.IDistanceFunction;
import org.vadere.util.math.InterpolationUtil;

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
    double getPotential(final IPoint pos, final Agent agent);

    Logger logger = Logger.getLogger(IPotentialField.class);

    /**
     * Factory method to construct an EikonalSolver for agents of target defined by targetShapes and targetId.
     * This method will also generate the underlying mesh/grid which discretise the spacial domain.
     *
     * @param topography            the topography
     * @param targetId              the
     * @param targetShapes          the area where T = 0
     * @param attributesPedestrian  pedestrian configuration
     * @param attributesPotential   potential field configuration (dynamic or static, parameters and so on...)
     * @param cache
	 * @return an EikonalSolver for a specific target
     */
    static EikonalSolver create(
            final Topography topography,
            final int targetId,
            final List<VShape> targetShapes,
            final AttributesAgent attributesPedestrian,
            final AttributesFloorField attributesPotential,
			final ScenarioCache cache) {
		logger.debug("create EikonalSolver");
        EikonalSolverType createMethod = attributesPotential.getCreateMethod();

        Rectangle2D.Double bounds = topography.getBounds();
	    EikonalSolver eikonalSolver;

	    if(createMethod == EikonalSolverType.NONE) {
	    	return new PotentialFieldCalculatorNone();
	    }

	    /**
	     * Use a regular grid based method.
	     */
        if(createMethod.isUsingCellGrid()) {
	        CellGrid cellGrid = new CellGrid(bounds.getWidth(), bounds.getHeight(),
			        attributesPotential.getPotentialFieldResolution(), new CellState(), bounds.getMinX(), bounds.getMinY());

	        if (createMethod != EikonalSolverType.NONE) {
		        for (VShape shape : targetShapes) {
			        FloorDiscretizer.setGridValuesForShape(cellGrid, shape,
					        new CellState(0.0, PathFindingTag.Target));
		        }

		        for (Obstacle obstacle : topography.getObstacles()) {
			        FloorDiscretizer.setGridValuesForShape(cellGrid, obstacle.getShape(),
					        new CellState(Double.MAX_VALUE, PathFindingTag.Obstacle));
		        }
	        }

	        boolean isHighAccuracyFM = createMethod.isHighAccuracy();

	        ITimeCostFunction timeCost = TimeCostFunctionFactory.create(
			        attributesPotential.getTimeCostAttributes(),
			        attributesPedestrian,
			        topography,
			        targetId, 1.0 / cellGrid.getResolution());

	        /**
	         * The distance function returns values < 0 if the point is inside the domain,
	         * i.e. outside of any obstacle and values > 0 if the point lies inside an obstacle.
	         */
	        IDistanceFunction distFunc = new DistanceFunctionTarget(cellGrid, targetShapes);

	        switch (createMethod) {
		        case NONE:
			        eikonalSolver = new PotentialFieldCalculatorNone();
			        break;
		        case FAST_ITERATIVE_METHOD:
			        eikonalSolver = new EikonalSolverFIM(cellGrid, distFunc, isHighAccuracyFM, timeCost, attributesPotential.getObstacleGridPenalty(), attributesPotential.getTargetAttractionStrength());
			        break;
		        case FAST_SWEEPING_METHOD:
			        eikonalSolver = new EikonalSolverFSM(cellGrid, distFunc, isHighAccuracyFM, timeCost, attributesPotential.getObstacleGridPenalty(), attributesPotential.getTargetAttractionStrength());
			        break;
		        default:
			        eikonalSolver = new EikonalSolverFMM(cellGrid, distFunc, isHighAccuracyFM, timeCost, attributesPotential.getObstacleGridPenalty(), attributesPotential.getTargetAttractionStrength());
	        }
        }
        /**
         * Use a mesh based method.
         */
        else {
	        //Collection<VShape> holes = Topography.createObstacleBoundary(topography).stream().map(obs -> obs.getShape()).collect(Collectors.toList());
	        Collection<VShape> holes = new ArrayList<>();

	        holes.addAll(topography.getObstacles().stream().map(obs -> obs.getShape()).collect(Collectors.toList()));
	        holes.addAll(topography.getTargets(targetId).stream().map(target -> target.getShape()).collect(Collectors.toList()));
			VRectangle bbox = new VRectangle(bounds);

	        /*
	          A default distance function which uses all shapes to compute the distance.
	         */
			IDistanceFunction distanceFunc = new DistanceFunction(bbox, holes);
	        IEdgeLengthFunction edgeLengthFunction = p -> 1.0 + Math.max(0, Math.min(-distanceFunc.apply(p), 22));

	        //IEdgeLengthFunction edgeLengthFunction = p -> 1.0;

	        /*
	          Generate the mesh, we use the pointer based implementation here.
	         */
	        PEikMeshGen<PotentialPoint> meshGenerator = new PEikMeshGen<>(distanceFunc,edgeLengthFunction, attributesPotential.getPotentialFieldResolution(), bbox, holes, (x, y) -> new PotentialPoint(x ,y));
	        IIncrementalTriangulation<PotentialPoint, PVertex<PotentialPoint>, PHalfEdge<PotentialPoint>, PFace<PotentialPoint>> triangulation = meshGenerator.generate();

	        ITimeCostFunction timeCost = TimeCostFunctionFactory.create(
			        attributesPotential.getTimeCostAttributes(),
			        attributesPedestrian,
			        topography,
			        targetId,
			        //TODO [refactoring]: this attribute value is used in an not intuitive way, we should introduce an extra attribute value!
			        1.0 / attributesPotential.getPotentialFieldResolution());

	        // TODO: here we assume the shapes are convex!
	        List<PVertex<PotentialPoint>> targetVertices = new ArrayList<>();
	        for(VShape shape : targetShapes) {
	        	VPoint point = shape.getCentroid();
	        	PFace<PotentialPoint> targetFace = triangulation.locateFace(point.getX(), point.getY()).get();
				targetVertices.addAll(triangulation.getMesh().getVertices(targetFace));
	        }

	        eikonalSolver = new EikonalSolverFMMTriangulation(
			        timeCost,
			        triangulation,
			        targetVertices,
			        distanceFunc);
        }

		/*
		   Initialize floor field. If caching is activate try to read cached version. If no
		   cache is present or the cache loading does not work fall back to standard
		   floor field initialization and log errors.
		 */
		boolean isInitialized = false;
		logger.info("initialize floor field");
		if (attributesPotential.isUseCachedFloorField() && cache.isNotEmpty()){
			String targetIdentifier = cache.targetToIdentifier(targetId);
			long ms = System.currentTimeMillis();
			CacheLoader cacheLoader = cache.getCacheForTarget(targetId);
			if (cacheLoader != null){
				isInitialized = eikonalSolver.loadCachedFloorField(cacheLoader);
				logger.info("floor field initialization time:" + (System.currentTimeMillis() - ms + "[ms] (cache load time)"));
			} else {
				ms = System.currentTimeMillis();
				logger.infof("No cache found for scenario initialize floor field");
				eikonalSolver.initialize();
				isInitialized = true;
				logger.info("floor field initialization time:" + (System.currentTimeMillis() - ms + "[ms]"));
				ms = System.currentTimeMillis();
				logger.info("save floor field cache:");
				eikonalSolver.saveFloorFieldToCache(cache, targetIdentifier);
				logger.info("save floor field cache time:" + (System.currentTimeMillis() - ms + "[ms]"));
			}
		}

		if (!isInitialized){
			long ms = System.currentTimeMillis();
			eikonalSolver.initialize();
			logger.info("floor field initialization time:" + (System.currentTimeMillis() - ms + "[ms]"));
		}

        return eikonalSolver;
    }


	static IPotentialField copyAgentField(
			final @NotNull IPotentialField potentialField,
			final @NotNull Agent agent,
			final @NotNull VRectangle bound,
			final double steps) {

		final int gridWidth = (int)Math.ceil(bound.getWidth() / steps)+1;
		final int gridHeight = (int)Math.ceil(bound.getHeight() / steps)+1;
		final double[][] potentialFieldApproximation = new double[gridHeight][gridWidth];

		for(int row = 0; row < gridHeight; row++) {
			for(int col = 0; col < gridWidth; col++) {
				double x = col*steps;
				double y = row*steps;
				potentialFieldApproximation[row][col] = potentialField.getPotential(new VPoint(x, y), agent);
			}
		}

		return (pos, ped) -> {
			if(ped.equals(agent)) {
				int incX = 1;
				int incY = 1;

				int col = (int)((pos.getX() - bound.getMinX()) / steps);
				int row = (int)((pos.getY() - bound.getMinY()) / steps);

				if (col + 1 >= gridWidth) {
					incX = 0;
				}

				if (row + 1 >= gridHeight) {
					incY = 0;
				}

				VPoint gridPointCoord = new VPoint(col * steps, row * steps);
				double z1 = potentialFieldApproximation[row][col];
				double z2 = potentialFieldApproximation[row][col + incX];
				double z3 = potentialFieldApproximation[row + incY][col + incX];
				double z4 = potentialFieldApproximation[row + incY][col];

				double t = (pos.getX() - gridPointCoord.x) / steps;
				double u = (pos.getY() - gridPointCoord.y) / steps;

				return InterpolationUtil.bilinearInterpolation(z1, z2, z3, z4, t, u);
			}
			else {
				return 0.0;
			}
		};
	}
}
