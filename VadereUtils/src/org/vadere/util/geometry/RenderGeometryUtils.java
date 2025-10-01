package org.vadere.util.geometry;
import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.shapes.VCircle;
import java.awt.*;
import java.awt.geom.Line2D;

public class RenderGeometryUtils {

    private static final int POLYGON_POINTS = 15;

    public static void render(@NotNull final Shape shape, @NotNull final Graphics2D g2d, Color color) {
        Color tmpColor = g2d.getColor();
        g2d.setColor(color);

        if(shape instanceof Line2D){
            draw(shape, g2d);
        }else{
            fill(shape, g2d);
        }

        g2d.setColor(tmpColor);
    }

    public static void draw(@NotNull final Shape shape, @NotNull final Graphics2D g) {
        if(shape instanceof VCircle) {
            g.draw(GeometryUtils.toPolygon((VCircle) shape, POLYGON_POINTS));
        }
        else {
            g.draw(shape);
        }
    }

    public static void fill(@NotNull final Shape shape, @NotNull final Graphics2D g) {
        if(shape instanceof VCircle) {
            g.fill(GeometryUtils.toPolygon((VCircle) shape, POLYGON_POINTS));
        }
        else {
            g.fill(shape);
        }
    }
}
