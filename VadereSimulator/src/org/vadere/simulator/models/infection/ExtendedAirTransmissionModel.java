package org.vadere.simulator.models.infection;

import org.vadere.simulator.control.scenarioelements.SourceController;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.infection.*;
import org.vadere.state.attributes.scenario.AttributesAerosolCloud;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.health.AirTransmissionModelHealthStatus;
import org.vadere.state.health.ExtendedAirTransmissionModelHealthStatus;
import org.vadere.state.scenario.AerosolCloud;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.Vector2D;
import org.vadere.util.logging.Logger;

import java.util.*;

public class ExtendedAirTransmissionModel extends AirTransmissionModel {

    private static final int STUCK_MAX = 10;

    private final Logger logger = Logger.getLogger(ExtendedAirTransmissionModel.class);
    private AttributesExtendedAirTransmissionModel attrAirTransmissionModel;

    private Map<AerosolCloud, Integer> aerosolCounter;
    private Map<Integer, Integer> spawnCounter;

    @Override
    public void initialize(List<Attributes> attributesList, Domain domain, AttributesAgent attributesPedestrian, Random random) {
        super.initialize(domain, attributesPedestrian, random);
        attrAirTransmissionModel = Model.findAttributes(attributesList, AttributesExtendedAirTransmissionModel.class);
        spawnCounter = new HashMap<>();
        aerosolCounter = new HashMap<>();
    }

    @Override
    protected AttributesExposureModel getAttributes() {
        return attrAirTransmissionModel;
    }

    @Override
    public void updateAerosolClouds(double simTimeInSec) {
        updateAerosolCloudsPathogenLoad(simTimeInSec);
        updateAerosolCloudsExtent();
        updateAerosolCloudsLocation();
        deleteExpiredAerosolClouds();
        removeStuckAerosolClouds();
    }

    public void removeStuckAerosolClouds() {
        Collection<AerosolCloud> aerosolClouds = topography.getAerosolClouds();
        Collection<Obstacle> obstacles = topography.getObstacles();
        Collection<AerosolCloud> toRemove = new ArrayList<>();
        for (AerosolCloud aerosolCloud : aerosolClouds) {
            boolean isStuck = obstacles.stream().anyMatch(obstacle -> obstacle.getShape().contains(aerosolCloud.getCenter()));
            if (isStuck) {
                if (aerosolCounter.containsKey(aerosolCloud)) {
                    aerosolCounter.put(aerosolCloud, aerosolCounter.get(aerosolCloud) + 1);

                    if (aerosolCounter.get(aerosolCloud) > STUCK_MAX) {
                        aerosolCounter.remove(aerosolCloud);
                        toRemove.add(aerosolCloud);
                    }
                } else {
                    aerosolCounter.put(aerosolCloud, 1);
                }
            }  else {
                aerosolCounter.remove(aerosolCloud);
            }
        }
        aerosolClouds.removeAll(toRemove);
    }

    public void updateAerosolCloudsLocation() {
        AttributesExtendedAirTransmissionModel attrModel = (AttributesExtendedAirTransmissionModel) getAttributes();
        Collection<AerosolCloud> allAerosolClouds = topography.getAerosolClouds();
        for (AerosolCloud aerosolCloud : allAerosolClouds) {
            double[] windXY = topography.getAirFlow().getFlowDirection(aerosolCloud.getCenter().getX(), aerosolCloud.getCenter().getY());
            aerosolCloud.shiftShape(windXY[0] / simTimeStepLength, windXY[1] / simTimeStepLength);
        }
    }

    @Override
    public void createAerosolClouds(double simTimeInSec, Pedestrian pedestrian) {
        ExtendedAirTransmissionModelHealthStatus healthStatus = pedestrian.getHealthStatus();

        if (healthStatus.isStartingExhalation()) {
            healthStatus.setExhalationStartPosition(pedestrian.getPosition());

        } else if (healthStatus.isStartingInhalation()) {
            VPoint aerosolCloudCenter = computeAerosolCloudCenter(pedestrian);
            int initialPathogenLoad = computeAerosolCloudPathogenLoad(pedestrian);
            AerosolCloud aerosolCloud = generateAerosolCloud(simTimeInSec, aerosolCloudCenter, initialPathogenLoad);
            topography.addAerosolCloud(aerosolCloud);

            healthStatus.resetStartExhalationPosition();
        }
    }

    private int computeAerosolCloudPathogenLoad(Pedestrian pedestrian) {
        AttributesExtendedAirTransmissionModel attrModel = (AttributesExtendedAirTransmissionModel) getAttributes();
        ExtendedAirTransmissionModelHealthStatus healthStatus = pedestrian.getHealthStatus();

        int initialPathogenLoad = attrModel.getAerosolCloudInitialPathogenLoad();
        if (healthStatus.isSneezing()) {
            healthStatus.incrementBreathCounterSneezing();
            if (healthStatus.isSneezingNow()) {
                initialPathogenLoad = initialPathogenLoad * attrModel.getAerosolCloudPathogenLoadMultiplierSneezing();
                healthStatus.resetBreathCounterSneezing();
            }
        }
        else if (healthStatus.isCoughing()) {  //cannot cough/sneeze/speak at the same time
            healthStatus.incrementBreathCounterCoughing();
            if (healthStatus.isCoughingNow()) {
                initialPathogenLoad = initialPathogenLoad * attrModel.getAerosolCloudPathogenLoadMultiplierCoughing();
                healthStatus.resetBreathCounterCoughing();
            }
        }
        else if (healthStatus.isTalking()) {
            initialPathogenLoad = initialPathogenLoad * attrModel.getAerosolCloudPathogenLoadMultiplierTalking();
        }
        return initialPathogenLoad;
    }

