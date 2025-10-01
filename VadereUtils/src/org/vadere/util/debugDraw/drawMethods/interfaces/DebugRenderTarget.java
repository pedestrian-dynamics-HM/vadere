package org.vadere.util.debugDraw.drawMethods.interfaces;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public interface DebugRenderTarget {
    void updateTime(double simulationTimeSeconds);

    void drawToGraphics(
            @NotNull Graphics2D g2d,
            float lineWidth
    );

    void informModelRenderListeners(
            Object model,
            @NotNull Graphics2D graphics2D,
            float lineWidth);
}
