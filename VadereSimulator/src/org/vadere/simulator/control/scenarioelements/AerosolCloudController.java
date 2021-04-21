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
    }
}
