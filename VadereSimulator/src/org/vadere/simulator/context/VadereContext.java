package org.vadere.simulator.context;

import org.vadere.simulator.models.potential.solver.EikonalSolverDefaultProvider;
import org.vadere.simulator.models.potential.solver.EikonalSolverProvider;
import org.vadere.simulator.utils.random.VRandom;
import org.vadere.state.scenario.Topography;
import org.vadere.util.logging.Logger;

import java.util.concurrent.ConcurrentHashMap;

public class VadereContext extends Context {
	protected static Logger logger = Logger.getLogger(VadereContext.class);

	private static ConcurrentHashMap<String, VadereContext> contextMap = new ConcurrentHashMap<>();


	public synchronized static VadereContext get(final Topography topography){
		if (!contextMap.containsKey(topography.getContextId())){
			logger.warnf("Deprecation warning: no VadereContext found for id: '%s'. Creating new Context. Initialize Context before first usage.", topography.getContextId());
		}
		return contextMap.getOrDefault(topography.getContextId(), new VadereContext());
	}

	public synchronized static void add(String contextId, VadereContext ctx){
		contextMap.put(contextId, ctx);
	}

	public synchronized static VadereContext create(String contextId){
		VadereContext ctx = new VadereContext();
		contextMap.put(contextId, ctx);
		return ctx;
	}

	public synchronized static void remove(String contextId){
		contextMap.remove(contextId);
	}

	public synchronized static void clear(){
		contextMap.clear();
	}

	private EikonalSolverProvider eikonalSolverProvider;

	private VadereContext() {
		super();
		setEikonalSolverProvider(new EikonalSolverDefaultProvider());
	}

	public void setEikonalSolverProvider(EikonalSolverProvider eikonalSolverProvider) {
		put(EikonalSolverProvider.class.getCanonicalName(), eikonalSolverProvider);
	}

	public EikonalSolverProvider getEikonalSolverProvider() {
		return (EikonalSolverProvider) get(EikonalSolverProvider.class.getCanonicalName());
	}


	public void setVRandom(VRandom random){
		put(VRandom.class.getCanonicalName(), random);
	}

	public VRandom getVRandom(){
		return (VRandom)get(VRandom.class.getCanonicalName());
	}
}
