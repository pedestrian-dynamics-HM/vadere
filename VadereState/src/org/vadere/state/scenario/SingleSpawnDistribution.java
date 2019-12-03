package org.vadere.state.scenario;


public class SingleSpawnDistribution implements SpawnDistribution {

    private int spawnNumber;
    private double spawnTime;
    private int outstandingAgents;

    public SingleSpawnDistribution(int spawnNumber, double spawnTime){
        this.spawnNumber = spawnNumber;
        this.spawnTime = spawnTime;
        this.outstandingAgents = 0;
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
    public void setOutstandingAgents(int outstandingAgents){
        this.outstandingAgents = outstandingAgents;
    }

    @Override
    public int getOutstandingSpawnNumber(){
        return this.outstandingAgents;
    }

}
