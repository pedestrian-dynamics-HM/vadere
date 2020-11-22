package org.vadere.state.scenario;


public class SingleSpawnDistribution implements SpawnDistribution {

    private int spawnNumber;
    private double spawnTime;
    private int remainingAgents;

    public SingleSpawnDistribution(int spawnNumber, double spawnTime){
        this.spawnNumber = spawnNumber;
        this.spawnTime = spawnTime;
        this.remainingAgents = 0;
    }

    @Override
    public int getSpawnNumber(double timeCurrentEvent){
        return this.spawnNumber;
    }

    @Override
    public double getNextSpawnTime(double timeCurrentEvent) {
        return spawnTime;
    }

    @Override
    public void setRemainingSpawnAgents(int remainingAgents){
        this.remainingAgents = remainingAgents;
    }

    @Override
    public int getRemainingSpawnAgents(){
        return this.remainingAgents;
    }

}
