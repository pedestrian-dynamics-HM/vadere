package org.vadere.util.importSumo.processors.inverseSpace;

import org.apache.commons.lang3.mutable.MutableInt;
import org.locationtech.jts.geom.Polygon;
import org.vadere.util.importSumo.ImportSumoGeometryUtils;
import org.vadere.util.importSumo.fileParsers.SumoUnoccupied;

import java.util.*;
import java.util.List;

public class InverseSpaceSumoProcessor {
    public List<SumoUnoccupied> calculateInvertedSpace(List<Polygon> allPolygons, MutableInt vadereIdProvider, SumoInvertSettings invertSettings) {
        List<Polygon> inversePolygons = ImportSumoGeometryUtils.calculateInverse(allPolygons, invertSettings);

        List<SumoUnoccupied> inversed = new ArrayList<>();
        for (Polygon polygon : inversePolygons) {
            inversed.add(new SumoUnoccupied(polygon, vadereIdProvider.getAndIncrement(), null));
        }

        return inversed;
    }
}
