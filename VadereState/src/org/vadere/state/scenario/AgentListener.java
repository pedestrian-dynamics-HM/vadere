package org.vadere.state.scenario;

import java.util.LinkedList;

public interface AgentListener {

    void agentTargetsChanged(LinkedList<Integer> targetIds, int agentId);

    void agentNextTargetSet(double nextSpeed, int agentId);

    void agentElementEncountered(ScenarioElement element, int agentId);
}
