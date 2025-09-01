package org.vadere.util.importSumo.fileParsers.structures;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.apache.commons.lang3.mutable.MutableInt;
import org.vadere.util.importSumo.fileParsers.SumoFileParserBase;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SumoPolygonFileParser extends SumoFileParserBase {
    private final List<SumoStructure> structures = new ArrayList<>();
    private final GeometryFactory geometryFactory = new GeometryFactory();
    public SumoPolygonFileParser(File parentDirectory, MutableInt vadereObjectId) {
        super(parentDirectory, vadereObjectId);
    }

    @Override
    public String fileName() {
        return "osm.poly.xml.gz";
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
                case "poly":
                    ParsePoly(node);
                    break;
            }
        }
    }

    private void ParsePoly(Node node) {
        NamedNodeMap attributes = node.getAttributes();
        String fullType = attributes.getNamedItem("type").getNodeValue();
        String[] types = fullType.split("\\.");
        if(types.length!=2){
            return;
        }

        String generalType = types[0];
        String specificType = types[1];
        switch (generalType) {
            case "building":
                ParseStructure(node, attributes, specificType);
                break;
        }
    }

    private void ParseStructure(Node node, NamedNodeMap attributes, String specificType) {
        Node shapeNode = attributes.getNamedItem("shape");
        if(shapeNode==null) return;

        Node colorNode = attributes.getNamedItem("color");
        Optional<Color> color = parseColor(colorNode);

        Boolean filledPolygon = parseNullableAttributeBoolean(attributes, "fill");
        if(filledPolygon == null || !filledPolygon){
            return;
        }

        String id = parseNullableAttributeString(attributes, "id");
        if(id == null) return;

        Coordinate[] coordinates = parseCoordinates(shapeNode.getNodeValue());
        coordinates = closeCoordinatesForPolygon(coordinates);
        if(coordinates == null) return;

        Polygon polygon = geometryFactory.createPolygon(coordinates);

        if(polygon.getArea() < 1){
            logger.warn("polygon {} with area is less than 1m ignored", id);
            return; // less than 1 sqr meter
        }

        structures.add(new SumoStructure(vadereObjectId.incrementAndGet(), id, SumoStructure.Type.Building, polygon, color.orElse(null)));
    }

    public List<SumoStructure> getStructures() {
        return structures;
    }

    @Override
    public void clear() {
        structures.clear();
    }
}
