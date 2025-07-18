package org.vadere.util.debugDraw.drawMethods;
import org.vadere.util.debugDraw.drawMethods.interfaces.DebugDrawModelRenderListener;
import org.vadere.util.geometry.RenderGeometryUtils;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.Vector2D;

import java.awt.*;

/**
 * Used in {@link DebugDrawMethodsImpl#addRenderListener(DebugDrawModelRenderListener) DebugDrawModelRenderListener}
 * to draw directly onto the Graphics2D object from a renderer. Unlike other DebugDraw methods, this does not retain any state.
 * Therefore, the information must be rendered anew each time the renderer draws a frame.
 */
public record DebugDrawImmediate(Graphics2D graphics2D) {

    public void line(VPoint from, Vector2D direction, Color color) {
        VPoint to = new VPoint(from.getX(), from.getY());
        to = to.add(direction.getX(), direction.getY());

        line(from, to, color);
    }

    public void line(VPoint from, Vector2D direction, double length, Color color) {
        VPoint to = new VPoint(from.getX(), from.getY());
        Vector2D normalizedDirection = direction.normalize();
        to = to.add(normalizedDirection.getX() * length, normalizedDirection.getY() * length);

        line(from, to, color);
    }

    public void line(VPoint from, VPoint to, Color color) {
        VLine line = new VLine(from, to);
        RenderGeometryUtils.render(line, graphics2D, color);
    }

    public void circle(VPoint position, double radius, Color color) {
        VCircle circle = new VCircle(position, radius);
        RenderGeometryUtils.render(circle, graphics2D, color);
    }
}
