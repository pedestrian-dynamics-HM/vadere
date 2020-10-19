
package org.vadere.simulator.models.potential.solver;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.AFace;
import org.vadere.meshing.mesh.gen.AHalfEdge;
import org.vadere.meshing.mesh.gen.AVertex;
import org.vadere.meshing.mesh.gen.IncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.simulator.models.potential.fields.IPotentialField;
import org.vadere.simulator.models.potential.solver.calculators.EikonalSolver;
import org.vadere.simulator.models.potential.solver.calculators.PotentialFieldCalculatorNone;
import org.vadere.simulator.models.potential.solver.calculators.cartesian.EikonalSolverFIM;
import org.vadere.simulator.models.potential.solver.calculators.cartesian.EikonalSolverFMM;
import org.vadere.simulator.models.potential.solver.calculators.cartesian.EikonalSolverFSM;
import org.vadere.simulator.models.potential.solver.calculators.cartesian.EikonalSolverIFIM;
import org.vadere.simulator.models.potential.solver.calculators.mesh.MeshEikonalSolverFIM;
import org.vadere.simulator.models.potential.solver.calculators.mesh.MeshEikonalSolverFIMLockFree;
import org.vadere.simulator.models.potential.solver.calculators.mesh.MeshEikonalSolverFMM;
import org.vadere.simulator.models.potential.solver.calculators.mesh.MeshEikonalSolverIFIM;
import org.vadere.simulator.models.potential.solver.calculators.mesh.MeshEikonalSolverIFIMLockFree;
import org.vadere.simulator.models.potential.solver.timecost.ITimeCostFunction;
import org.vadere.simulator.models.potential.solver.timecost.UnitTimeCostFunction;
import org.vadere.simulator.models.potential.timeCostFunction.TimeCostFunctionFactory;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.scenario.Topography;
import org.vadere.state.types.EikonalSolverType;
import org.vadere.util.data.cellgrid.CellGrid;
import org.vadere.util.data.cellgrid.CellState;
import org.vadere.util.data.cellgrid.FloorDiscretizer;
import org.vadere.util.data.cellgrid.PathFindingTag;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.DistanceFunctionTarget;
import org.vadere.util.math.IDistanceFunction;

import java.awt.geom.Rectangle2D;
import java.util.List;

public abstract class EikonalSolverProvider  {
	private  static Logger logger = Logger.getLogger(IPotentialField.class);


	public abstract EikonalSolver provide(
			final Domain domain,
			final int targetId,
			final List<VShape> targetShapes,
			final AttributesAgent attributesPedestrian,
			final AttributesFloorField attributesPotential);

	protected EikonalSolver buildBase(final Domain domain, @NotNull final List<VShape> targetShapes) {
		var triangulation = new IncrementalTriangulation<>(domain.getFloorFieldMesh());

		ITimeCostFunction timeCost = new UnitTimeCostFunction();
		EikonalSolver eikonalSolver = new MeshEikonalSolverFMM(
				targetShapes,
				timeCost,
				triangulation);

		return eikonalSolver;
	}

	/**
	 * Returns a new {@link EikonalSolver} which can be used to compute the eikonal equation.
	 *
	 * @param domain                    representation of the spatial domain containing the topography
	 * @param targetId                  the target for which the solver solves the eikonal equation for
	 * @param targetShapes              the target shapes i.e. all points for which T = 0.
	 * @param attributesPedestrian
	 * @param attributesPotential
	 *
	 * @return a new {@link EikonalSolver} which can be used to compute the eikonal equation
	 */
	protected EikonalSolver buildBase(
			final Domain domain,
			final int targetId,
			final List<VShape> targetShapes,
			final AttributesAgent attributesPedestrian,
			final AttributesFloorField attributesPotential){
		logger.debug("create EikonalSolver");
		Topography topography = domain.getTopography();
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
				case INFORMED_FAST_ITERATIVE_METHOD:
					eikonalSolver = new EikonalSolverIFIM(cellGrid, distFunc, timeCost, attributesPotential.getObstacleGridPenalty(), attributesPotential.getTargetAttractionStrength());
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
		} else {
			if(domain.getFloorFieldMesh() != null) {
				IIncrementalTriangulation<AVertex, AHalfEdge, AFace> triangulation = new IncrementalTriangulation<>(domain.getFloorFieldMesh());

				ITimeCostFunction timeCost = TimeCostFunctionFactory.create(
						attributesPotential.getTimeCostAttributes(),
						attributesPedestrian,
						topography,
						targetId, triangulation);

				switch (createMethod) {
					case INFORMED_FAST_ITERATIVE_METHOD_TRI:
						eikonalSolver = new MeshEikonalSolverIFIM<>(targetId+"", targetShapes, timeCost, triangulation);
						break;
					case FAST_ITERATIVE_METHOD_TRI:
						eikonalSolver = new MeshEikonalSolverFIM<>(targetId+"", targetShapes, timeCost, triangulation);
						break;
					case FAST_ITERATIVE_METHOD_TRI_LOCK_FREE:
						eikonalSolver = new MeshEikonalSolverFIMLockFree<>(targetId+"", targetShapes, timeCost, triangulation);
						break;
					case INFORMED_FAST_ITERATIVE_METHOD_TRI_LOCK_FREE:
						eikonalSolver = new MeshEikonalSolverIFIMLockFree<>(targetId+"", targetShapes, timeCost, triangulation);
						break;
					default:
						eikonalSolver = new MeshEikonalSolverFMM<>(targetId+"", targetShapes, timeCost, triangulation);
						break;
				}

				//eikonalSolver = new MeshEikonalSolverFMM<>(targetId+"", targetShapes, timeCost, triangulation
				//	/*,topography.getSources().stream().map(s -> s.getShape()).collect(Collectors.toList())*/);
				//eikonalSolver.solve();
			} else {
				throw new UnsupportedOperationException("Can not use mesh based floor field computation without a mesh!");
			}
		}
		return eikonalSolver;
	}
}
