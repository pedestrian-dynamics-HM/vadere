package org.vadere.simulator.models.infection;

import org.vadere.simulator.context.VadereContext;
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
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.Vector2D;
import org.vadere.util.logging.Logger;

import java.util.*;

public class ExtendedAirTransmissionModel extends AirTransmissionModel {

    private final Logger logger = Logger.getLogger(ExtendedAirTransmissionModel.class);
    private AttributesExtendedAirTransmissionModel attrAirTransmissionModel;

    @Override
    public void initialize(List<Attributes> attributesList, Domain domain, AttributesAgent attributesPedestrian, Random random) {
        super.initialize(attributesList, domain, attributesPedestrian, random);
        attrAirTransmissionModel = Model.findAttributes(attributesList, AttributesExtendedAirTransmissionModel.class);
    }

    @Override
    public void createAerosolClouds(double simTimeInSec, Pedestrian pedestrian) {

        if (pedestrian.<AirTransmissionModelHealthStatus>getHealthStatus().isStartingExhalation()) {
            pedestrian.<AirTransmissionModelHealthStatus>getHealthStatus().setExhalationStartPosition(pedestrian.getPosition());

        } else if (pedestrian.<AirTransmissionModelHealthStatus>getHealthStatus().isStartingInhalation()) {
            VPoint aerosolCloudCenter = computeAerosolCloudCenter(pedestrian);
            int initialPathogenLoad = computeAerosolCloudPathogenLoad(pedestrian);
            AerosolCloud aerosolCloud = generateAerosolCloud(simTimeInSec, aerosolCloudCenter, initialPathogenLoad);
            topography.addAerosolCloud(aerosolCloud);

            pedestrian.<AirTransmissionModelHealthStatus>getHealthStatus().resetStartExhalationPosition();
        }
    }

    private int computeAerosolCloudPathogenLoad(Pedestrian pedestrian) {
        int initialPathogenLoad = attrAirTransmissionModel.getAerosolCloudInitialPathogenLoad();
        if (pedestrian.<ExtendedAirTransmissionModelHealthStatus>getHealthStatus().isSneezing()) {
            pedestrian.<ExtendedAirTransmissionModelHealthStatus>getHealthStatus().incrementBreathCounterSneezing();
            if (pedestrian.<ExtendedAirTransmissionModelHealthStatus>getHealthStatus().isSneezingNow()) {
                initialPathogenLoad = initialPathogenLoad * attrAirTransmissionModel.getAerosolCloudPathogenLoadMultiplierSneezing();
                pedestrian.<ExtendedAirTransmissionModelHealthStatus>getHealthStatus().resetBreathCounterSneezing();
            }
        }
        else if (pedestrian.<ExtendedAirTransmissionModelHealthStatus>getHealthStatus().isCoughing()) {  //cannot cough/sneeze/speak at the same time
            pedestrian.<ExtendedAirTransmissionModelHealthStatus>getHealthStatus().incrementBreathCounterCoughing();
            if (pedestrian.<ExtendedAirTransmissionModelHealthStatus>getHealthStatus().isCoughingNow()) {
                initialPathogenLoad = initialPathogenLoad * attrAirTransmissionModel.getAerosolCloudPathogenLoadMultiplierCoughing();
                pedestrian.<ExtendedAirTransmissionModelHealthStatus>getHealthStatus().resetBreathCounterCoughing();
            }
        }
        else if (pedestrian.<ExtendedAirTransmissionModelHealthStatus>getHealthStatus().isTalking()) {
            initialPathogenLoad = initialPathogenLoad * attrAirTransmissionModel.getAerosolCloudPathogenLoadMultiplierTalking();
        }
        return initialPathogenLoad;
    }

