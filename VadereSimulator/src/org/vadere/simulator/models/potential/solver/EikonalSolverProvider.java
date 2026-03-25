
package org.vadere.simulator.models.potential.solver;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.pointLocator.JumpAndWalkPointLocator;
import org.vadere.simulator.models.potential.fields.IPotentialField;
import org.vadere.simulator.models.potential.solver.calculators.EikonalSolver;
import org.vadere.simulator.models.potential.solver.calculators.PotentialFieldCalculatorNone;
import org.vadere.simulator.models.potential.solver.calculators.cartesian.multiCoreOptimized.EikonalSolverFIM;
import org.vadere.simulator.models.potential.solver.calculators.cartesian.EikonalSolverFMM;
import org.vadere.simulator.models.potential.solver.calculators.cartesian.multiCoreOptimized.EikonalSolverIFIM;
import org.vadere.simulator.models.potential.solver.calculators.mesh.MeshEikonalSolverFMM;
import org.vadere.simulator.models.potential.solver.calculators.mesh.multiCoreOptimized.MeshEikonalSolverFIM;
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
		ITimeCostFunction timeCost = new UnitTimeCostFunction();
		EikonalSolver eikonalSolver = new MeshEikonalSolverFMM(
				targetShapes,
				timeCost,
				domain.getFloorFieldMesh(),
				new JumpAndWalkPointLocator(domain.getFloorFieldMesh().getMesh()));

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

		if(createMethod == EikonalSolverType.NONE) {
			return new PotentialFieldCalculatorNone();
		}

		if(createMethod.isUsingCellGrid()) {
			return createGridBasedSolver(targetId, targetShapes, attributesPedestrian, attributesPotential, bounds, createMethod, topography);
		}

		if(domain.getFloorFieldMesh() != null) {
			return createTriangleMeshBasedSolver(domain, targetId, targetShapes, attributesPedestrian, attributesPotential, topography, createMethod);
		}else{
			throw new UnsupportedOperationException("Can not use mesh based floor field computation without a mesh!");
		}
	}

	/**
	 * Use a regular grid based method.
	 */
	@NotNull
	private static EikonalSolver createGridBasedSolver(int targetId, List<VShape> targetShapes, AttributesAgent attributesPedestrian, AttributesFloorField attributesPotential, Rectangle2D.Double bounds, EikonalSolverType createMethod, Topography topography) {
		EikonalSolver eikonalSolver;
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
			case INFORMED_FAST_ITERATIVE_METHOD:
				eikonalSolver = new EikonalSolverIFIM(cellGrid, distFunc, timeCost, attributesPotential.getObstacleGridPenalty(), attributesPotential.getTargetAttractionStrength());
				break;
			case HIGH_ACCURACY_FAST_MARCHING:
			case FAST_MARCHING:
			default:
				eikonalSolver = new EikonalSolverFMM(cellGrid, distFunc, isHighAccuracyFM, timeCost, attributesPotential.getObstacleGridPenalty(), attributesPotential.getTargetAttractionStrength());
		}
		return eikonalSolver;
	}

	@NotNull
	private static EikonalSolver createTriangleMeshBasedSolver(Domain domain, int targetId, List<VShape> targetShapes, AttributesAgent attributesPedestrian, AttributesFloorField attributesPotential, Topography topography, EikonalSolverType createMethod) {
		EikonalSolver eikonalSolver;
		var triangulation = domain.getFloorFieldMesh();

		ITimeCostFunction timeCost = TimeCostFunctionFactory.create(
				attributesPotential.getTimeCostAttributes(),
				attributesPedestrian,
				topography,
				targetId, triangulation);

		var pointLocator = new JumpAndWalkPointLocator<>(triangulation.getMesh());
		switch (createMethod) {
			case FAST_ITERATIVE_METHOD_TRI:
				eikonalSolver = new MeshEikonalSolverFIM<>(targetId +"", targetShapes, timeCost, triangulation, pointLocator);
				break;
			case FAST_MARCHING_TRI:
			default:
				eikonalSolver = new MeshEikonalSolverFMM<>(targetId +"", targetShapes, timeCost, triangulation, pointLocator);
				break;
		}
		return eikonalSolver;
	}
}
