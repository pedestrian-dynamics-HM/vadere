package org.vadere.util.importSumo.fileParsers.roadNetwork;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.vadere.util.importSumo.fileParsers.SumoObject;
import org.vadere.util.importSumo.fileParsers.roadNetwork.Edges.SumoAgentType;
import org.vadere.util.importSumo.fileParsers.roadNetwork.Edges.SumoEdge;
import org.vadere.util.importSumo.fileParsers.roadNetwork.Edges.SumoLane;
import org.vadere.util.logging.Logger;

import javax.annotation.Nullable;
import java.util.List;

public class SumoJunction extends SumoObject {
    private static final Logger logger = Logger.getLogger(SumoJunction.class);

    private final String id;
    private final List<SumoEdge> walkingAreas;
    private final List<SumoEdge> crossings;
    private final List<SumoEdge> toEdges;
    private final List<SumoEdge> fromEdges;

    public SumoJunction(int vadereId, String id, @Nullable Polygon polygon, List<SumoEdge> walkingAreas, List<SumoEdge> crossings, List<SumoEdge> toEdges, List<SumoEdge> fromEdges) {
        super(polygon, vadereId, null);
        this.id = id;
        this.walkingAreas = walkingAreas;
        this.crossings = crossings;
        this.toEdges = toEdges;
        this.fromEdges = fromEdges;
    }

    public SumoJunction(int vadereId, SumoJunction sumoJunction, @Nullable Polygon polygon) {
        super(polygon, vadereId, null);
        this.id = sumoJunction.id;
        this.walkingAreas = sumoJunction.walkingAreas;
        this.crossings = sumoJunction.crossings;
        this.toEdges = sumoJunction.toEdges;
        this.fromEdges = sumoJunction.fromEdges;
    }

    @Override
    public String getSumoId() {
        return id;
    }

    @Override
    public String getTypedSumoId() {
        return "[junction] " + id;
    }

    public List<SumoEdge> getWalkingAreas() {
        return walkingAreas;
    }

    public List<SumoEdge> getCrossings() {
        return crossings;
    }

    @Nullable
    @Override
    public Polygon getPolygon() {
        return super.getPolygon();
    }

    public boolean isWalkingArea(){
        if(walkingAreas.isEmpty()) return false;
        if(polygon == null) return true;

        try{
            Geometry polygonFixed = polygon.buffer(0);

            Geometry walkingAreasMerged = new GeometryFactory().createPolygon();
            for(SumoEdge walkingArea : walkingAreas){
                Polygon walkingAreaMergedPolygon = walkingArea.getMergedPolygon();
                if(walkingAreaMergedPolygon == null) continue;

                if(walkingAreaMergedPolygon.equals(polygonFixed)) return true;

                walkingAreasMerged = walkingAreasMerged.union(walkingAreaMergedPolygon.buffer(0));
            }

            return walkingAreasMerged.equals(polygonFixed);
        }catch(Exception e){
            logger.error("Failed to determine isWalkingArea for {}", getTypedSumoId(), e);
            return false;
        }
    }

    public boolean usedByPedestrians(){
        int allowsPedestrians = 0;
        int totalLanes = 0;
        for (SumoEdge edge : toEdges){
            for (SumoLane lane : edge.getLanesFromLeftToRight()) {
                totalLanes++;
                if(lane.getAllowedAgents().contains(SumoAgentType.Pedestrian)){
                    allowsPedestrians++;
                }
            }
        }

        for (SumoEdge edge : fromEdges){
            for (SumoLane lane : edge.getLanesFromLeftToRight()) {
                totalLanes++;
                if(lane.getAllowedAgents().contains(SumoAgentType.Pedestrian)){
                    allowsPedestrians++;
                }
            }
        }

        return allowsPedestrians/(double)totalLanes > 0.6;
    }
}
