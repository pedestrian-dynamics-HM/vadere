package org.vadere.state.scenario;

public interface SpawnDistribution {

    int getSpawnNumber(double timeCurrentEvent);
    double getNextSpawnTime(double timeCurrentEvent);
    int getRemainingSpawnAgents();
    void setRemainingSpawnAgents(int remainingAgents);

}
