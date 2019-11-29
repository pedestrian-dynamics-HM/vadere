package org.vadere.state.scenario;

public interface SpawnDistribution {

    int getSpawnNumber(double timeCurrentEvent);
    int getOutstandingSpawnNumber();
    double getNextSpawnTime(double timeCurrentEvent);
    void setOutstandingAgents(int outstandingAgents);

}
