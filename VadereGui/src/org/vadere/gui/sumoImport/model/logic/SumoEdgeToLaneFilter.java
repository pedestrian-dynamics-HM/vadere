package org.vadere.gui.sumoImport.model.logic;

import org.jetbrains.annotations.NotNull;
import org.vadere.gui.sumoImport.model.data.SumoObjectType;
import org.vadere.util.importSumo.fileParsers.roadNetwork.Edges.SumoAgentType;
import org.vadere.util.importSumo.fileParsers.roadNetwork.Edges.SumoEdge;
import org.vadere.util.importSumo.fileParsers.roadNetwork.Edges.SumoEdgeFunction;
import org.vadere.util.importSumo.fileParsers.roadNetwork.Edges.SumoLane;

import java.util.List;
import java.util.NoSuchElementException;

public class SumoEdgeToLaneFilter implements Iterable<SumoLane> {
    private final List<SumoEdge> edges;
    private final SumoObjectType filter;

    public SumoEdgeToLaneFilter(List<SumoEdge> edges, SumoObjectType sumoObjectType) {
        this.edges = edges;
        this.filter = sumoObjectType;
    }

    @NotNull
    @Override
    public Iterator iterator() {
        return new Iterator(edges, filter);
    }

    public static class Iterator implements java.util.Iterator<SumoLane> {

        private final List<SumoEdge> edges;
        private final SumoObjectType filter;

        private int currentEdgeIndex = 0;
        private int nextEdgeIndex = -1;

        private int currentLaneIndex = -1;
        private int nextLaneIndex = -1;

        public Iterator(List<SumoEdge> edges, SumoObjectType filter) {
            this.edges = edges;
            this.filter = filter;

            findNext();
        }

        private void findNext(){
            int laneIndex = currentLaneIndex +1;
            for (int edgeIndex = currentEdgeIndex; edgeIndex < edges.size(); edgeIndex++) {
                SumoEdge edge = edges.get(edgeIndex);
                List<SumoLane> lanes = edge.getLanesFromLeftToRight();
                for (; laneIndex < lanes.size(); laneIndex++) {
                    SumoLane lane = lanes.get(laneIndex);
                    if(isValidForFilter(edge, lane)){
                        nextEdgeIndex = edgeIndex;
                        nextLaneIndex = laneIndex;
                        return;
                    }
                }
                laneIndex = 0;
            }

            nextEdgeIndex = -1;
            nextLaneIndex = -1;
        }

        private boolean isValidForFilter(SumoEdge edge, SumoLane lane) {
            switch (filter) {
                case Roads:
                    return !lane.getAllowedAgents().contains(SumoAgentType.Pedestrian)
                            && edge.getFunction() != SumoEdgeFunction.Crossing;
                case PedestrianRoadCrossings:
                    return edge.getFunction() == SumoEdgeFunction.Crossing;
                case PedestrianWalkways:
                    return lane.getAllowedAgents().contains(SumoAgentType.Pedestrian)
                            && edge.getFunction() != SumoEdgeFunction.Crossing
                            && edge.getFunction() != SumoEdgeFunction.WalkingArea;
                case PedestrianWalkingAreas:
                    return edge.getFunction() == SumoEdgeFunction.WalkingArea;
                case Structures:
                case RoadJunctions:
                    throw new NoSuchElementException(filter + " are not part of roads");
                default:
                    throw new IllegalStateException("Unexpected value: " + filter);
            }
        }

        @Override
        public boolean hasNext() {
            return nextLaneIndex != -1;
        }

        @Override
        public SumoLane next() {
            if(nextLaneIndex == -1){
                throw new NoSuchElementException();
            }

            currentEdgeIndex = nextEdgeIndex;
            currentLaneIndex = nextLaneIndex;
            findNext();

            SumoEdge edge = edges.get(currentEdgeIndex);
            List<SumoLane> lanes = edge.getLanesFromLeftToRight();
            SumoLane sumoLane = lanes.get(currentLaneIndex);
            return sumoLane;
        }
    }
}
