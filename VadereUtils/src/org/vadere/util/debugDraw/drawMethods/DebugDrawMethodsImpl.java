package org.vadere.util.debugDraw.drawMethods;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.util.debugDraw.drawInformation.DebugDrawableBase;
import org.vadere.util.debugDraw.drawInformation.DebugShapeDrawInformation;
import org.vadere.util.debugDraw.drawInformation.DebugTimeDrawInformation;
import org.vadere.util.debugDraw.drawInformation.MultiShapeDrawInformation;
import org.vadere.util.debugDraw.drawMethods.interfaces.DebugDrawModelRenderListener;
import org.vadere.util.debugDraw.drawMethods.interfaces.DebugRenderTarget;
import org.vadere.util.debugDraw.drawMethods.interfaces.TimeDrawMethods;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.Vector2D;
import org.vadere.util.math.InterpolationUtil;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class DebugDrawMethodsImpl implements TimeDrawMethods, DebugRenderTarget {
    private static final float ARROW_HEAD_LENGTH_PERCENTAGE = .1f;
    private final List<DebugDrawableBase> drawables = new ArrayList<>();
    private final List<DebugDrawModelRenderListener> renderListeners = new ArrayList<>();
    private double lastDrawSystemTimeSeconds = -1;
    private double lastDrawSimulationTimeSeconds;

    @Override
    public DebugTimeDrawInformation line(VPoint from, Vector2D direction){
        VPoint to = new VPoint(from.getX(), from.getY());
        to = to.add(direction.getX(), direction.getY());

        return line(from, to);
    }

    @Override
    public DebugTimeDrawInformation line(VPoint from, Vector2D direction, double length){
        VPoint to = new VPoint(from.getX(), from.getY());
        Vector2D normalizedDirection = direction.normalize();
        to = to.add(normalizedDirection.getX() * length, normalizedDirection.getY() * length);

        return line(from, to);
    }

    @Override
    public DebugTimeDrawInformation line(VPoint from, VPoint to){
        VLine line = new VLine(from, to);
        DebugShapeDrawInformation drawInformation = new DebugShapeDrawInformation(line);

        enqueueDrawShape(drawInformation);
        return drawInformation;
    }

    @Override
    public DebugTimeDrawInformation arrow(VPoint from, Vector2D direction){
        VPoint to = new VPoint(from.getX(), from.getY());
        to = to.add(direction.getX(), direction.getY());

        return arrow(from, to);
    }

    @Override
    public DebugTimeDrawInformation arrow(VPoint from, Vector2D direction, double length){
        VPoint to = new VPoint(from.getX(), from.getY());
        Vector2D normalizedDirection = direction.normalize();
        to = to.add(normalizedDirection.getX() * length, normalizedDirection.getY() * length);

        return arrow(from, to);
    }

    @Override
    public DebugTimeDrawInformation arrow(VPoint from, VPoint to){
        VLine shaft = new VLine(from, to);

        ArrowLines arrowLines = CalculateArrowHeadLines(shaft);
        if(arrowLines == null){
            DebugShapeDrawInformation shaftOnlyDrawInformation = new DebugShapeDrawInformation(shaft);
            enqueueDrawShape(shaftOnlyDrawInformation);
            return shaftOnlyDrawInformation;
        }

        MultiShapeDrawInformation drawInformation = new MultiShapeDrawInformation(
                shaft,
                arrowLines.head.getLeft(),
                arrowLines.head.getRight(),
                arrowLines.tail
                );
        enqueueDrawShape(drawInformation);
        return drawInformation;
    }

    @Nullable
    protected static ArrowLines CalculateArrowHeadLines(VLine shaft) {
        VPoint from = shaft.getVPoint1();
        VPoint to = shaft.getVPoint2();
        Vector2D shaftVector = shaft.asVector();

        double shaftLength =  shaftVector.getLength();
        if(shaftLength < GeometryUtils.DOUBLE_EPS){
            return null;
        }

        Vector2D normalized = shaftVector.divide(shaftLength);
        VPoint arrowHeadStartPoint = InterpolationUtil.lerp(to, from, ARROW_HEAD_LENGTH_PERCENTAGE);

        double arrowHeadLength = shaftLength * ARROW_HEAD_LENGTH_PERCENTAGE;
        Vector2D orthogonal = new Vector2D(normalized.getY(), -normalized.getX());
        VPoint leftArrowEndPoint = arrowHeadStartPoint.add(orthogonal.multiply(-arrowHeadLength/2));
        VPoint rightArrowEndPoint = arrowHeadStartPoint.add(orthogonal.multiply(arrowHeadLength/2));

        VLine leftArrowHeadLine = new VLine(to, leftArrowEndPoint);
        VLine rightArrowHeadLine = new VLine(to, rightArrowEndPoint);
        Pair<VLine, VLine> headLines = Pair.of(leftArrowHeadLine, rightArrowHeadLine);

        double tailLength = arrowHeadLength / 2;
        VPoint tailFrom = from.add(orthogonal.multiply(-tailLength / 2));
        VPoint tailTo = from.add(orthogonal.multiply(tailLength / 2));
        VLine tail = new VLine(tailFrom, tailTo);

        return new ArrowLines(headLines, tail);
    }

    protected record ArrowLines(Pair<VLine, VLine> head, VLine tail) {}

    @Override
    public DebugTimeDrawInformation circle(VPoint position, double radius){
        VCircle circle = new VCircle(position, radius);
        DebugShapeDrawInformation drawInformation = new DebugShapeDrawInformation(circle);

        enqueueDrawShape(drawInformation);
        return drawInformation;
    }



    protected void enqueueDrawShape(DebugDrawableBase drawInformation) {
        synchronized (drawables){
            drawables.add(drawInformation);
        }
    }

    @Override
    public void clearShapes(){
        synchronized (drawables) {
            drawables.clear();
        }
    }

    /**
     * Adds a listener that is called each time the renderer draws a new frame.
     * The provided {@link DebugDrawImmediate} allows drawing directly onto the frame's {@code Graphics2D}.
     */
    @Override
    public void addRenderListener(DebugDrawModelRenderListener renderListener){
        synchronized (renderListeners){
            renderListeners.add(renderListener);
        }
    }

    @Override
    public void removeRenderListener(DebugDrawModelRenderListener renderListener){
        synchronized (renderListeners){
            renderListeners.remove(renderListener);
        }
    }

    public void updateTime(double simulationTimeSeconds) {
        double systemTimeSeconds = getCurrentSystemTimeSeconds();

        double deltaSimulationTime = simulationTimeSeconds - lastDrawSimulationTimeSeconds;
        double deltaSystemTime = lastDrawSystemTimeSeconds < 0
                ? 0
                : systemTimeSeconds - lastDrawSystemTimeSeconds;

        synchronized (drawables) {
            for (int i = 0; i < drawables.size(); i++) {
                DebugDrawableBase shape = drawables.get(i);
                shape.updateRemainingTime(deltaSimulationTime, deltaSystemTime);

                if(shape.getRemainingPlayTime() <= 0){
                    drawables.remove(i);
                    i--;
                }
            }
        }

        lastDrawSystemTimeSeconds = systemTimeSeconds;
        lastDrawSimulationTimeSeconds = simulationTimeSeconds;
    }

    private static double getCurrentSystemTimeSeconds() {
        return System.currentTimeMillis() / 1000.0;
    }

    public void drawToGraphics(@NotNull Graphics2D g2d, float lineWidth) {
        final Color tmpColor = g2d.getColor();
        final Stroke tmpStroke = g2d.getStroke();
        g2d.setStroke(new BasicStroke(lineWidth));

        synchronized (drawables) {
            for (DebugDrawableBase drawable : drawables) {
                drawable.draw(g2d);
            }
        }

        g2d.setStroke(tmpStroke);
        g2d.setColor(tmpColor);
    }

    public void informModelRenderListeners(Object model, @NotNull Graphics2D g2d, float lineWidth) {
        if(renderListeners.isEmpty()){
            return;
        }

        final Color tmpColor = g2d.getColor();
        final Stroke tmpStroke = g2d.getStroke();
        g2d.setStroke(new BasicStroke(lineWidth));

        synchronized (renderListeners){
            for (DebugDrawModelRenderListener renderListener : renderListeners) {
                renderListener.OnDrawModel(model, new DebugDrawImmediate(g2d));
            }
        }

        g2d.setStroke(tmpStroke);
        g2d.setColor(tmpColor);
    }
}
