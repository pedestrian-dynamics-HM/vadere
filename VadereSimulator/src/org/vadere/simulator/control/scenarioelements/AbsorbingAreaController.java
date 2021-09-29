package org.vadere.simulator.control.scenarioelements;

import org.vadere.state.scenario.*;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.logging.Logger;

import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Absorb agents which enter the given {@link AbsorbingArea}.
 * <p>
 * Take following attributes into account when absorbing agents:
 * - {@link org.vadere.state.attributes.scenario.AttributesAbsorbingArea#getDeletionDistance()}
 * <p>
 */
public class AbsorbingAreaController extends ScenarioElementController {

    // Variables
    private static final Logger log = Logger.getLogger(AbsorbingAreaController.class);

    public final AbsorbingArea absorbingArea;
    private Topography topography;

    // Constructors
    public AbsorbingAreaController(Topography topography, AbsorbingArea absorbingArea) {
        this.absorbingArea = absorbingArea;
        this.topography = topography;
    }

    // Other Methods
    public void update(double simTimeInSec) {
        for (DynamicElement element : getDynamicElementsNearAbsorbingArea()) {

            final Agent agent;
            if (element instanceof Agent) {
                agent = (Agent) element;
            } else {
                log.error("The given object is not a subtype of Agent.");
                continue;
            }

            if (hasAgentReachedAbsorbingArea(agent)) {
                notifyListenersAbsorbingAreaReached(agent);
                topography.removeElement(agent);
            }
        }
    }

    private Collection<DynamicElement> getDynamicElementsNearAbsorbingArea() {
        final Rectangle2D absorbingAreaBounds = absorbingArea.getShape().getBounds2D();
        final VPoint centerOfAbsorbingArea = new VPoint(absorbingAreaBounds.getCenterX(), absorbingAreaBounds.getCenterY());

        final double deletionDistance = absorbingArea.getAttributes().getDeletionDistance();
        final double deletionRadius = Math.max(absorbingAreaBounds.getHeight(), absorbingAreaBounds.getWidth()) + deletionDistance;

        final Collection<DynamicElement> elementsNearAbsorbingArea = new LinkedList<>();

        List<Pedestrian> pedestriansNearAbsorbingArea = topography.getSpatialMap(Pedestrian.class).getObjects(centerOfAbsorbingArea, deletionRadius);
        elementsNearAbsorbingArea.addAll(pedestriansNearAbsorbingArea);

        return elementsNearAbsorbingArea;
    }

    private boolean hasAgentReachedAbsorbingArea(Agent agent) {
        final double deletionDistance = absorbingArea.getAttributes().getDeletionDistance();
        final VPoint agentPosition = agent.getPosition();
        final VShape absorbingAreaShape = absorbingArea.getShape();

        return absorbingAreaShape.contains(agentPosition)
                || absorbingAreaShape.distance(agentPosition) < deletionDistance;
    }

    private void notifyListenersAbsorbingAreaReached(final Agent agent) {
        for (AbsorbingAreaListener listener : absorbingArea.getAbsorbingAreaListeners()) {
            listener.reachedAbsorbingArea(absorbingArea, agent);
        }
    }

}
