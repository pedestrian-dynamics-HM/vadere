package org.vadere.simulator.context;

import java.util.concurrent.ConcurrentHashMap;
import org.vadere.simulator.models.potential.solver.EikonalSolverDefaultProvider;
import org.vadere.simulator.models.potential.solver.EikonalSolverProvider;
import org.vadere.state.scenario.Topography;

public class VadereContext extends Context {

  public static final String TREE_NODE_CTX = "gui.treeNodeContext";
  private static ConcurrentHashMap<String, VadereContext> contextMap = new ConcurrentHashMap<>();

  public static synchronized VadereContext getCtx(final Topography topography) {
    return contextMap.getOrDefault(topography.getContextId(), new VadereContext());
  }

  public static synchronized VadereContext getCtx(final String key) {
    return contextMap.getOrDefault(key, new VadereContext());
  }

  public static synchronized void add(String contextId, VadereContext ctx) {
    contextMap.put(contextId, ctx);
  }

  public static synchronized void remove(String contextId) {
    contextMap.remove(contextId);
  }

  public static synchronized void clear() {
    contextMap.clear();
  }

  private EikonalSolverProvider eikonalSolverProvider;

  public VadereContext() {
    this.eikonalSolverProvider = new EikonalSolverDefaultProvider();
  }

  public EikonalSolverProvider getEikonalSolverProvider() {
    return eikonalSolverProvider;
  }

  public void setEikonalSolverProvider(EikonalSolverProvider eikonalSolverProvider) {
    this.eikonalSolverProvider = eikonalSolverProvider;
  }
}
