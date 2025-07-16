package org.vadere.util.debugDraw.drawInformation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.util.debugDraw.DebugDrawShapeTimeType;

import java.awt.*;

public abstract class DebugDrawableBase implements DebugTimeDrawInformation {
    protected Color color = Color.red;
    protected double secondsToShow = Double.MAX_VALUE;
    @Nullable
    private DebugDrawShapeTimeType timeType = DebugDrawShapeTimeType.RealTime;

    public double getRemainingPlayTime() {
        return secondsToShow;
    }

    public Color getColor() {
        return color;
    }

    public void updateRemainingTime(double deltaSimulationTime, double deltaSystemTime) {
        if (timeType == null) {
            return;
        }
        switch (timeType) {
            case SimulationTime :
                secondsToShow -= deltaSimulationTime;
                break;
            case RealTime:
                secondsToShow -= deltaSystemTime;
                break;
        }
    }

    @Override
    public DebugTimeDrawInformation withColor(@NotNull Color color) {
        this.color = color;
        return this;
    }

    @Override
    public DebugTimeDrawInformation forSeconds(double seconds, DebugDrawShapeTimeType timeType){
        this.secondsToShow = seconds;
        this.timeType = timeType;
        return this;
    }

    @Override
    public DebugTimeDrawInformation drawIfTrue(boolean value){
        if(!value){
            this.secondsToShow = -1;
        }

        return this;
    }

    public void draw(Graphics2D g2d){
        if(secondsToShow >= 0){
            drawInternal(g2d);
        }
    }

    protected abstract void drawInternal(Graphics2D g2d);
}
