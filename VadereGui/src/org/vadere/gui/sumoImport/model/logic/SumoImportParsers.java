package org.vadere.gui.sumoImport.model.logic;

import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.NotNull;
import org.vadere.util.importSumo.fileParsers.SumoFileParserBase;
import org.vadere.util.importSumo.fileParsers.roadNetwork.Edges.SumoEdge;
import org.vadere.util.importSumo.fileParsers.roadNetwork.Edges.SumoLane;
import org.vadere.util.importSumo.fileParsers.roadNetwork.SumoJunction;
import org.vadere.util.importSumo.fileParsers.roadNetwork.SumoRoadNetworkFileParser;
import org.vadere.util.importSumo.fileParsers.structures.SumoPolygonFileParser;
import org.vadere.util.importSumo.fileParsers.structures.SumoStructure;
import org.vadere.util.logging.Logger;

import java.io.File;
import java.util.*;

public class SumoImportParsers {
    private static final Logger logger = Logger.getLogger(SumoImportParsers.class);

    private final List<SumoFileParserBase> allParsers = new ArrayList<>();

    private SumoPolygonFileParser polygonParser;
    private SumoRoadNetworkFileParser networkParser;
    private MutableInt vadereIdProvider;

    public SumoImportParsers(File sumoDirectoryPath, MutableInt vadereIdProvider) {
        this.vadereIdProvider = vadereIdProvider;
        createParsers(sumoDirectoryPath);
    }

    private void createParsers(File sumoDirectoryPath) {
        polygonParser = new SumoPolygonFileParser(sumoDirectoryPath, vadereIdProvider);
        allParsers.add(polygonParser);
        networkParser = new SumoRoadNetworkFileParser(sumoDirectoryPath, vadereIdProvider);
        allParsers.add(networkParser);
    }

    public record TopologyResult(List<SumoEdge> allEdges, List<SumoJunction> allJunctions, List<SumoJunction> roadJunctions, List<SumoJunction> pedestrianJunctions, List<SumoStructure> structures){}

    public TopologyResult ParseSumoFiles() {
        for (SumoFileParserBase allParser : allParsers) {
            allParser.parse();
        }

        List<SumoEdge> allEdges = networkParser.getAllEdges();
        List<SumoJunction> allJunction = networkParser.getAllJunction();

        List<SumoJunction> allJunctionsWithoutWalkingAreas = getSumoJunctions(allEdges, allJunction);

        List<SumoJunction> roadJunctions = new ArrayList<>();
        List<SumoJunction> pedestrianJunction = new ArrayList<>();
        for (SumoJunction junction : allJunctionsWithoutWalkingAreas) {
            if(junction.usedByPedestrians()){
                pedestrianJunction.add(junction);
            }else{
                roadJunctions.add(junction);
            }
        }
        List<SumoStructure> structures = polygonParser.getStructures();

        return new TopologyResult(allEdges, allJunction, roadJunctions, pedestrianJunction, structures);
    }

    @NotNull
    private static List<SumoJunction> getSumoJunctions(List<SumoEdge> allEdges, List<SumoJunction> allJunction) {
        log(allEdges, allJunction);

        List<SumoJunction> allJunctionsWithoutWalkingAreas = allJunction.stream().filter(sumoJunction -> !sumoJunction.isWalkingArea()).toList();
        return allJunctionsWithoutWalkingAreas;
    }

    private static void log(List<SumoEdge> allEdges, List<SumoJunction> allJunction) {
        for (SumoEdge edge : allEdges) {
            logger.info(edge.getTypedSumoId());
            for (SumoLane sumoLane : edge.getLanesFromLeftToRight()) {
                logger.info("  {} -> {}", sumoLane.getTypedSumoId(), sumoLane.getVadereId());
            }
        }
        for (SumoJunction junction : allJunction) {
            logger.info("  {} -> {}", junction.getTypedSumoId(), junction.getVadereId());
        }
    }

    public boolean isValidSumoDirectory(File sumoDirectoryPath) {
        if (sumoDirectoryPath == null || !sumoDirectoryPath.exists()) {
            return false;
        }
        return allParsers.stream().allMatch(parser -> parser.getFile().exists());
    }

    public void clear() {
        for (SumoFileParserBase allParser : allParsers) {
            allParser.clear();
        }
    }
}
