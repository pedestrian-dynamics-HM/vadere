package org.vadere.simulator.models.psychology.selfcategorization;

import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.jetbrains.annotations.NotNull;
import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.models.MainModel;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.models.SpeedAdjuster;
import org.vadere.simulator.models.osm.optimization.StepCircleOptimizer;
import org.vadere.simulator.models.potential.fields.IPotentialFieldTarget;
import org.vadere.simulator.models.potential.fields.IPotentialFieldTargetGrid;
import org.vadere.simulator.models.potential.fields.PotentialFieldAgent;
import org.vadere.simulator.models.potential.fields.PotentialFieldObstacle;
import org.vadere.simulator.models.psychology.selfcategorization.locomotion.UpdateSchemeEventDriven;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesOSM;
import org.vadere.state.attributes.models.AttributesSelfCatThreat;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.psychology.cognition.GroupMembership;
import org.vadere.state.psychology.perception.types.Threat;
import org.vadere.state.scenario.DynamicElement;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.state.types.UpdateType;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.logging.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * This class models how agents react to a perceived threat (e.g., a loud bang) while considering the
 * self-categorization theory ("reicher-2010").
 *
 * Please, note:
 * <ul>
 *     <li>A {@link Threat} has a loudness and a radius.</li>
 *     <li>We use the self-categorization theory to divide the agents into in- and out-group members.
 *     In-group members imitate the behavior of other in-group members. Out-group members ignore the
 *     behavior of in-group members.</li>
 * </ul>
 *
 * Implement following behavior:
 *
 * <ol>
 *     <li>If agent A is within the threat radius, escape (i.e., first maximize distance to the threat and then
 *     go to a safe zone).</li>
 *     <li>If agent A is outside threat radius, check if "searchRadius" contains an escaping in-group member.
 *     If yes, escape also. Otherwise, go to original target.</li>
 *     <li>Out-group members use the locomotion layer to go to a target (i.e., keep their original behavior).</li>
 * </ol>
 */
@ModelClass(isMainModel = true)
public class SelfCatThreatModel implements MainModel {

    // Static Variables
    private final static Logger logger = Logger.getLogger(SelfCatThreatModel.class);
    private static final int BINOMIAL_DISTRIBUTION_SUCCESS_VALUE = 1;

    // Variables
    private AttributesSelfCatThreat attributesSelfCatThreat;
    private AttributesOSM attributesLocomotion;
    AttributesAgent attributesPedestrian;

    private Domain domain;
    private Random random;

    private IPotentialFieldTarget potentialFieldTarget;
    private PotentialFieldObstacle potentialFieldObstacle;
    private PotentialFieldAgent potentialFieldPedestrian;
    private StepCircleOptimizer stepCircleOptimizer;
    private UpdateSchemeEventDriven updateSchemeEventDriven;

    // These models are updated in the actual simulation loop.
    private List<Model> models = new LinkedList<>();
    private double lastSimTimeInSec;

    // Distribution to assign pedestrians as IN_GROUP or OUT_GROUP members.
    BinomialDistribution binomialDistribution;

    @Override
    public List<Model> getSubmodels() {
        return models;
    }

    @Override
    public <T extends DynamicElement> DynamicElement createElement(VPoint position, int id, Class<T> type) {

        return createElement(position, id, this.attributesPedestrian, type);
    }

    @Override
    public <T extends DynamicElement> DynamicElement createElement(VPoint position, int id, Attributes attr, Class<T> type) {

        AttributesAgent aAttr = (AttributesAgent)attr;

        if (!Pedestrian.class.isAssignableFrom(type))
            throw new IllegalArgumentException("cannot initialize " + type.getCanonicalName());

        AttributesAgent pedAttributes = new AttributesAgent(
                aAttr, registerDynamicElementId(domain.getTopography(), id));

        PedestrianSelfCatThreat pedestrian = createElement(position, pedAttributes);

        return pedestrian;
    }

    private PedestrianSelfCatThreat createElement(VPoint position, @NotNull final AttributesAgent attributesAgent) {
        List<SpeedAdjuster> noSpeedAdjusters = new LinkedList<>();

        PedestrianSelfCatThreat pedestrian = new PedestrianSelfCatThreat(attributesLocomotion,
                attributesAgent, domain.getTopography(), random, potentialFieldTarget,
                potentialFieldObstacle.copy(), potentialFieldPedestrian,
                noSpeedAdjusters, stepCircleOptimizer.clone());

        pedestrian.setPosition(position);

        GroupMembership groupMembership = drawGroupMembershipFromDistribution();
        pedestrian.setGroupMembership(groupMembership);

        return pedestrian;
    }

