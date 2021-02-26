package org.vadere.simulator.control.scenarioelements;

import org.vadere.state.scenario.*;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.logging.Logger;

import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Manipulate pedestrians which enter the given {@link AerosolCloud}.
 * <p>
 * Take following attributes into account when manipulating pedestrians:
 * - {@link org.vadere.state.attributes.scenario.AttributesAerosolCloud#getAerosolCloudRadius()}
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
    public void update(double simTimeInSec) {
        for (DynamicElement element : getDynamicElementsNearAerosolCloud()) {

            final Pedestrian pedestrian;
            if (element instanceof Pedestrian) {
                pedestrian = (Pedestrian) element;
            } else {
                log.error("The given object is not a subtype of Pedestrian");
                continue;
            }

            if (hasPedestianReachedAerosolCloud(pedestrian)) {
                notifyListenersAerosolCloudReached(pedestrian);
                // do something with pedestrian (accumulate "viral load")
            }
        }
    }

    private Collection<DynamicElement> getDynamicElementsNearAerosolCloud() {
        final Rectangle2D aerosolCloudBounds = aerosolCloud.getShape().getBounds2D();
        final VPoint centerOfAerosolCloud = new VPoint(aerosolCloudBounds.getCenterX(), aerosolCloudBounds.getCenterY());

        final double aerosolCloudRadius = aerosolCloud.getAerosolCloudRadius();
        final double aerosolCloudProximity = Math.max(aerosolCloudBounds.getHeight(), aerosolCloudBounds.getWidth()) + aerosolCloudRadius;

        final Collection<DynamicElement> elementsNearAerosolCloud = new LinkedList<>();

        List<Pedestrian> pedestrianNearAerosolCloud = topography.getSpatialMap(Pedestrian.class).getObjects(centerOfAerosolCloud, aerosolCloudProximity);
        elementsNearAerosolCloud.addAll(pedestrianNearAerosolCloud);

        return elementsNearAerosolCloud;
    }

    private boolean hasPedestianReachedAerosolCloud(Pedestrian pedestrian) {
        final double aerosolCloudRadius = aerosolCloud.getAerosolCloudRadius();
        final VPoint pedestrianPosition = pedestrian.getPosition();
        final VShape aerosolCloudShape = aerosolCloud.getShape();

        return aerosolCloudShape.contains(pedestrianPosition) || aerosolCloudShape.distance(pedestrianPosition) < aerosolCloudRadius;
    }

    private void notifyListenersAerosolCloudReached(final Pedestrian pedestrian) {
        for (AerosolCloudListener listener : aerosolCloud.getAerosolCloudListeners()) {
            listener.reachedAerosolCloud(aerosolCloud, pedestrian);
        }
    }
}
