package org.vadere.state.attributes.spawner;

import org.vadere.util.reflection.VadereAttribute;

import java.util.List;

public class AttributesLerpSpawner extends AttributesSpawner {
    @VadereAttribute
    Double spawnFrequency;
    @VadereAttribute
    List<Double> xValues;
    @VadereAttribute
    List<Double> yValues;


    @Override
    public int getEventElementCount() {
        return 0;
    }

    @Override
    public void setEventElementCount(Integer eventElementCount) {

    }

    public Double getSpawnFrequency() {
        return spawnFrequency;
    }

    public void setSpawnFrequency(Double spawnFrequency) {
        this.spawnFrequency = spawnFrequency;
    }

    public List<Double> getxValues() {
        return xValues;
    }

    public void setxValues(List<Double> xValues) {
        this.xValues = xValues;
    }

    public List<Double> getyValues() {
        return yValues;
    }

    public void setyValues(List<Double> yValues) {
        this.yValues = yValues;
    }
}
