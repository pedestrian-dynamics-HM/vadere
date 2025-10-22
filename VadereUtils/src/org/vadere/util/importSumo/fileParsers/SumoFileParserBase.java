package org.vadere.util.importSumo.fileParsers;

import org.locationtech.jts.geom.Coordinate;
import org.apache.commons.lang3.mutable.MutableInt;
import org.vadere.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

public abstract class SumoFileParserBase {
    protected final Logger logger = Logger.getLogger(this.getClass());

    private final File parentDirectory;
    protected final MutableInt vadereObjectId;

    public abstract String fileName();

    private static final int GZIP_HEADER_BYTE1 = 0x1f;
    private static final int GZIP_HEADER_BYTE2 = 0x8b;

    public SumoFileParserBase(File parentDirectory, MutableInt continousVadereObjectId) {
        this.parentDirectory = parentDirectory;
        this.vadereObjectId = continousVadereObjectId;
    }

    public boolean parse() {
        return isGzipCompressed(getFile()) ? ParseGZipFile(getFile()) : ParseUncompressedFile(getFile());
    }

    private boolean ParseUncompressedFile(File file) {
        try (
                FileInputStream fileInputStream = new FileInputStream(file);
                InputStreamReader reader = new InputStreamReader(fileInputStream);
                BufferedReader bufferedReader = new BufferedReader(reader)) {

            parseXML(bufferedReader);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean ParseGZipFile(File file) {
        try (
                FileInputStream fileInputStream = new FileInputStream(file);
                GZIPInputStream gzipInputStream = new GZIPInputStream(fileInputStream);
                InputStreamReader reader = new InputStreamReader(gzipInputStream);
                BufferedReader bufferedReader = new BufferedReader(reader)) {

            parseXML(bufferedReader);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void parseXML(BufferedReader bufferedReader) throws ParserConfigurationException, SAXException, IOException {
        // Parse the XML from the decompressed file
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(bufferedReader));

        Element root = document.getDocumentElement();
        parseInternal(root);
    }

    protected abstract void parseInternal(Element root);

    public static boolean isGzipCompressed(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            // Read the first two bytes to check for GZIP signature
            int byte1 = fis.read();
            int byte2 = fis.read();

            // Check if the first two bytes match the GZIP signature (0x1f 0x8b)
            return byte1 == GZIP_HEADER_BYTE1 && byte2 == GZIP_HEADER_BYTE2;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public File getFile() {
        Path path = parentDirectory.toPath();
        Path filePath = path.resolve(fileName());
        return filePath.toFile();
    }

    /**
     * parses coordinates assuming format: UTM Zone 32 (EPSG:32632), unit: meters.
     */
    protected Coordinate[] parseCoordinates(String coordinates) {
        String[] coordinatePairs = coordinates.split("\\s+"); // split by spaces
        ArrayList<Coordinate> parsedCoordinates = new ArrayList<>(coordinatePairs.length + 1);

        for (String coordinatePair : coordinatePairs) {
            String[] latLon = coordinatePair.split(",");
            if (latLon.length != 2) {
                logger.error("Invalid coordinate pair format: {}", coordinatePair);
                continue;
            }

            try {
                Coordinate coordinate = new Coordinate(Double.parseDouble(latLon[0].trim()), Double.parseDouble(latLon[1].trim()));
                parsedCoordinates.add(coordinate);
            } catch (NumberFormatException e) {
                logger.error("parsing coordinates: {}", coordinatePair);
            }
        }

        return parsedCoordinates.toArray(new Coordinate[0]);
    }

    @Nullable
    protected Coordinate[] closeCoordinatesForPolygon(Coordinate[] coordinate) {
        if(coordinate.length <= 2) return null;
        if(coordinate[0] == coordinate[coordinate.length - 1]) return coordinate;

        Coordinate[] result = new Coordinate[coordinate.length + 1];
        System.arraycopy(coordinate, 0, result, 0, coordinate.length);
        result[result.length - 1] = coordinate[0];
        return result;
    }

    protected static Optional<Color> parseColor(Node colorNode) {
        if (colorNode == null) {
            return Optional.empty();
        }

        String colorString = colorNode.getNodeValue();

        if (colorString.contains(",")) {
            String[] parts = colorString.split(",");

            try {
                // Parse the components
                int r = Integer.parseInt(parts[0].trim());
                int g = Integer.parseInt(parts[1].trim());
                int b = Integer.parseInt(parts[2].trim());

                return Optional.of(new Color(r, g, b));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }

        Color color;
        if (colorString.startsWith("#")) {
            color = Color.decode(colorString);
        } else {
            color = Color.getColor(colorString);
        }
        return Optional.ofNullable(color);
    }

    @Nullable
    protected Double parseNullableAttributeDouble(NamedNodeMap attributes, String name) {
        Node node = attributes.getNamedItem(name);
        if (node == null) {
            return null;
        }
        return Double.parseDouble(node.getNodeValue());
    }

    @Nullable
    protected Boolean parseNullableAttributeBoolean(NamedNodeMap attributes, String name) {
        Node node = attributes.getNamedItem(name);
        if (node == null) {
            return null;
        }

        return switch (node.getNodeValue().toLowerCase()) {
            case "true", "1" -> true;
            case "false", "0" -> false;
            default -> null;
        };
    }

    @Nullable
    protected String parseNullableAttributeString(NamedNodeMap attributes, String name) {
        Node node = attributes.getNamedItem(name);
        if (node == null) {
            return null;
        }
        return node.getNodeValue();
    }

    @Nullable
    protected Integer parseNullableAttributeInt(NamedNodeMap attributes, String name) {
        Node node = attributes.getNamedItem(name);
        if (node == null) {
            return null;
        }
        return Integer.parseInt(node.getNodeValue());
    }

    protected double parseAttributeDouble(NamedNodeMap attributes, String name, double defaultValue) {
        Node node = attributes.getNamedItem(name);
        if (node == null) {
            return defaultValue;
        }
        return Double.parseDouble(node.getNodeValue());
    }

    protected int parseAttributeInt(NamedNodeMap attributes, String name, int defaultValue) {
        Node node = attributes.getNamedItem(name);
        if (node == null) {
            return defaultValue;
        }
        return Integer.parseInt(node.getNodeValue());
    }


    public abstract void clear();
}
