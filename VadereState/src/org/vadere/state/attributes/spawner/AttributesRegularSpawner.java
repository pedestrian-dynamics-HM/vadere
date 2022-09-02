package org.vadere.state.attributes.spawner;

public class AttributesRegularSpawner extends AttributesSpawner{
    protected Integer spawnNumber;

    @Override
    public int getSpawnNumber() {
        return spawnNumber;
    }
    @Override
    public void setSpawnNumber(int spawnNumber) {
        checkSealed();
        this.spawnNumber = spawnNumber;
    }

}
