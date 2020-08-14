package org.vadere.simulator.models.potential.solver;

import org.vadere.simulator.models.potential.solver.calculators.EikonalSolver;
import org.vadere.simulator.projects.Domain;
import org.vadere.simulator.utils.cache.ICacheObject;
import org.vadere.simulator.utils.cache.ScenarioCache;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.logging.Logger;

import java.util.List;

public class EikonalSolverCacheProvider extends EikonalSolverProvider {

	private  static Logger logger = Logger.getLogger(EikonalSolverCacheProvider.class);

	private final ScenarioCache cache;

	public EikonalSolverCacheProvider(ScenarioCache cache) {
		this.cache = cache;
	}

	@Override
	public EikonalSolver provide(Domain domain, int targetId, List<VShape> targetShapes, AttributesAgent attributesPedestrian, AttributesFloorField attributesPotential) {
		EikonalSolver eikonalSolver = buildBase(domain, targetId, targetShapes, attributesPedestrian, attributesPotential);
		initSolver(eikonalSolver, targetId, targetShapes, attributesPedestrian, attributesPotential);
		return eikonalSolver;
	}

	private void initSolver(EikonalSolver eikonalSolver, int targetId, List<VShape> targetShapes, AttributesAgent attributesPedestrian, AttributesFloorField attributesPotential) {
		/*
		   Initialize floor field. If caching is activate try to read cached version. If no
		   cache is present or the cache loading does not work fall back to standard
		   floor field initialization and log errors.
		 */
		boolean isInitialized = false;
		logger.info("solve floor field");
		if (attributesPotential.isUseCachedFloorField() && cache.isNotEmpty()){
			long ms = System.currentTimeMillis();
			ICacheObject cacheObject = cache.getCacheForTarget(targetId);
			if (cacheObject.readable()){
				isInitialized = eikonalSolver.loadCachedFloorField(cacheObject);
				logger.info("floor field initialization time:" + (System.currentTimeMillis() - ms + "[ms] (cache load time)"));
			} else if (cacheObject.writable()) {
				ms = System.currentTimeMillis();
				logger.infof("No cache found for scenario solve floor field");
				eikonalSolver.solve();
				isInitialized = true;

				logger.info("floor field initialization time:" + (System.currentTimeMillis() - ms + "[ms]"));
				ms = System.currentTimeMillis();
				logger.info("save floor field cache:");
				eikonalSolver.saveFloorFieldToCache(cacheObject);
				logger.info("save floor field cache time:" + (System.currentTimeMillis() - ms + "[ms]"));
			}
		}

		if (!isInitialized){
			long ms = System.currentTimeMillis();
			eikonalSolver.solve();
			logger.info("floor field initialization time:" + (System.currentTimeMillis() - ms + "[ms]"));
		}
	}
}
