package org.vadere.util.debugDraw;
import org.vadere.util.debugDraw.drawMethods.*;
import org.vadere.util.debugDraw.drawMethods.interfaces.DrawMethods;
import org.vadere.util.debugDraw.drawMethods.interfaces.TimeDrawMethods;

public class DebugDraw {
    private DebugDraw() {}

    private static final DebugDrawMethodsImpl simulation = new DebugDrawMethodsImpl();
    private static final DebugDrawMethodsImpl postVisualization = new DebugDrawMethodsImpl();
    private static final DebugDrawMethodsImpl topographyCreator = new DebugDrawMethodsImpl();

    public static TimeDrawMethods simulation() {
        return simulation;
    }
    public static TimeDrawMethods postVisualization() {
        return postVisualization;
    }
    public static DrawMethods topographyCreator() {
        return topographyCreator;
    }
}
