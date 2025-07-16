package org.vadere.util.debugDraw.drawInformation;

import org.vadere.util.geometry.RenderGeometryUtils;

import java.awt.*;

public class MultiShapeDrawInformation extends DebugDrawableBase implements DebugTimeDrawInformation {
    private final Shape[] shapes;

    public MultiShapeDrawInformation(Shape... toDraw) {
        this.shapes = toDraw;
    }

    @Override
    public void drawInternal(Graphics2D g2d) {
        for (Shape shape : shapes) {
            RenderGeometryUtils.render(shape, g2d, getColor());
        }
    }
}
