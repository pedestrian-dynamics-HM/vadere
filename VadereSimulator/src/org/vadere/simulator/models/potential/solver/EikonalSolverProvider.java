package org.vadere.simulator.models.potential.solver;

import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.triangulation.IEdgeLengthFunction;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.impl.PEikMesh;
import org.vadere.simulator.models.potential.fields.IPotentialField;
import org.vadere.simulator.models.potential.solver.calculators.EikonalSolver;
import org.vadere.simulator.models.potential.solver.calculators.PotentialFieldCalculatorNone;
import org.vadere.simulator.models.potential.solver.calculators.cartesian.EikonalSolverFIM;
import org.vadere.simulator.models.potential.solver.calculators.cartesian.EikonalSolverFMM;
import org.vadere.simulator.models.potential.solver.calculators.cartesian.EikonalSolverFSM;
import org.vadere.simulator.models.potential.solver.calculators.mesh.EikonalSolverFMMTriangulation;
import org.vadere.simulator.models.potential.solver.timecost.ITimeCostFunction;
import org.vadere.simulator.models.potential.timeCostFunction.TimeCostFunctionFactory;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.scenario.Topography;
import org.vadere.state.types.EikonalSolverType;
import org.vadere.util.data.cellgrid.CellGrid;
import org.vadere.util.data.cellgrid.CellState;
import org.vadere.util.data.cellgrid.FloorDiscretizer;
import org.vadere.util.data.cellgrid.PathFindingTag;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.DistanceFunction;
import org.vadere.util.math.DistanceFunctionTarget;
import org.vadere.util.math.IDistanceFunction;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public abstract class EikonalSolverProvider  {
	private  static Logger logger = Logger.getLogger(IPotentialField.class);

	public abstract EikonalSolver provide(
			final Topography topography,
			final int targetId,
			final List<VShape> targetShapes,
			final AttributesAgent attributesPedestrian,
			final AttributesFloorField attributesPotential);

	protected EikonalSolver buildBase(
			final Topography topography,
			final int targetId,
			final List<VShape> targetShapes,
			final AttributesAgent attributesPedestrian,
			final AttributesFloorField attributesPotential){
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
			IEdgeLengthFunction edgeLengthFunction = p -> 1.0 + Math.max(0, -distanceFunc.apply(p));

	        /*
	          Generate the mesh, we use the pointer based implementation here.
	         */
			PEikMesh meshGenerator = new PEikMesh(distanceFunc,edgeLengthFunction, 0.7, bbox, holes);
			IIncrementalTriangulation<PVertex, PHalfEdge, PFace> triangulation = meshGenerator.generate();

			ITimeCostFunction timeCost = TimeCostFunctionFactory.create(
					attributesPotential.getTimeCostAttributes(),
					attributesPedestrian,
					topography,
					targetId,
					//TODO [refactoring]: this attribute value is used in an not intuitive way, we should introduce an extra attribute value!
					1.0 / attributesPotential.getPotentialFieldResolution());

			// TODO: here we assume the shapes are convex!
			List<PVertex> targetVertices = new ArrayList<>();
			for(VShape shape : targetShapes) {
				VPoint point = shape.getCentroid();
				PFace targetFace = triangulation.locateFace(point.getX(), point.getY()).get();
				targetVertices.addAll(triangulation.getMesh().getVertices(targetFace));
			}

			eikonalSolver = new EikonalSolverFMMTriangulation(
					timeCost,
					triangulation,
					targetVertices,
					distanceFunc);

		}
		return eikonalSolver;
	}
}
