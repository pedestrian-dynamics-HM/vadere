package org.vadere.util.importSumo.fileParsers.roadNetwork.Edges;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.vadere.util.importSumo.ImportSumoGeometryUtils;
import org.vadere.util.importSumo.fileParsers.SumoObject;

import javax.annotation.Nullable;
import java.util.Set;

public class SumoLane extends SumoObject {
    private final String sumoId;

    @Nullable
    private Double width;
    @Nullable
    private LineString lineString;
    private final Set<SumoAgentType> allowedAgents;
    private final SumoEdge parent;

    public SumoLane(int vadereId, String sumoId, SumoEdge parent, Double width, Geometry geometry, Set<SumoAgentType> allowedAgents) {
        super(ToPolygon(width, geometry), vadereId, null);
        this.sumoId = sumoId;
        this.parent = parent;
        this.width = width;
        this.allowedAgents = allowedAgents;

        if(geometry instanceof LineString) {
            lineString = (LineString) geometry;
        }
    }

    private static Polygon ToPolygon(Double width, Geometry geometry) {
        if(geometry instanceof LineString lineString) {
            assert width != null : "width is null for a polygon";
            return ImportSumoGeometryUtils.expandLineToWidth(lineString, width);
        }

        if(geometry instanceof Polygon) {
            return (Polygon) geometry;
        }

        throw new RuntimeException("Geometry "+geometry.getGeometryType()+" not supported");
    }

    public SumoEdge getParent() { return parent; }

    @Nullable
    public Double getWidth() {
        return width;
    }

    public Set<SumoAgentType> getAllowedAgents() {
        return allowedAgents;
    }

    @Nullable
    public LineString getLineString() {
        return lineString;
    }

    public String getSumoId() {
        return sumoId;
    }

    @Override
    public String getTypedSumoId() { return parent.getTypedSumoId() +" [lane] " + sumoId; }
}
