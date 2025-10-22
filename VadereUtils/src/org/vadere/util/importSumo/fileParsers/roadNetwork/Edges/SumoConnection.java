package org.vadere.util.importSumo.fileParsers.roadNetwork.Edges;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class SumoConnection {
    private final SumoEdge soure;
    private final SumoEdge target;

    private final int fromLane;
    private final int toLane;

    @Nullable
    private final String trafficLightId;
    @Nullable
    private final Integer trafficLightLinkId;

    private final Direction direction;
    private final State state;

    public SumoConnection(SumoEdge source, SumoEdge target, int fromLane, int toLane, @Nullable String trafficLightId, @Nullable Integer trafficLightLinkId, Direction direction, State state) {
        this.soure = source;
        this.target = target;
        this.fromLane = fromLane;
        this.toLane = toLane;
        this.trafficLightId = trafficLightId;
        this.trafficLightLinkId = trafficLightLinkId;
        this.direction = direction;
        this.state = state;
    }

    public SumoEdge getTarget() {
        return target;
    }

    public SumoLane getTargetLane(){
        return target.getLanesFromLeftToRight().get(toLane);
    }

    public SumoEdge getSource() {
        return target;
    }

    public SumoLane getSourceLane(){
        return soure.getLanesFromLeftToRight().get(fromLane);
    }

    public int getFromLane() {
        return fromLane;
    }

    public int getToLane() {
        return toLane;
    }

    @Nullable
    public String getTrafficLightId() {
        return trafficLightId;
    }

    @Nullable
    public Integer getTrafficLightLinkId() {
        return trafficLightLinkId;
    }

    public Direction getDirection() {
        return direction;
    }

    public State getState() {
        return state;
    }

    public enum Direction{
        straight("s"),
        turn("t"),
        left("l"),
        right("r"),
        partiallyLeft("L"),
        partiallyRight("R"),
        invalid("invalid");

        private final String sumoIdentifier;
        private static final Map<String, Direction> sumoIdentifierLookup = new HashMap<>();

        static {
            for (Direction s : Direction.values()) {
                sumoIdentifierLookup.put(s.sumoIdentifier, s);  // or use a custom key
            }
        }

        Direction(String sumoIdentifier) {
            this.sumoIdentifier = sumoIdentifier;
        }

        public static Direction fromSumoIdentifier(String sumoIdentifier) {
            if(!sumoIdentifierLookup.containsKey(sumoIdentifier)){
                return invalid;
            }

            return sumoIdentifierLookup.get(sumoIdentifier);
        }
    }

    public enum State{
        deadEnd("-"),
        equal("="),
        minorLink("m"),
        majorLink("M"),

        trafficLightControllerOff("O"),
        trafficLightYellowFlashing("o"),
        trafficLightYellowMinorLink("y"),
        trafficLightYellowMajorLink("y"),
        trafficLightRed("r"),
        trafficLightGreen("g"),
        trafficLightGreenMajor("G"),

        invalid("invalid");

        private final String sumoIdentifier;
        private static final Map<String, State> sumoIdentifierLookup = new HashMap<>();

        static {
            for (State s : State.values()) {
                sumoIdentifierLookup.put(s.sumoIdentifier, s);  // or use a custom key
            }
        }

        State(String sumoIdentifier) {
            this.sumoIdentifier = sumoIdentifier;
        }

        public static State fromSumoIdentifier(String sumoIdentifier) {
            if(!sumoIdentifierLookup.containsKey(sumoIdentifier)){
                return invalid;
            }
            return sumoIdentifierLookup.get(sumoIdentifier);
        }
    }
}
