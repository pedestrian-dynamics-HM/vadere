package org.vadere.util.importSumo.fileParsers.roadNetwork.Edges;

import javax.annotation.Nullable;
import java.util.Set;

public class SumoEdgeTypeDefinition {
    private final Set<SumoAgentType> allowedAgents;
    @Nullable
    private final Double width;
    @Nullable
    private final Double sidewalkWidth;

    private final int numLanes;
    private final double speed;

    public SumoEdgeTypeDefinition(Set<SumoAgentType> allowedAgents, @Nullable Double width, @Nullable Double sidewalkWidth, int numLanes, double speed) {
        this.allowedAgents = allowedAgents;
        this.width = width;
        this.sidewalkWidth = sidewalkWidth;
        this.numLanes = numLanes;
        this.speed = speed;
    }

    public Set<SumoAgentType> getAllowedAgents() {
        return allowedAgents;
    }

    @Nullable
    public Double getSidewalkWidth() {
        return sidewalkWidth;
    }

    public int getNumLanes() {
        return numLanes;
    }

    public double getSpeed() {
        return speed;
    }

    @Nullable
    public Double getWidth() {
        return width;
    }
}