    private VPoint computeAerosolCloudCenter(Pedestrian pedestrian) {
        VPoint aerosolCloudCenter;
        if (pedestrian.isSitting()) {
            Vector2D aerosolCloudDirection = pedestrian.getSittingDirection().normalize(attrAirTransmissionModel.getAerosolCloudInitialRadius());
            aerosolCloudCenter = new VPoint(pedestrian.getPosition().getX() + aerosolCloudDirection.getX(),
                    pedestrian.getPosition().getY() + aerosolCloudDirection.getY());
        }
        else {
            VPoint startBreatheOutPosition = pedestrian.<AirTransmissionModelHealthStatus>getHealthStatus().getExhalationStartPosition();
            VPoint stopBreatheOutPosition = pedestrian.getPosition();
            double walkingDirectionX = stopBreatheOutPosition.getX() - startBreatheOutPosition.getX();
            double walkingDirectionY = stopBreatheOutPosition.getY() - startBreatheOutPosition.getY();
            Vector2D aerosolCloudDirection = new Vector2D(walkingDirectionX, walkingDirectionY).normalize(attrAirTransmissionModel.getAerosolCloudInitialRadius());

            VLine distanceWalkedDuringExhalation = new VLine(startBreatheOutPosition, stopBreatheOutPosition);
            //aerosolCloudCenter = distanceWalkedDuringExhalation.midPoint();
            VPoint walkingMidPoint = distanceWalkedDuringExhalation.midPoint();
            aerosolCloudCenter = new VPoint(walkingMidPoint.getX() + aerosolCloudDirection.getX(),
                    walkingMidPoint.getY() + aerosolCloudDirection.getY());
        }
        return aerosolCloudCenter;
    }

    private AerosolCloud generateAerosolCloud(double simTimeInSec, VPoint AerosolCloudCenter, double initialPathogenLoad) {
        AerosolCloud aerosolCloud = new AerosolCloud(new AttributesAerosolCloud(aerosolCloudIdCounter,
                attrAirTransmissionModel.getAerosolCloudInitialRadius(),
                AerosolCloudCenter,
                simTimeInSec,
                initialPathogenLoad));

        aerosolCloudIdCounter = aerosolCloudIdCounter + 1;

        return aerosolCloud;
    }

    public void updateAerosolCloudsPathogenLoad(double simTimeInSec) {
        double lambda = exponentialDecayFactor / attrAirTransmissionModel.getAerosolCloudHalfLife();

        Collection<AerosolCloud> allAerosolClouds = topography.getAerosolClouds();
        for (AerosolCloud aerosolCloud : allAerosolClouds) {
            double t = simTimeInSec - aerosolCloud.getCreationTime();
            //aerosolCloud.setCurrentPathogenLoad(attrAirTransmissionModel.getAerosolCloudInitialPathogenLoad() * Math.exp(-lambda * t));

            aerosolCloud.setCurrentPathogenLoad(aerosolCloud.getCurrentPathogenLoad() * Math.exp(-lambda * simTimeStepLength));
        }
    }

    @Override
    public Agent sourceControllerEvent(SourceController controller, double simTimeInSec, Agent scenarioElement) {
        AttributesExtendedExposureModelSourceParameters sourceParameters = (AttributesExtendedExposureModelSourceParameters) defineSourceParameters(controller, attrAirTransmissionModel);

        Pedestrian ped = (Pedestrian) scenarioElement;
        ped.setHealthStatus(new ExtendedAirTransmissionModelHealthStatus());
        ped.setInfectious(sourceParameters.isInfectious());
        ped.<ExtendedAirTransmissionModelHealthStatus>getHealthStatus().setTalking(sourceParameters.isTalking());
        ped.<ExtendedAirTransmissionModelHealthStatus>getHealthStatus().setCoughing(sourceParameters.isCoughing());
        ped.<ExtendedAirTransmissionModelHealthStatus>getHealthStatus().setSneezing(sourceParameters.isSneezing());
        ped.<ExtendedAirTransmissionModelHealthStatus>getHealthStatus().setCoughingEveryNthBreath(sourceParameters.getCoughingEveryNthBreath());
        ped.<ExtendedAirTransmissionModelHealthStatus>getHealthStatus().setSneezingEveryNthBreath(sourceParameters.getSneezingEveryNthBreath());
        ped.<AirTransmissionModelHealthStatus>getHealthStatus().setRespiratoryTimeOffset(random.nextDouble() * attrAirTransmissionModel.getPedestrianRespiratoryCyclePeriod());
        ped.<AirTransmissionModelHealthStatus>getHealthStatus().setBreathingIn(false);
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
