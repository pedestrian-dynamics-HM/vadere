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
    public void update(double simTimeInSec, double simTimeStepLength) {
            System.out.println("in AerosolCloudController");

            // Effect of diffusion is negligible if the simulation time is short
            // double rateOfSpread = 0.001;
            // aerosolCloud.increaseShape(rateOfSpread * simTimeStepLength);

            aerosolCloud.updateCurrentAerosolCloudPathogenLoad(simTimeInSec);

            if (hasAerosolCloudReachedLifeEnd()) {
                aerosolCloud.setHasReachedLifeEnd(true);
            }
            deleteAerosolCloudFlagController();
    }

    public boolean hasAerosolCloudReachedLifeEnd() {
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
