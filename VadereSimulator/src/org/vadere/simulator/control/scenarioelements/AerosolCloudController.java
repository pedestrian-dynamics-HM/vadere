package org.vadere.simulator.control.scenarioelements;

import org.vadere.state.attributes.scenario.AttributesAerosolCloud;
import org.vadere.state.scenario.*;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.logging.Logger;

import static org.vadere.state.attributes.Attributes.ID_NOT_SET;


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

    // Constructors
    public AerosolCloudController(Topography topography, AerosolCloud aerosolCloud) {
        this.aerosolCloud = aerosolCloud;
        this.topography = topography;
    }

    // Other methods
    public void create(double simTimeInSec, Pedestrian pedestrian) {
        VPoint position = pedestrian.getPosition();
        double pathogenLoad = pedestrian.getPathogenEmissionCapacity();

        // ToDo create aerosolCloud at position with pathogenLoad, at simTimeInSec
    }

    public void update(double simTimeInSec) {
        changeAerosolCloudExtent(aerosolCloud);
        reduceAerosolCloudPathogenLoad(aerosolCloud);
        if (hasAerosolCloudReachedLifeEnd(aerosolCloud, simTimeInSec)) {
            // ToDo delete cloud
        }
    }

    public void changeAerosolCloudExtent(AerosolCloud aerosolCloud) {
        // ToDo change extent
        // int dimension = 2;
        // double scalingFactor1D = 1;
        // double scalingFactorInDimension = Math.pow(scalingFactor1D, dimension);
        // setShape(new VShape(getShape().getCentroid(), getShape()."extentInDimension" * scalingFactorInDimension); // increase extent
        // aerosolCloud.setPathogenLoad(aerosolCloud.getPathogenLoad() / scalingFactorInDimension); // reduce pathogenLoad (density)
    }

    public void reduceAerosolCloudPathogenLoad(AerosolCloud aerosolCloud) {
        // ToDo
    }

    public boolean hasAerosolCloudReachedLifeEnd(AerosolCloud aerosolCloud, double simTimeInSec) {
        double minimumRelevantPathogenLoad = 0.0;
        return (aerosolCloud.getPathogenLoad() <= minimumRelevantPathogenLoad) || (simTimeInSec > aerosolCloud.getCreationTime() + aerosolCloud.getLifeTime());
    }

//    private void notifyListenersAerosolCloudReached(final Pedestrian pedestrian) {
//        for (AerosolCloudListener listener : aerosolCloud.getAerosolCloudListeners()) {
//            listener.reachedAerosolCloud(aerosolCloud, pedestrian);
//        }
//    }
}
