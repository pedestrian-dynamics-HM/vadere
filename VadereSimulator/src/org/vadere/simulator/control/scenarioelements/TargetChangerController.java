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
    private LinkedList<Double> probabilitiesToChangeTarget;
    private Topography topography;
    private Map<Integer, Agent> processedAgents;
    int seed;
    //BinomialDistribution binomialDistribution;
    private Random random;
    private LinkedList<BinomialDistribution> binomialDistributions = null;

    // Constructors
    public TargetChangerController(Topography topography, TargetChanger targetChanger, Random random) {
        this.targetChanger = targetChanger;
        this.topography = topography;
        this.processedAgents = new HashMap<>();
        this.random = random;


        this.probabilitiesToChangeTarget = targetChanger.getAttributes().getProbabilitiesToChangeTarget();

        seed = random.nextInt();
        binomialDistributions = getBinomialDistributions();
    }


    // Getters
    public Map<Integer, Agent> getProcessedAgents() {
        return processedAgents;
    }

    public LinkedList<BinomialDistribution> getBinomialDistributions(){

        if (binomialDistributions == null) {

            LinkedList<BinomialDistribution> binomialDistributionsL = new LinkedList<>();

            JDKRandomGenerator randomGenerator = new JDKRandomGenerator();
            randomGenerator.setSeed(this.seed);

            for (Double probability : this.probabilitiesToChangeTarget) {
                binomialDistributionsL.add(new BinomialDistribution(randomGenerator, BINOMIAL_DISTRIBUTION_SUCCESS_VALUE, probability));
            }
            binomialDistributions = binomialDistributionsL;
        }

        return binomialDistributions;
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


                LinkedList<Integer> keepTargets = getFilteredTargetList();

                int aa  = keepTargets.size();

                if (keepTargets.size() > 0) {
                    if (targetChanger.getAttributes().isNextTargetIsPedestrian()) {
                        useDynamicTargetForAgentOrUseStaticAsFallback(agent, keepTargets);
                    } else {
                        useStaticTargetForAgent(agent,keepTargets);
                    }
                }

                notifyListenersTargetChangerAreaReached(agent);

                processedAgents.put(agent.getId(), agent);
            }
        }
    }

    private LinkedList<Integer> getFilteredTargetList(){


        int binomialDistributionSample;
        LinkedList<Integer> keepTargets = new LinkedList<>();


        int index = 0;
        for( BinomialDistribution binomialDistribution : getBinomialDistributions()  ) {

            binomialDistributionSample = binomialDistribution.sample();

            if (binomialDistributionSample == BINOMIAL_DISTRIBUTION_SUCCESS_VALUE){

                if (targetChanger.getAttributes().getProbabilitiesToChangeTarget().size() == 1){
                    keepTargets = targetChanger.getAttributes().getNextTarget();
                    break;
                }
                else {
                    keepTargets.add(targetChanger.getAttributes().getNextTarget().get(index));
                }
            }
            index += 1;

        }
        return keepTargets;

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

    private void useStaticTargetForAgent(Agent agent, LinkedList<Integer> keepTargets) {
        agent.setTargets(keepTargets);
        agent.setNextTargetListIndex(0);
        agent.setIsCurrentTargetAnAgent(false);
    }

    private void notifyListenersTargetChangerAreaReached(final Agent agent) {
        for (TargetChangerListener listener : targetChanger.getTargetChangerListeners()) {
            listener.reachedTargetChanger(targetChanger, agent);
        }
    }

}
