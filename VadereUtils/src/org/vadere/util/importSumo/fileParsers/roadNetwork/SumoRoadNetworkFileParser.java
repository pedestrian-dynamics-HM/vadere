package org.vadere.util.importSumo.fileParsers.roadNetwork;

import org.jetbrains.annotations.NotNull;
import org.locationtech.jts.geom.*;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.Nullable;
import org.vadere.util.importSumo.ImportSumoGeometryUtils;
import org.vadere.util.importSumo.fileParsers.SumoFileParserBase;
import org.vadere.util.importSumo.fileParsers.roadNetwork.Edges.*;
import org.vadere.util.logging.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SumoRoadNetworkFileParser extends SumoFileParserBase {
    private static final Logger logger = Logger.getLogger(SumoRoadNetworkFileParser.class);
    private final GeometryFactory geometryFactory = new GeometryFactory();

    private final Map<String, SumoEdgeTypeDefinition> edgeTypes = new HashMap<>();
    private static final Set<SumoAgentType> PEDESTRIAN_ONLY = Collections.singleton(SumoAgentType.Pedestrian);
    private final List<SumoEdge> allEdges = new ArrayList<>();
    private final List<SumoJunction> allJunction = new ArrayList<>();
    private final HashMap<String, List<SumoEdge>> parentIdToEdges =  new HashMap<>();
    private final HashMap<String, List<SumoEdge>> junctionIdToEdges =  new HashMap<>();
    private final HashMap<String, List<SumoEdge>> junctionIdFromEdges =  new HashMap<>();
    private final HashMap<String, SumoEdge> idToEdge =  new HashMap<>();

    Pattern getParentIdPattern = Pattern.compile(":(?<parentId>[a-zA-Z_0-9#]+)_[wcWC]\\d+(_\\d+)?");


    public SumoRoadNetworkFileParser(File parentDirectory, MutableInt continousVadereObjectId) {
        super(parentDirectory, continousVadereObjectId);
    }

    @Override
    public String fileName() {
        return "osm.net.xml.gz";
    }

    @Override
    protected void parseInternal(Element root) {
        NodeList childNodes = root.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            switch (node.getNodeName()) {
                case "type":
                    parseType(node);
                    break;
                case "edge":
                    parseEdge(node);
                    break;
                case "junction":
                    parseJunction(node);
                    break;
                case "connection":
                    parseConnection(node);
                    break;
            }
        }
    }

    private void parseType(Node node) {
        NamedNodeMap attributes = node.getAttributes();

        Node idNode = attributes.getNamedItem("id");
        if (idNode == null) return;
        String id = idNode.getNodeValue();

        Set<SumoAgentType> allowedAgents = parseAllowedAgents(attributes);

        Double width = parseNullableAttributeDouble(attributes, "width");
        Double sidewalkWidth = parseNullableAttributeDouble(attributes, "sideWalkWidth");
        int numLanes = parseAttributeInt(attributes, "numLanes", 1);
        double speed = parseAttributeDouble(attributes, "speed", 0.0);

        edgeTypes.put(id, new SumoEdgeTypeDefinition(allowedAgents, width, sidewalkWidth, numLanes, speed));
    }

    private void parseEdge(Node node) {
        NamedNodeMap attributes = node.getAttributes();

        SumoEdgeTypeDefinition edgeType = getEdgeType(attributes);
        Set<SumoAgentType> allowedAgents = parseAllowedAgents(attributes);

        Double edgeWidth = getEdgeWidth(attributes, edgeType);
        Double sidewalkWidth = edgeType == null? null: edgeType.getSidewalkWidth();

        String edgeId = attributes.getNamedItem("id").getNodeValue();

        String fromJunctionId = parseNullableAttributeString(attributes, "from");
        String toJunctionId = parseNullableAttributeString(attributes, "to");

        NodeList childNodes = node.getChildNodes();

        SumoEdgeFunction function = SumoEdgeFunction.get(attributes.getNamedItem("function"));
        switch (function) {
            case Internal:
                // An internal edge lies within an intersection
                // it connects and incoming normal edge with an outgoing normal edge
                return;
            case Connector:
                // The edge is a macroscopic connector - not a part of the real world road network.
                // Still, within the simulation, no distinction is made between "connector" roads and "normal" nodes.
                return;
        }

        List<SumoLane> lanesFromLeftToRight = new ArrayList<>();
        SumoEdge edge = new SumoEdge(edgeId, function, lanesFromLeftToRight, fromJunctionId, toJunctionId);

        for (int i = 0; i < childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            switch (childNode.getNodeName()) {
                case "lane":
                    SumoLane parsedLane = parseLane(edge, childNode, edgeWidth, function, allowedAgents);
                    if(parsedLane == null){
                        continue;
                    }
                    lanesFromLeftToRight.add(parsedLane);
                    break;
            }
        }
        if (sidewalkWidth != null && !lanesFromLeftToRight.isEmpty()) {
            addSidewalkToTheRight(edge, lanesFromLeftToRight, sidewalkWidth);
        }

        allEdges.add(edge);
        idToEdge.put(edgeId, edge);

        if(edge.getFromJunctionId() != null){
            junctionIdFromEdges.computeIfAbsent(edge.getFromJunctionId(), k->new ArrayList<>()).add(edge);
        }
        if(edge.getToJunctionId() != null){
            junctionIdToEdges.computeIfAbsent(edge.getToJunctionId(), k->new ArrayList<>()).add(edge);
        }

        Matcher matcher = getParentIdPattern.matcher(edgeId);
        if(matcher.matches()){
            String parentId = matcher.group("parentId");
            parentIdToEdges.computeIfAbsent(parentId, k -> new ArrayList<>()).add(edge);
        }
    }

    private void addSidewalkToTheRight(SumoEdge edge, List<SumoLane> allLanes, double sidewalkWidth) {
        SumoLane last = allLanes.get(allLanes.size()-1);

        LineString lineString = last.getLineString();
        Double width = last.getWidth();
        if(lineString == null || width == null){
            return;
        }

        // distance from center of the last road to center of the pedestrian road
        double shiftDistance = sidewalkWidth / 2 + width / 2;

        LineString shiftedLine = ImportSumoGeometryUtils.shiftLineRight(lineString, shiftDistance);
        allLanes.add(new SumoLane(vadereObjectId.incrementAndGet(), edge.getId() + "_Sidewalk", edge, sidewalkWidth, shiftedLine, PEDESTRIAN_ONLY));
    }

    @Nullable
    private SumoLane parseLane(SumoEdge edge, Node node, @Nullable Double edgeWidth, SumoEdgeFunction function, Set<SumoAgentType> allowedAgents) {
        NamedNodeMap attributes = node.getAttributes();

        allowedAgents = parseAllowedAgents(attributes, allowedAgents);

        Node shape = attributes.getNamedItem("shape");
        if(shape == null) return null;

        String laneId = attributes.getNamedItem("id").getNodeValue();

        Double width =  parseNullableAttributeDouble(attributes, "width");
        if(width == null) width = edgeWidth;
        if(width == null && !function.isPolygon()) width = 3.2; // sumo default lane width

        Coordinate[] coordinates = parseCoordinates(shape.getNodeValue());
        if(function.isPolygon()){
            coordinates = closeCoordinatesForPolygon(coordinates);
            if(coordinates == null) return null;
        }

        if(function.isPolygon() && coordinates.length <= 3){
            logger.warn("Warning: Polygon coordinates for lane {} are too small", laneId);
            return null;
        }

        Geometry geometry = function.isPolygon()
                ? geometryFactory.createPolygon(coordinates)
                : geometryFactory.createLineString(coordinates);

        return new SumoLane(vadereObjectId.incrementAndGet(), laneId, edge, width, geometry, allowedAgents);
    }

    @Nullable
    private Double getEdgeWidth(NamedNodeMap attributes, @Nullable SumoEdgeTypeDefinition edgeType) {
        Double edgeWidth = parseNullableAttributeDouble(attributes, "width");
        if(edgeWidth != null) return edgeWidth;
        if(edgeType != null) return edgeType.getWidth();
        return null;
    }

    @Nullable
    private SumoEdgeTypeDefinition getEdgeType(NamedNodeMap attributes) {
        Node edgeTypeNode = attributes.getNamedItem("type");
        if (edgeTypeNode == null) {
            return null;
        }
        return edgeTypes.get(edgeTypeNode.getNodeValue());
    }

    private static Set<SumoAgentType> parseAllowedAgents(NamedNodeMap attributes) {
        return parseAllowedAgents(attributes, null);
    }

    private static Set<SumoAgentType> parseAllowedAgents(NamedNodeMap attributes, Set<SumoAgentType> fallback) {
        Node allow = attributes.getNamedItem("allow"); // white-list
        if(allow != null){
            return parseAgentTypes(allow);
        }else{
            Node disallow = attributes.getNamedItem("disallow"); // black-list
            if (disallow == null && fallback != null) {
                return fallback;
            }
            Set<SumoAgentType> disallowedAgents = parseAgentTypes(disallow);

            return SumoAgentType.subtractFromAll(disallowedAgents);
        }
    }

    private static Set<SumoAgentType> parseAgentTypes(Node node) {
        if(node == null) return new HashSet<>();

        String elementsValue = node.getNodeValue();
        String[] elements = elementsValue.split(" ");

        HashSet<SumoAgentType> result = new HashSet<>();
        for (String element : elements) {
            SumoAgentType agentType = SumoAgentType.get(element);
            result.add(agentType);
        }

        return result;
    }

    private void parseJunction(Node node) {
        NamedNodeMap attributes = node.getAttributes();
        String junctionId = attributes.getNamedItem("id").getNodeValue();

        Node shape = attributes.getNamedItem("shape");
        Polygon polygon = null;
        if(shape != null){
            Coordinate[] coordinates = parseCoordinates(shape.getNodeValue());
            coordinates = closeCoordinatesForPolygon(coordinates);
            if(coordinates == null) return;

            polygon = geometryFactory.createPolygon(coordinates);
        }

        List<SumoEdge> walkingAreas = getChildren(junctionId, SumoEdgeFunction.WalkingArea);
        List<SumoEdge> crossings = getChildren(junctionId, SumoEdgeFunction.Crossing);

        List<SumoEdge> toEdges = junctionIdToEdges.get(junctionId);
        if(toEdges == null)
            toEdges = new ArrayList<>(0);

        List<SumoEdge> fromEdges = junctionIdFromEdges.get(junctionId);
        if(fromEdges == null)
            fromEdges = new ArrayList<>(0);

        SumoJunction junction = new SumoJunction(vadereObjectId.incrementAndGet(), junctionId, polygon, walkingAreas, crossings, fromEdges, toEdges);
        allJunction.add(junction);
    }

    @NotNull
    private List<SumoEdge> getChildren(String junctionId, SumoEdgeFunction edgeFunction) {
        String parentKey = junctionId;
        if (!parentIdToEdges.containsKey(parentKey)) {
            return new ArrayList<>(0);
        }
        List<SumoEdge> connectedEdges = parentIdToEdges.get(parentKey);
        return connectedEdges.stream()
                .filter(sumoEdge -> sumoEdge.getFunction() == edgeFunction)
                .toList();
    }

    private void parseConnection(Node node) {
        NamedNodeMap attributes = node.getAttributes();

        String fromEdgeId = attributes.getNamedItem("from").getNodeValue();
        String toEdgeId = attributes.getNamedItem("to").getNodeValue();

        if(!idToEdge.containsKey(fromEdgeId) ||  !idToEdge.containsKey(toEdgeId)){
            return;
        }

        SumoEdge fromEdge = idToEdge.get(fromEdgeId);
        SumoEdge toEdge = idToEdge.get(toEdgeId);

        int fromLane = parseAttributeInt(attributes, "fromLane", 0);
        int toLane = parseAttributeInt(attributes, "toLane", 0);

        String trafficLightId = parseNullableAttributeString(attributes, "tl");
        Integer trafficLightLinkIndex = parseNullableAttributeInt(attributes, "linkIndex");

        SumoConnection.Direction direction = SumoConnection.Direction.invalid;
        String directionString = parseNullableAttributeString(attributes, "dir");
        if(directionString != null){
            direction = SumoConnection.Direction.fromSumoIdentifier(directionString);
        }

        SumoConnection.State state = SumoConnection.State.invalid;
        String stateString = parseNullableAttributeString(attributes, "state");
        if(stateString != null){
            state = SumoConnection.State.fromSumoIdentifier(stateString);
        }

        SumoConnection sumoConnection = new SumoConnection(fromEdge, toEdge, fromLane, toLane, trafficLightId, trafficLightLinkIndex, direction, state);
        fromEdge.addOutboundConnection(sumoConnection);
        toEdge.addInboundConnection(sumoConnection);
    }

    public List<SumoEdge> getAllEdges() {
        return allEdges;
    }

    public List<SumoJunction> getAllJunction() {
        return allJunction;
    }

    @Override
    public void clear() {
        edgeTypes.clear();
        allEdges.clear();
        allJunction.clear();
        parentIdToEdges.clear();
        idToEdge.clear();
    }
}
