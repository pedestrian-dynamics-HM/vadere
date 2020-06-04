package org.vadere.simulator.models.potential.solver;

import org.vadere.simulator.models.potential.solver.calculators.EikonalSolver;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.logging.Logger;

import java.util.List;

public class EikonalSolverDefaultProvider extends EikonalSolverProvider {

	private  static Logger logger = Logger.getLogger(EikonalSolverDefaultProvider.class);

	@Override
	public EikonalSolver provide(Domain domain, int targetId, List<VShape> targetShapes, AttributesAgent attributesPedestrian, AttributesFloorField attributesPotential) {

		EikonalSolver eikonalSolver = buildBase(domain, targetId, targetShapes, attributesPedestrian, attributesPotential);

		long ms = System.currentTimeMillis();
		eikonalSolver.solve();
		logger.info("floor field initialization time:" + (System.currentTimeMillis() - ms + "[ms]"));

		return eikonalSolver;
	}
}
