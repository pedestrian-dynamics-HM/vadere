package org.vadere.util.debugDraw.drawMethods.interfaces;

import org.vadere.util.debugDraw.drawInformation.DebugDrawInformation;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.Vector2D;

public interface DrawMethods {
    DebugDrawInformation line(VPoint from, Vector2D direction);
    DebugDrawInformation line(VPoint from, Vector2D direction, double length);
    DebugDrawInformation line(VPoint from, VPoint to);

    DebugDrawInformation arrow(VPoint from, Vector2D direction);
    DebugDrawInformation arrow(VPoint from, Vector2D direction, double length);
    DebugDrawInformation arrow(VPoint from, VPoint to);

    DebugDrawInformation circle(VPoint position, double radius);

    void clearShapes();

    void addRenderListener(DebugDrawModelRenderListener renderListener);

    void removeRenderListener(DebugDrawModelRenderListener renderListener);
}
