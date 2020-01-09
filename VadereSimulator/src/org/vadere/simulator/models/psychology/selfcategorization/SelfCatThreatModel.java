package org.vadere.simulator.models.psychology.selfcategorization;

import org.jetbrains.annotations.NotNull;
import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.models.MainModel;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.models.SpeedAdjuster;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.simulator.models.osm.optimization.StepCircleOptimizer;
import org.vadere.simulator.models.osm.updateScheme.UpdateSchemeOSM;
import org.vadere.simulator.models.potential.fields.IPotentialFieldTarget;
import org.vadere.simulator.models.potential.fields.IPotentialFieldTargetGrid;
import org.vadere.simulator.models.potential.fields.PotentialFieldAgent;
import org.vadere.simulator.models.potential.fields.PotentialFieldObstacle;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesOSM;
import org.vadere.state.attributes.models.AttributesSelfCatThreat;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.psychology.perception.types.Bang;
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
 * This class models how agents react to a perceived threat (a loud bang) while considering the
 * self-categorization theory ("reicher-2010").
 *
 * Please, note:
 * <ul>
 *     <li>A {@link Bang} has a loudness and a radius.</li>
 *     <li>We use the self-categorization theory to divide the agents into in- and out-group members.
 *     In-group members imitate the behavior of other in-group members. Out-group members ignore the
 *     behavior of in-group members.</li>
 * </ul>
 *
 * Implement following behavior:
 *
 * <ol>
 *     <li>If agent A is within the bang radius, escape (i.e., first maximize distance to the bang and then
 *     go to a safe zone).</li>
 *     <li>If agent A is outside bang radius, check if "searchRadius" contains an escaping in-group member.
 *     If yes, escape also. Otherwise, go to original target.</li>
 *     <li>Out-group members use the locomotion layer to go to a target (i.e., keep their original behavior).</li>
 * </ol>
 */
@ModelClass(isMainModel = true)
public class SelfCatThreatModel implements MainModel {

    // Static Variables
    private final static Logger logger = Logger.getLogger(SelfCatThreatModel.class);

    // Variables
    private AttributesSelfCatThreat attributesSelfCatThreat;
    private AttributesOSM attributesLocomotion;
    AttributesAgent attributesPedestrian;

    private Topography topography;
    private Random random;

    private IPotentialFieldTarget potentialFieldTarget;
    private PotentialFieldObstacle potentialFieldObstacle;
    private PotentialFieldAgent potentialFieldPedestrian;
    private StepCircleOptimizer stepCircleOptimizer;
    private UpdateSchemeOSM updateSchemeOSM;

    // These models are updated in actual simulation loop.
    private List<Model> models = new LinkedList<>();
    private double lastSimTimeInSec;

    @Override
    public List<Model> getSubmodels() {
        return models;
    }

    @Override
    public <T extends DynamicElement> DynamicElement createElement(VPoint position, int id, Class<T> type) {
        if (!Pedestrian.class.isAssignableFrom(type))
            throw new IllegalArgumentException("cannot initialize " + type.getCanonicalName());

        AttributesAgent pedAttributes = new AttributesAgent(
                this.attributesPedestrian, registerDynamicElementId(topography, id));

        PedestrianSelfCatThreat pedestrian = createElement(position, pedAttributes);

        return pedestrian;
    }

    private PedestrianSelfCatThreat createElement(VPoint position, @NotNull final AttributesAgent attributesAgent) {
        List<SpeedAdjuster> noSpeedAdjusters = new LinkedList<>();

        PedestrianSelfCatThreat pedestrian = new PedestrianSelfCatThreat(attributesLocomotion,
                attributesAgent, topography, random, potentialFieldTarget,
                potentialFieldObstacle.copy(), potentialFieldPedestrian,
                noSpeedAdjusters, stepCircleOptimizer.clone());
        pedestrian.setPosition(position);

        return pedestrian;
    }

    @Override
    public VShape getDynamicElementRequiredPlace(@NotNull VPoint position) {
        return createElement(position,  new AttributesAgent(attributesPedestrian, -1)).getShape();
    }

    @Override
    public void initialize(List<Attributes> attributesList, Topography topography, AttributesAgent attributesPedestrian, Random random) {
        logger.debug("initialize " + getClass().getSimpleName());

        this.attributesSelfCatThreat = Model.findAttributes(attributesList, AttributesSelfCatThreat.class);
        this.attributesLocomotion = attributesSelfCatThreat.getAttributesLocomotion();
        this.topography = topography;
        this.random = random;
        this.attributesPedestrian = attributesPedestrian;

        initializeLocomotionLayer(attributesList, topography, attributesPedestrian, random);

        this.topography.addElementAddedListener(Pedestrian.class, updateSchemeOSM);
        this.topography.addElementRemovedListener(Pedestrian.class, updateSchemeOSM);

        models.add(potentialFieldTarget);
        models.add(this);
    }

    private void initializeLocomotionLayer(List<Attributes> attributesList, Topography topography, AttributesAgent attributesPedestrian, Random random) {
        IPotentialFieldTargetGrid potentialTargetGrid = IPotentialFieldTargetGrid.createPotentialField(
                attributesList, topography, attributesPedestrian, attributesLocomotion.getTargetPotentialModel());

        this.potentialFieldTarget = potentialTargetGrid;

        this.potentialFieldObstacle = PotentialFieldObstacle.createPotentialField(
                attributesList, topography, attributesPedestrian, random, attributesLocomotion.getObstaclePotentialModel());
        this.potentialFieldPedestrian = PotentialFieldAgent.createPotentialField(
                attributesList, topography, attributesPedestrian, random, attributesLocomotion.getPedestrianPotentialModel());

        this.stepCircleOptimizer = StepCircleOptimizer.create(
                attributesLocomotion, random, topography, potentialTargetGrid);

        if (attributesPedestrian.isDensityDependentSpeed()) {
            throw new UnsupportedOperationException("densityDependentSpeed not yet implemented.");
            // this.speedAdjusters.add(new SpeedAdjusterWeidmann());
        }

        if (attributesLocomotion.getUpdateType() == UpdateType.PARALLEL) {
           throw new IllegalArgumentException("\"UpdateType.PARALLEL\" not supported!");
        }

        this.updateSchemeOSM = UpdateSchemeOSM.create(attributesLocomotion.getUpdateType(), topography, random);
    }

    @Override
    public void preLoop(final double simTimeInSec) {
        this.lastSimTimeInSec = simTimeInSec;
    }

    @Override
    public void postLoop(final double simTimeInSec) {
        updateSchemeOSM.shutdown();
    }

    @Override
    public void update(final double simTimeInSec) {
        double timeStepInSec = simTimeInSec - this.lastSimTimeInSec;
        // TODO: Copy "UpdateSchemeEventDriven" as default locomotion model
        //  and implement specific behavior there by using behavior repertoire from "OSMBehaviorController":
        updateSchemeOSM.update(timeStepInSec, simTimeInSec);
        lastSimTimeInSec = simTimeInSec;
    }
}
