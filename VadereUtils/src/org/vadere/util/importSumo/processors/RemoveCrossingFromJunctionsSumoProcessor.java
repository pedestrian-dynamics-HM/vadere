package org.vadere.util.importSumo.processors;

import org.apache.commons.lang3.mutable.MutableInt;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Polygon;
import org.vadere.util.importSumo.ImportSumoGeometryUtils;
import org.vadere.util.importSumo.fileParsers.roadNetwork.Edges.SumoEdge;
import org.vadere.util.importSumo.fileParsers.roadNetwork.SumoJunction;
import org.vadere.util.importSumo.processors.inverseSpace.SumoInvertSettings;

import java.util.ArrayList;
import java.util.List;

public class RemoveCrossingFromJunctionsSumoProcessor {
    private final MutableInt vadereIdProvider;

    public RemoveCrossingFromJunctionsSumoProcessor(MutableInt vadereIdProvider) {
        this.vadereIdProvider = vadereIdProvider;
    }

    public List<SumoJunction> process(List<SumoJunction> junctions, SumoInvertSettings sumoAdvancedInvertSettings){
        List<SumoJunction> result = new ArrayList<>(junctions.size());

        for (SumoJunction junction : junctions) {
            if(junction.getPolygon() == null){
                result.add(junction);
                continue;
            }

            result.addAll(process(junction, sumoAdvancedInvertSettings));
        }
        return result;
    }

    private List<SumoJunction> process(SumoJunction junction, SumoInvertSettings sumoAdvancedInvertSettings){
        List<Polygon> allPolygons = new ArrayList<>(junction.getCrossings().size() + 1);

        Polygon junctionPolygon = junction.getPolygon();
        Envelope junctionBounds = junctionPolygon.getEnvelopeInternal();

        List<Polygon> invertedJunction = ImportSumoGeometryUtils.calculateInverse(junctionPolygon, junctionBounds, new SumoInvertSettings(sumoAdvancedInvertSettings.getCellSize()));
        allPolygons.addAll(invertedJunction);

        for (SumoEdge crossing : junction.getCrossings()) {
            allPolygons.add(crossing.getMergedPolygon());
        }

        for (SumoEdge walkingArea : junction.getWalkingAreas()) {
            allPolygons.add(walkingArea.getMergedPolygon());
        }

        List<Polygon> junctionParts = ImportSumoGeometryUtils.calculateInverse(allPolygons, junctionBounds, sumoAdvancedInvertSettings);

        List<SumoJunction> result = new ArrayList<>(junctionParts.size());
        for (Polygon junctionPart : junctionParts) {
            result.add(new SumoJunction(vadereIdProvider.getAndIncrement(), junction, junctionPart));
        }

        return result;
    }
}
