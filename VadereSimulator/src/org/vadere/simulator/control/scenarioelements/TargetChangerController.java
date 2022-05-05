package org.vadere.simulator.control.scenarioelements;

import org.vadere.simulator.control.scenarioelements.targetchanger.TargetChangerAlgorithm;
import org.vadere.simulator.models.groups.GroupIterator;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.DynamicElement;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.TargetChanger;
import org.vadere.state.scenario.TargetChangerListener;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.logging.Logger;

import java.awt.geom.Rectangle2D;
import java.util.*;

/**
 * Change target id of an agent which enters the corresponding {@link TargetChanger} area.
 * <p>
 * {@link TargetChanger}'s attributes contain two important parameters to control the changing behavior:
 * <ul>
 *     <li>
 *         "changeTargetProbability": This defines how many percent of the agents,
 *         who enter the area, should change their target.
 *     </li>
 *     <li>
 *         If "nextTargetIsPedestrian == false", assign a new static target.
 *         Otherwise, randomly choose a pedestrian (with given target id) to follow.
 *     </li>
 * </ul>
 */
public class TargetChangerController extends ScenarioElementController {

    // Static Variables
    private static final Logger log = Logger.getLogger(TargetChangerController.class);
    private static final int BINOMIAL_DISTRIBUTION_SUCCESS_VALUE = 1;


    // Member Variables
    public final TargetChanger targetChanger;
    private final GroupIterator groupIterator;
    private Topography topography;
    private Map<Integer, Agent> processedAgents;
    private Random random;
    private TargetChangerAlgorithm changerAlgorithm;

    // Constructors


    public TargetChangerController(Topography topography, TargetChanger targetChanger, Random random, GroupIterator groupIterator) {
        this.changerAlgorithm = TargetChangerAlgorithm.create(targetChanger, topography);
        this.changerAlgorithm.throwExceptionOnInvalidInput(targetChanger);
        this.changerAlgorithm.init(random);

        this.targetChanger = targetChanger;
        this.topography = topography;
        this.processedAgents = new HashMap<>();
        this.random = random;
        this.groupIterator = groupIterator;
    }

    public TargetChangerController(Topography topography, TargetChanger targetChanger, Random random) {
        this(topography, targetChanger, random, null);
    }

    // Getters
    public Map<Integer, Agent> getProcessedAgents() {
        return processedAgents;
    }

    public void update(double simTimeInSec) {
        for (DynamicElement element : getDynamicElementsNearTargetChangerArea()) {

            final Agent agent;
            if (element instanceof Agent) {
                agent = (Agent) element;
            } else {
                log.error("The given object is not a subtype of Agent.");
                continue;
            }

            if (agent.getElementsEncountered().contains(targetChanger.getId()) && !processedAgents.containsKey(agent.getId())) {
                processedAgents.put(agent.getId(), agent);
            }

            if (hasAgentReachedTargetChangerArea(agent) && !processedAgents.containsKey(agent.getId())) {
                logEnteringTimeOfAgent(agent, simTimeInSec);
                notifyListenersTargetChangerAreaReached(agent);
                changerAlgorithm.setAgentTargetList(agent);
                processedAgents.put(agent.getId(), agent);
                agent.elementEncountered(targetChanger);
            }
        }
    }

    private Collection<DynamicElement> getDynamicElementsNearTargetChangerArea() {
        final Rectangle2D areaBounds = targetChanger.getShape().getBounds2D();
        final VPoint areaCenter = new VPoint(areaBounds.getCenterX(), areaBounds.getCenterY());

        final double reachDistance = targetChanger.getAttributes().getReachDistance();
        final double reachRadius = Math.max(areaBounds.getHeight(), areaBounds.getWidth()) + reachDistance;

        List<Pedestrian> pedestriansNearArea = topography.getSpatialMap(Pedestrian.class).getObjects(areaCenter, reachRadius);

        return new LinkedList<>(pedestriansNearArea);
    }

    private boolean hasAgentReachedTargetChangerArea(Agent agent) {
        final double reachDistance = targetChanger.getAttributes().getReachDistance();
        final VPoint agentPosition = agent.getPosition();
        final VShape targetChangerShape = targetChanger.getShape();

        return targetChangerShape.contains(agentPosition)
                || targetChangerShape.distance(agentPosition) < reachDistance;
    }

    private void logEnteringTimeOfAgent(Agent agent, double simTimeInSec) {
        Map<Integer, Double> enteringTimes = targetChanger.getEnteringTimes();
        Integer agentId = agent.getId();

        if (enteringTimes.containsKey(agentId) == false) {
            enteringTimes.put(agentId, simTimeInSec);
        }
    }

    private void notifyListenersTargetChangerAreaReached(final Agent agent) {
        for (TargetChangerListener listener : targetChanger.getTargetChangerListeners()) {
            listener.reachedTargetChanger(targetChanger, agent);
        }
    }

    public TargetChangerAlgorithm getChangerAlgorithm() {
        return changerAlgorithm;
    }

    public void setChangerAlgorithm(TargetChangerAlgorithm changerAlgorithm) {
        this.changerAlgorithm = changerAlgorithm;
    }
}
