package org.vadere.simulator.control.scenarioelements;

import org.vadere.state.attributes.scenario.AttributesAerosolCloud;
import org.vadere.state.scenario.*;
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
    public void update(double simTimeInSec) {
            System.out.println("in AerosolCloudController");
            changeAerosolCloudExtent();
            reduceAerosolCloudPathogenLoad(simTimeInSec);
            if (hasAerosolCloudReachedLifeEnd(simTimeInSec)) {
                aerosolCloud.setHasReachedLifeEnd(true);
            }
            deleteAerosolCloudFlagController();
    }

    public void changeAerosolCloudExtent() {
        // Idea: change extent -> for now: constant
        // int dimension = 2;
        // double scalingFactor1D = 1;
        // double scalingFactorInDimension = Math.pow(scalingFactor1D, dimension);
        // setShape(new VShape(getShape().getCentroid(), getShape()."extentInDimension" * scalingFactorInDimension); // increase extent
        // aerosolCloud.setPathogenLoad(aerosolCloud.getPathogenLoad() / scalingFactorInDimension); // reduce pathogenLoad (density)
    }

    /**
     * Reduce pathogenDensity2D at the height of the pedestrians' faces (x-y-plane)
     * Considered effects: declining activity of pathogen, evaporation, gravitation
     */
    public void reduceAerosolCloudPathogenLoad(double simTimeInSec) {
        double t = simTimeInSec - aerosolCloud.getCreationTime();
        double lambda = - Math.log(0.5) / aerosolCloud.getHalfLife();
        aerosolCloud.setCurrentPathogenLoad(aerosolCloud.getInitialPathogenLoad() * Math.exp(-lambda * t));
    }

    public boolean hasAerosolCloudReachedLifeEnd(double simTimeInSec) {
        // assumption: aerosolCloud is not relevant anymore if it has reached less than 1% of its initialPathogenLoad
        // As a consequence, the life time is about ln(1%) / -lambda * halfLife = 6.6 times halfLife.
        double minimumRelevantPathogenLoad = 0.01 * aerosolCloud.getInitialPathogenLoad();
        return (aerosolCloud.getCurrentPathogenLoad() < minimumRelevantPathogenLoad);
    }

    public void deleteAerosolCloudFlagController() {
        if (aerosolCloud.getHasReachedLifeEnd()) {
            topography.getAerosolClouds().remove(aerosolCloud);
            this.activeController = false; // flag aerosolCloudController so that it can be removed by Simulation
        }
    }
}
