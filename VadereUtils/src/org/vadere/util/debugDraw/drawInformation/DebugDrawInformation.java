package org.vadere.util.debugDraw.drawInformation;

import org.jetbrains.annotations.NotNull;

import java.awt.*;

public interface DebugDrawInformation {
    DebugDrawInformation withColor(@NotNull Color color);
    DebugDrawInformation drawIfTrue(boolean value);
}
