package org.vadere.util.importSumo.processors.increaseJunctionSize;

import org.locationtech.jts.geom.Polygon;
import org.vadere.util.importSumo.fileParsers.roadNetwork.SumoJunction;
import org.vadere.util.logging.Logger;

import java.util.ArrayList;
import java.util.List;

public class IncreaseJunctionSizesProcessor {
    private static final Logger logger = Logger.getLogger(IncreaseJunctionSizesProcessor.class);

    public void process(List<SumoJunction> junctions, IncreaseJunctionSizeSettings increaseJunctionSizeSettings){
        if(increaseJunctionSizeSettings.getIncreasedJunctionSize() == null){
            return;
        }

        for (SumoJunction junction : junctions) {
            if (junction.getPolygon() == null) {
                continue;
            }
            process(junction, increaseJunctionSizeSettings);
        }
    }

    private void process(SumoJunction junction, IncreaseJunctionSizeSettings increaseJunctionSizeSettings) {
        try{
            Polygon polygon = junction.getPolygon();
            polygon = (Polygon) polygon.buffer(increaseJunctionSizeSettings.getIncreasedJunctionSize());
            junction.setPolygonUnsafe(polygon);
        }catch (Exception e){
            logger.error("Could not increase junction size for " + junction.getTypedSumoId(), e);
        }
    }
}
