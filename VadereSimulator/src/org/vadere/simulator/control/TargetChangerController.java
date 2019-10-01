package org.vadere.simulator.control;

import org.vadere.state.scenario.*;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.logging.Logger;

import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Change target id of an agent which enters the given {@link TargetChanger} area.
 *
 * TargetChanger's attributes contain two important parameters to control the change behavior:
 * - changeTargetProbability
 * - nextTargetIsPedestrian
 *
 * "changeTargetProbability" defines how many percent of the agents, who enter the area,
 * should change its target. If "nextTargetIsPedestrian == false", assign a new
 * static target. Otherwise, randomly choose a pedestrian (with given target id) to follow.
 */
public class TargetChangerController {

    // Variables
    private static final Logger log = Logger.getLogger(TargetChangerController.class);

    public final TargetChanger targetChanger;
    private Topography topography;

    // Constructors
    public TargetChangerController(Topography topography, TargetChanger targetChanger) {
        this.targetChanger = targetChanger;
        this.topography = topography;
    }

    // Other Methods
    public void update(double simTimeInSec) {
        for (DynamicElement element : getDynamicElementsNearTargetChangerArea()) {

            final Agent agent;
            if (element instanceof Agent) {
                agent = (Agent) element;
            } else {
                log.error("The given object is not a subtype of Agent.");
                continue;
            }

            if (hasAgentReachedTargetChangerArea(agent)) {
                notifyListenersTargetChangerAreaReached(agent);
                // TODO: Implement logic in "update()" method.

            }
        }
    }

    private Collection<DynamicElement> getDynamicElementsNearTargetChangerArea() {
        final Rectangle2D areaBounds = targetChanger.getShape().getBounds2D();
        final VPoint areaCenter = new VPoint(areaBounds.getCenterX(), areaBounds.getCenterY());

        final double reachDistance = targetChanger.getAttributes().getReachDistance();
        final double reachRadius = Math.max(areaBounds.getHeight(), areaBounds.getWidth()) + reachDistance;

        final Collection<DynamicElement> elementsNearArea = new LinkedList<>();

        List<Pedestrian> pedestriansNearArea = topography.getSpatialMap(Pedestrian.class).getObjects(areaCenter, reachRadius);
        elementsNearArea.addAll(pedestriansNearArea);

        return elementsNearArea;
    }

    private boolean hasAgentReachedTargetChangerArea(Agent agent) {
        final double reachDistance = targetChanger.getAttributes().getReachDistance();
        final VPoint agentPosition = agent.getPosition();
        final VShape targetChangerShape = targetChanger.getShape();

        return targetChangerShape.contains(agentPosition)
                || targetChangerShape.distance(agentPosition) < reachDistance;
    }

    private void notifyListenersTargetChangerAreaReached(final Agent agent) {
        for (TargetChangerListener listener : targetChanger.getTargetChangerListeners()) {
            listener.reachedTargetChanger(targetChanger, agent);
        }
    }

}
