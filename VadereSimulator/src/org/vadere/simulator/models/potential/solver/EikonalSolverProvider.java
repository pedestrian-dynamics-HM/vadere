
package org.vadere.simulator.models.potential.solver;

import it.unimi.dsi.fastutil.io.FastBufferedInputStream;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.IncrementalTriangulation;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.impl.PMeshPanel;
import org.vadere.meshing.mesh.impl.PSLG;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.triangulation.DistanceFunctionApproxBF;
import org.vadere.meshing.mesh.triangulation.EdgeLengthFunctionApprox;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.impl.PEikMesh;
import org.vadere.meshing.utils.io.poly.MeshPolyReader;
import org.vadere.simulator.models.potential.fields.IPotentialField;
import org.vadere.simulator.models.potential.solver.calculators.EikonalSolver;
import org.vadere.simulator.models.potential.solver.calculators.PotentialFieldCalculatorNone;
import org.vadere.simulator.models.potential.solver.calculators.cartesian.EikonalSolverFIM;
import org.vadere.simulator.models.potential.solver.calculators.cartesian.EikonalSolverFMM;
import org.vadere.simulator.models.potential.solver.calculators.cartesian.EikonalSolverFSM;
import org.vadere.simulator.models.potential.solver.calculators.mesh.EikonalSolverFMMTriangulation;
import org.vadere.simulator.models.potential.solver.timecost.ITimeCostFunction;
import org.vadere.simulator.models.potential.solver.timecost.UnitTimeCostFunction;
import org.vadere.simulator.models.potential.timeCostFunction.TimeCostFunctionFactory;
import org.vadere.simulator.projects.Domain;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.utils.pslg.PSLGConverter;
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
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.DistanceFunctionTarget;
import org.vadere.util.math.IDistanceFunction;

import java.awt.geom.Rectangle2D;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public abstract class EikonalSolverProvider  {
	private  static Logger logger = Logger.getLogger(IPotentialField.class);


	public abstract EikonalSolver provide(
			final Domain domain,
			final int targetId,
			final List<VShape> targetShapes,
			final AttributesAgent attributesPedestrian,
			final AttributesFloorField attributesPotential);

	protected EikonalSolver buildBase(final Domain domain, @NotNull final List<VShape> targetShapes) {
		var triangulation = new IncrementalTriangulation<>(domain.getBackgroundMesh());

		ITimeCostFunction timeCost = new UnitTimeCostFunction();
		EikonalSolver eikonalSolver = new EikonalSolverFMMTriangulation(
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
				case FAST_ITERATIVE_METHOD:
					eikonalSolver = new EikonalSolverFIM(cellGrid, distFunc, isHighAccuracyFM, timeCost, attributesPotential.getObstacleGridPenalty(), attributesPotential.getTargetAttractionStrength());
					break;
				case FAST_SWEEPING_METHOD:
					eikonalSolver = new EikonalSolverFSM(cellGrid, distFunc, isHighAccuracyFM, timeCost, attributesPotential.getObstacleGridPenalty(), attributesPotential.getTargetAttractionStrength());
					break;
				default:
					eikonalSolver = new EikonalSolverFMM(cellGrid, distFunc, isHighAccuracyFM, timeCost, attributesPotential.getObstacleGridPenalty(), attributesPotential.getTargetAttractionStrength());
			}
		} else if(domain.getBackgroundMesh() != null) {
			IIncrementalTriangulation<PVertex, PHalfEdge, PFace> triangulation = new IncrementalTriangulation<>(domain.getBackgroundMesh());
			eikonalSolver = new EikonalSolverFMMTriangulation<>("target_" + targetId, targetShapes, new UnitTimeCostFunction(), triangulation);
			//eikonalSolver.initialize();
			//System.out.println(triangulation.getMesh().toPythonTriangulation(v -> triangulation.getMesh().getDoubleData(v, "target_"+targetId+"_potential")));
			//System.out.println();
		} else {
			throw new UnsupportedOperationException("potential field has to be grid based.");
		}
		return eikonalSolver;
	}
}
