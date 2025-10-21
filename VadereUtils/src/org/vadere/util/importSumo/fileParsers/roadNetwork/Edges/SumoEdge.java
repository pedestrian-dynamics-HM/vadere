package org.vadere.util.importSumo.fileParsers.roadNetwork.Edges;


import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.union.UnaryUnionOp;
import org.vadere.util.importSumo.fileParsers.SumoObject;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class SumoEdge {
    private final String id;
    private final SumoEdgeFunction function;
    private final List<SumoLane> lanesFromLeftToRight;
    private final List<SumoConnection> inBoundConnection = new ArrayList<>();
    private final List<SumoConnection> outBoundConnection = new ArrayList<>();

    @Nullable private final String fromJunctionId;
    @Nullable private final String toJunctionId;

    public SumoEdge(String sumoId, SumoEdgeFunction function, List<SumoLane> lanesFromLeftToRight, @Nullable String fromJunctionId, @Nullable String toJunctionId) {
        this.id = sumoId;
        this.function = function;
        this.lanesFromLeftToRight = lanesFromLeftToRight;
        this.fromJunctionId = fromJunctionId;
        this.toJunctionId = toJunctionId;
    }

    public List<SumoLane> getLanesFromLeftToRight() {
        return lanesFromLeftToRight;
    }

    public SumoEdgeFunction getFunction() {
        return function;
    }

    public String getId() {
        return id;
    }

    public String getTypedSumoId() {
        return "[edge] " + id;
    }

    @Nullable
    public Polygon getMergedPolygon() {
        if(lanesFromLeftToRight.isEmpty()) return null;

        if(lanesFromLeftToRight.size() == 1) return lanesFromLeftToRight.get(0).getPolygon();

        Geometry merged = UnaryUnionOp.union(lanesFromLeftToRight.stream().map(SumoObject::getPolygon).toList());
        if (!(merged instanceof Polygon)) {
            return null;
        }
        return (Polygon) merged;
    }

    @Nullable
    public String getFromJunctionId() {
        return fromJunctionId;
    }

    @Nullable
    public String getToJunctionId() {
        return toJunctionId;
    }

    public List<SumoConnection> getInBoundConnection() {
        return inBoundConnection;
    }

    public List<SumoConnection> getOutBoundConnection() {
        return outBoundConnection;
    }

    public void addInboundConnection(SumoConnection sumoConnection) {
        inBoundConnection.add(sumoConnection);
    }

    public void addOutboundConnection(SumoConnection sumoConnection) {
        outBoundConnection.add(sumoConnection);
    }
}
