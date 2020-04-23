package org.vadere.simulator.control.scenarioelements;

import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.scenario.*;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.logging.Logger;

import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Change target id of an agent which enters the corresponding {@link TargetChanger} area.
 *
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
public class TargetChangerController {

    // Static Variables
    private static final Logger log = Logger.getLogger(TargetChangerController.class);
    private static final int BINOMIAL_DISTRIBUTION_SUCCESS_VALUE = 1;


    // Member Variables
    public final TargetChanger targetChanger;
    private Topography topography;
    private Map<Integer, Agent> processedAgents;

    private LinkedList<Double> probabilitiesToChangeTarget;
    private int seed;
    private Random random;
    private LinkedList<BinomialDistribution> binomialDistributions;

    // Constructors
    public TargetChangerController(Topography topography, TargetChanger targetChanger, Random random) {
        throwExceptionOnInvalidInput(targetChanger);

        this.targetChanger = targetChanger;
        this.topography = topography;
        this.processedAgents = new HashMap<>();

        this.random = random;
        this.probabilitiesToChangeTarget = targetChanger.getAttributes().getProbabilitiesToChangeTarget();
        this.seed = random.nextInt();
        this.binomialDistributions = getBinomialDistributions();
    }

    private void throwExceptionOnInvalidInput(TargetChanger targetChanger) {
        int totalTargets = targetChanger.getAttributes().getNextTarget().size();
        int totalProbabilities = targetChanger.getAttributes().getProbabilitiesToChangeTarget().size();

        boolean inputIsValid = (totalProbabilities == 1) || (totalProbabilities == totalTargets);

        if ( inputIsValid == false) {
            throw new IllegalArgumentException("The size of \"probabilitiesToChangeTarget\" must be 1 or equal to nextTarget.");
        }
    }

    private LinkedList<BinomialDistribution> getBinomialDistributions() {
        LinkedList<BinomialDistribution> binomialDistributions = new LinkedList<>();

        JDKRandomGenerator randomGenerator = new JDKRandomGenerator();
        randomGenerator.setSeed(seed);

        for (Double probability : probabilitiesToChangeTarget) {
            binomialDistributions.add(new BinomialDistribution(randomGenerator, BINOMIAL_DISTRIBUTION_SUCCESS_VALUE, probability));
        }

        return binomialDistributions;
    }


    // Getters
    public Map<Integer, Agent> getProcessedAgents() {
        return processedAgents;
    }


    // Public Methods
    public void update(double simTimeInSec) {
        for (DynamicElement element : getDynamicElementsNearTargetChangerArea()) {

            final Agent agent;
            if (element instanceof Agent) {
                agent = (Agent) element;
            } else {
                log.error("The given object is not a subtype of Agent.");
                continue;
            }

            if (hasAgentReachedTargetChangerArea(agent) && processedAgents.containsKey(agent.getId()) == false) {
                logEnteringTimeOfAgent(agent, simTimeInSec);
                setAgentTargetList(agent);
                notifyListenersTargetChangerAreaReached(agent);
                processedAgents.put(agent.getId(), agent);
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

    private void logEnteringTimeOfAgent(Agent agent, double simTimeInSec) {
        Map<Integer, Double> enteringTimes = targetChanger.getEnteringTimes();
        Integer agentId = agent.getId();

        if (enteringTimes.containsKey(agentId) == false) {
            enteringTimes.put(agentId, simTimeInSec);
        }
    }

    private void setAgentTargetList(Agent agent) {
        LinkedList<Integer> nextTargets = getNextTargets();

        if (nextTargets.size() > 0) {
            if (targetChanger.getAttributes().isNextTargetIsPedestrian()) {
                useDynamicTargetForAgentOrUseStaticAsFallback(agent, nextTargets);
            } else {
                useStaticTargetForAgent(agent,nextTargets);
            }
        }
    }

    private LinkedList<Integer> getNextTargets() {

        LinkedList<Integer> nextTargets = new LinkedList<>();

        for (int i = 0; i < binomialDistributions.size(); i++) {

            BinomialDistribution binomialDistribution = binomialDistributions.get(i);
            int binomialDistributionSample = binomialDistribution.sample();

            if (binomialDistributionSample == BINOMIAL_DISTRIBUTION_SUCCESS_VALUE) {

                // If only one probability is given, just take the target list which was defined by the user.
                // Otherwise, create a list of new targets (if we saw a "success" value while sampling.
                if (targetChanger.getAttributes().getProbabilitiesToChangeTarget().size() == 1) {
                    nextTargets = targetChanger.getAttributes().getNextTarget();
                    break;
                } else {
                    nextTargets.add(targetChanger.getAttributes().getNextTarget().get(i));
                }
            }
        }

        return nextTargets;
    }

    private void useDynamicTargetForAgentOrUseStaticAsFallback(Agent agent, LinkedList<Integer> keepTargets) {
        int nextTarget = (targetChanger.getAttributes().getNextTarget().size() > 0)
                ? targetChanger.getAttributes().getNextTarget().get(0)
                : Attributes.ID_NOT_SET;

        Collection<Pedestrian> allPedestrians = topography.getElements(Pedestrian.class);
        List<Pedestrian> pedsWithCorrectTargetId = allPedestrians.stream()
                .filter(pedestrian -> pedestrian.getTargets().contains(nextTarget))
                .collect(Collectors.toList());

        if (pedsWithCorrectTargetId.size() > 0) {
            // Try to use a pedestrian which has already some followers
            // to avoid calculating multiple dynamic floor fields.
            List<Pedestrian> pedsWithFollowers = pedsWithCorrectTargetId.stream()
                    .filter(pedestrian -> pedestrian.getFollowers().isEmpty() == false)
                    .collect(Collectors.toList());

            Pedestrian pedToFollow = (pedsWithFollowers.isEmpty()) ? pedsWithCorrectTargetId.get(0) : pedsWithFollowers.get(0);
            agentFollowsOtherPedestrian(agent, pedToFollow);
        } else {

            useStaticTargetForAgent(agent, keepTargets);
        }
    }

    private void agentFollowsOtherPedestrian(Agent agent, Pedestrian pedToFollow) {
        // Create the necessary TargetPedestrian wrapper object.
        // The simulation loop creates the corresponding controller objects
        // in the next simulation loop based on the exisiting targets in the topography.
        TargetPedestrian targetPedestrian = new TargetPedestrian(pedToFollow);
        topography.addTarget(targetPedestrian);

        // Make "agent" a follower of "pedToFollow".
        agent.setSingleTarget(targetPedestrian.getId(), true);
        pedToFollow.getFollowers().add(agent);
    }

    private void useStaticTargetForAgent(Agent agent, LinkedList<Integer> nextTargets) {
        agent.setTargets(nextTargets);
        agent.setNextTargetListIndex(0);
        agent.setIsCurrentTargetAnAgent(false);
    }

    private void notifyListenersTargetChangerAreaReached(final Agent agent) {
        for (TargetChangerListener listener : targetChanger.getTargetChangerListeners()) {
            listener.reachedTargetChanger(targetChanger, agent);
        }
    }

}
