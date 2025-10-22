package org.vadere.util.importSumo.fileParsers.roadNetwork.Edges;

import org.w3c.dom.Node;

public enum SumoEdgeFunction {
    Default(false),
    Crossing(false),
    WalkingArea(true),

    // ignored after parsing:
    Internal(false),
    Connector(false);

    private boolean isPolygon;

    SumoEdgeFunction(boolean usesPolygon) {
        isPolygon = usesPolygon;
    }

    public boolean isPolygon(){
        return isPolygon;
    }

    public static SumoEdgeFunction get(Node node) {
        if(node == null){
            return Default;
        }

        String nodeValue = node.getNodeValue().toLowerCase();
        switch (nodeValue){
            case "crossing":
                return SumoEdgeFunction.Crossing;
            case "internal":
                return SumoEdgeFunction.Internal;
            case "connector":
                return SumoEdgeFunction.Connector;
            case "walkingarea":
                return SumoEdgeFunction.WalkingArea;
            default:
                return SumoEdgeFunction.Default;
        }
    }
}