    private VPoint computeAerosolCloudCenter(Pedestrian pedestrian) {
        AttributesExtendedAirTransmissionModel attrModel = (AttributesExtendedAirTransmissionModel) getAttributes();
        VPoint aerosolCloudCenter;
        if (pedestrian.isSitting()) {
            Vector2D aerosolCloudDirection = pedestrian.getSittingDirection().normalize(attrModel.getAerosolCloudInitialRadius());
            aerosolCloudCenter = new VPoint(pedestrian.getPosition().getX() + aerosolCloudDirection.getX(),
                    pedestrian.getPosition().getY() + aerosolCloudDirection.getY());
        }
        else {
            VPoint startBreatheOutPosition = pedestrian.<AirTransmissionModelHealthStatus>getHealthStatus().getExhalationStartPosition();
            VPoint stopBreatheOutPosition = pedestrian.getPosition();
            double walkingDirectionX = stopBreatheOutPosition.getX() - startBreatheOutPosition.getX();
            double walkingDirectionY = stopBreatheOutPosition.getY() - startBreatheOutPosition.getY();
            Vector2D aerosolCloudDirection = new Vector2D(walkingDirectionX, walkingDirectionY).normalize(attrModel.getAerosolCloudInitialRadius());

            VLine distanceWalkedDuringExhalation = new VLine(startBreatheOutPosition, stopBreatheOutPosition);
            //aerosolCloudCenter = distanceWalkedDuringExhalation.midPoint();
            VPoint walkingMidPoint = distanceWalkedDuringExhalation.midPoint();
            aerosolCloudCenter = new VPoint(walkingMidPoint.getX() + aerosolCloudDirection.getX(),
                    walkingMidPoint.getY() + aerosolCloudDirection.getY());
        }
        return aerosolCloudCenter;
    }

    private AerosolCloud generateAerosolCloud(double simTimeInSec, VPoint AerosolCloudCenter, double initialPathogenLoad) {
        AttributesExtendedAirTransmissionModel attrModel = (AttributesExtendedAirTransmissionModel) getAttributes();
        AerosolCloud aerosolCloud = new AerosolCloud(new AttributesAerosolCloud(aerosolCloudIdCounter,
                attrModel.getAerosolCloudInitialRadius(),
                AerosolCloudCenter,
                simTimeInSec,
                initialPathogenLoad));

        aerosolCloudIdCounter = aerosolCloudIdCounter + 1;

        return aerosolCloud;
    }

    @Override
    synchronized public Agent sourceControllerEvent(SourceController controller, double simTimeInSec, Agent scenarioElement) {
        AttributesExtendedAirTransmissionModel attrModel = (AttributesExtendedAirTransmissionModel) getAttributes();
        AttributesExtendedExposureModelSourceParameters sourceParameters = (AttributesExtendedExposureModelSourceParameters) defineSourceParameters(controller, attrModel);

        Pedestrian ped = (Pedestrian) scenarioElement;
        ExtendedAirTransmissionModelHealthStatus healthStatus = new ExtendedAirTransmissionModelHealthStatus();
        healthStatus.setTalking(sourceParameters.isTalking());
        healthStatus.setCoughing(sourceParameters.isCoughing());
        healthStatus.setSneezing(sourceParameters.isSneezing());
        healthStatus.setCoughingEveryNthBreath(sourceParameters.getCoughingEveryNthBreath());
        healthStatus.setSneezingEveryNthBreath(sourceParameters.getSneezingEveryNthBreath());
        healthStatus.setRespiratoryTimeOffset(random.nextDouble() * attrModel.getPedestrianRespiratoryCyclePeriod());
        healthStatus.setBreathingIn(false);
        ped.setHealthStatus(healthStatus);

        if (!spawnCounter.containsKey(controller.getSourceId())) {
            spawnCounter.put(controller.getSourceId(), 0);
        }
        int counter = spawnCounter.get(controller.getSourceId());
        if (!sourceParameters.getInfectiousSpawnIds().isEmpty() && sourceParameters.getInfectiousSpawnIds().get(0) == counter) {
            ped.setInfectious(sourceParameters.isInfectious());
            sourceParameters.getInfectiousSpawnIds().remove(0);
        }
        spawnCounter.put(controller.getSourceId(), counter + 1);
        return ped;
    }

    public AttributesExposureModelSourceParameters defineSourceParameters(SourceController controller, AttributesExtendedAirTransmissionModel attributes) {
        int sourceId = controller.getSourceId();
        int defaultSourceId = -1;
        Optional<AttributesExtendedExposureModelSourceParameters> sourceParameters = attributes
                .getExtendedExposureModelSourceParameters().stream().filter(s -> s.getSourceId() == sourceId).findFirst();

        // if sourceId not set by user, check if the user has defined default attributes by setting sourceId = -1
        if (sourceParameters.isEmpty()) {
            sourceParameters = attributes.getExtendedExposureModelSourceParameters().stream().filter(s -> s.getSourceId() == defaultSourceId).findFirst();

            // if no user defined default values: use attributesAirTransmissionModel default values
            if (sourceParameters.isPresent()) {
                logger.infof(">>>>>>>>>>>defineSourceParameters: sourceId %d not set explicitly exposureModelSourceParameters. Source uses default exposureModelSourceParameters defined for sourceId: %d", sourceId, defaultSourceId);
            } else {
                logger.errorf(">>>>>>>>>>>defineSourceParameters: sourceId %d is not set in exposureModelSourceParameters", sourceId);
            }
        }
        return sourceParameters.get();
    }
}