    private GroupMembership drawGroupMembershipFromDistribution() {
        int binomialDistributionSample = binomialDistribution.sample();
        boolean inGroupMember = (binomialDistributionSample == BINOMIAL_DISTRIBUTION_SUCCESS_VALUE);

        GroupMembership groupMembership = (inGroupMember) ? GroupMembership.IN_GROUP : GroupMembership.OUT_GROUP;

        return groupMembership;
    }

    @Override
    public VShape getDynamicElementRequiredPlace(@NotNull VPoint position) {
        return createElement(position,  new AttributesAgent(attributesPedestrian, -1)).getShape();
    }

    @Override
    public void initialize(List<Attributes> attributesList, Domain domain, AttributesAgent attributesPedestrian, Random random) {
        logger.debug("initialize " + getClass().getSimpleName());

        this.attributesSelfCatThreat = Model.findAttributes(attributesList, AttributesSelfCatThreat.class);
        this.attributesLocomotion = attributesSelfCatThreat.getAttributesLocomotion();
        this.domain = domain;
        this.random = random;
        this.attributesPedestrian = attributesPedestrian;

        initializeLocomotionLayer(attributesList, domain, attributesPedestrian, random);

        this.domain.getTopography().addElementAddedListener(Pedestrian.class, updateSchemeEventDriven);
        this.domain.getTopography().addElementRemovedListener(Pedestrian.class, updateSchemeEventDriven);

        models.add(potentialFieldTarget);
        models.add(this);

        int seed = random.nextInt();
        this.binomialDistribution = createBinomialDistribution(seed, attributesSelfCatThreat.getProbabilityInGroupMembership());
    }

    private void initializeLocomotionLayer(List<Attributes> attributesList, Domain domain, AttributesAgent attributesPedestrian, Random random) {
        IPotentialFieldTargetGrid potentialTargetGrid = IPotentialFieldTargetGrid.createPotentialField(
                attributesList, domain, attributesPedestrian, attributesLocomotion.getTargetPotentialModel());

        this.potentialFieldTarget = potentialTargetGrid;

        this.potentialFieldObstacle = PotentialFieldObstacle.createPotentialField(
                attributesList, domain, attributesPedestrian, random, attributesLocomotion.getObstaclePotentialModel());
        this.potentialFieldPedestrian = PotentialFieldAgent.createPotentialField(
                attributesList, domain, attributesPedestrian, random, attributesLocomotion.getPedestrianPotentialModel());

        this.stepCircleOptimizer = StepCircleOptimizer.create(
                attributesLocomotion, random, domain.getTopography(), potentialTargetGrid);

        if (attributesPedestrian.isDensityDependentSpeed()) {
            throw new UnsupportedOperationException("densityDependentSpeed not yet implemented.");
            // this.speedAdjusters.add(new SpeedAdjusterWeidmann());
        }

        if (attributesLocomotion.getUpdateType() != UpdateType.EVENT_DRIVEN) {
           throw new IllegalArgumentException("Only \"UpdateType.EVENT_DRIVEN\" supported!");
        }

        this.updateSchemeEventDriven = new UpdateSchemeEventDriven(domain.getTopography());
    }

    private BinomialDistribution createBinomialDistribution(int seed, double probabilityForInGroupMembership) {
        JDKRandomGenerator randomGenerator = new JDKRandomGenerator();
        randomGenerator.setSeed(seed);
        int trials = BINOMIAL_DISTRIBUTION_SUCCESS_VALUE; // I.e., possible outcomes are 0 and 1 when calling "sample()".

        return new BinomialDistribution(randomGenerator, trials, probabilityForInGroupMembership);
    }

    @Override
    public void preLoop(final double simTimeInSec) {
        this.lastSimTimeInSec = simTimeInSec;
    }

    @Override
    public void postLoop(final double simTimeInSec) { }

    @Override
    public void update(final double simTimeInSec) {
        double timeStepInSec = simTimeInSec - this.lastSimTimeInSec;
        updateSchemeEventDriven.update(timeStepInSec, simTimeInSec);
        lastSimTimeInSec = simTimeInSec;
    }
}
