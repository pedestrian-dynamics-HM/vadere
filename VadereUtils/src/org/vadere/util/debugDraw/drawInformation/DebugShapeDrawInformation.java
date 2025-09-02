package org.vadere.util.debugDraw.drawInformation;

import org.vadere.util.geometry.RenderGeometryUtils;

import java.awt.*;

public class DebugShapeDrawInformation extends DebugDrawableBase implements DebugTimeDrawInformation {
    public static final DebugShapeDrawInformation Empty;
    static
    {
        Empty = new DebugShapeDrawInformation(null);
        Empty.secondsToShow = -1;
    }

    private final Shape shape;

    public DebugShapeDrawInformation(Shape toDraw) {
        this.shape = toDraw;
    }

    @Override
    public void drawInternal(Graphics2D g2d) {
        RenderGeometryUtils.render(shape, g2d, getColor());
    }
}
