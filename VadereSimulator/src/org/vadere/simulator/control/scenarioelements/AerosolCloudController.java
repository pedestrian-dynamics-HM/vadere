package org.vadere.simulator.control.scenarioelements;

import org.vadere.state.attributes.scenario.AttributesAerosolCloud;
import org.vadere.state.scenario.*;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.logging.Logger;


/**
 * Manipulate pedestrians which enter the given {@link AerosolCloud}.
 * <p>
 * Take following attributes into account when manipulating pedestrians:
 * - {@link AttributesAerosolCloud#getShape()}
 * <p>
 */

public class AerosolCloudController extends ScenarioElementController {

    private static final Logger log = Logger.getLogger(AerosolCloudController.class);

    public final AerosolCloud aerosolCloud;
    private Topography topography;

    private boolean activeController = true; // flag (false) marks aerosolCloudController that has already removed its
    // aerosolCloud and thus can be removed as well

    // Constructors
    public AerosolCloudController(Topography topography, AerosolCloud aerosolCloud) {
        this.aerosolCloud = aerosolCloud;
        this.topography = topography;
    }

    public boolean isActiveController() {
        return activeController;
    }

    // Other methods
    public void create(double simTimeInSec, Pedestrian pedestrian) {
        VPoint position = pedestrian.getPosition();
        double pathogenLoad = pedestrian.getPathogenEmissionCapacity();
        topography.addAerosolCloud(new AerosolCloud(new AttributesAerosolCloud()));
        // ToDo create aerosolCloud at position with pathogenLoad, at simTimeInSec
    }

    public void update(double simTimeInSec) {
            System.out.println("in AerosolCloudController");
            changeAerosolCloudExtent();
            reduceAerosolCloudPathogenLoad();
            if (hasAerosolCloudReachedLifeEnd(simTimeInSec)) {
                aerosolCloud.setHasReachedLifeEnd(true);
            }
            deleteAerosolCloudFlagController();
    }

    public void changeAerosolCloudExtent() {
        // ToDo change extent
        // int dimension = 2;
        // double scalingFactor1D = 1;
        // double scalingFactorInDimension = Math.pow(scalingFactor1D, dimension);
        // setShape(new VShape(getShape().getCentroid(), getShape()."extentInDimension" * scalingFactorInDimension); // increase extent
        // aerosolCloud.setPathogenLoad(aerosolCloud.getPathogenLoad() / scalingFactorInDimension); // reduce pathogenLoad (density)
    }

    public void reduceAerosolCloudPathogenLoad() {
        aerosolCloud.setPathogenLoad(Math.max(0.0, aerosolCloud.getPathogenLoad()-0.1));
    }

    public boolean hasAerosolCloudReachedLifeEnd(double simTimeInSec) {
        double minimumRelevantPathogenLoad = 0.0;
        return (aerosolCloud.getPathogenLoad() <= minimumRelevantPathogenLoad) || (simTimeInSec > aerosolCloud.getCreationTime() + aerosolCloud.getLifeTime());
    }

    public void deleteAerosolCloudFlagController() {
        if (aerosolCloud.getHasReachedLifeEnd()) {
            topography.getAerosolClouds().remove(aerosolCloud);
            this.activeController = false; // flag aerosolCloudController so that it can be removed by Simulation
        }
    }

//    private void notifyListenersAerosolCloudReached(final Pedestrian pedestrian) {
//        for (AerosolCloudListener listener : aerosolCloud.getAerosolCloudListeners()) {
//            listener.reachedAerosolCloud(aerosolCloud, pedestrian);
//        }
//    }
}
