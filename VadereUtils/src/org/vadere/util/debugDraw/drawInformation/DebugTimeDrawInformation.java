package org.vadere.util.debugDraw.drawInformation;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.debugDraw.DebugDrawShapeTimeType;

import java.awt.*;

public interface DebugTimeDrawInformation extends DebugDrawInformation {
    DebugTimeDrawInformation withColor(@NotNull Color color);
    DebugTimeDrawInformation forSeconds(double seconds, DebugDrawShapeTimeType timeType);
}
