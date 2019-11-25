package org.vadere.state.scenario;

public interface SpawnDistribution {

    int getSpawnNumber(double simTimeInSec);
    double getNextSpawnTime(double simTimeInSec);

}
