package org.vadere.util.debugDraw.drawMethods.interfaces;
import org.vadere.util.debugDraw.drawInformation.DebugTimeDrawInformation;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.Vector2D;

/**
 * Same as DrawMethods overriding the return type with DebugTimeDrawInformation
 */
public interface TimeDrawMethods extends DrawMethods {
    DebugTimeDrawInformation line(VPoint from, Vector2D direction);
    DebugTimeDrawInformation line(VPoint from, VPoint to);

    DebugTimeDrawInformation arrow(VPoint from, Vector2D direction);
    DebugTimeDrawInformation arrow(VPoint from, Vector2D direction, double length);
    DebugTimeDrawInformation arrow(VPoint from, VPoint to);

    DebugTimeDrawInformation circle(VPoint position, double radius);
}
