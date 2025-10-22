package org.vadere.gui.sumoImport.control;

import org.apache.commons.lang3.mutable.MutableInt;
import org.locationtech.jts.geom.Polygon;
import org.vadere.gui.sumoImport.model.data.SumoImportObjectFlag;
import org.vadere.gui.sumoImport.model.data.SumoObjectImportSetting;
import org.vadere.gui.sumoImport.model.data.SumoObjectType;
import org.vadere.gui.sumoImport.model.logic.SumoEdgeToLaneFilter;
import org.vadere.gui.sumoImport.model.logic.SumoImportParsers;
import org.vadere.gui.topographycreator.model.TopographyElementFactory;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.importSumo.fileParsers.SumoObject;
import org.vadere.util.importSumo.fileParsers.SumoUnoccupied;
import org.vadere.util.importSumo.fileParsers.roadNetwork.Edges.SumoEdge;
import org.vadere.util.importSumo.fileParsers.roadNetwork.SumoJunction;
import org.vadere.util.importSumo.fileParsers.structures.SumoStructure;
import org.vadere.util.importSumo.processors.inverseSpace.InverseSpaceSumoProcessor;
import org.vadere.util.importSumo.processors.inverseSpace.SumoInvertSettings;
import org.vadere.util.importSumo.settings.SumoAdvancedImportSettings;
import org.vadere.util.importSumo.settings.SumoInvertGroup;
import org.vadere.util.io.CollectionUtils;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SumoImportApplyImportSettings {
    private final InverseSpaceSumoProcessor invertedGroupProcessor = new InverseSpaceSumoProcessor();
    private final MutableInt vadereIdProvider;

    public SumoImportApplyImportSettings(MutableInt vadereIdProvider) {
        this.vadereIdProvider = vadereIdProvider;
    }

    public record Result(List<SumoObject> allObstacles, List<ScenarioElement> scenarioElements){}

    public Result apply(List<SumoObjectImportSetting> importSettings, SumoAdvancedImportSettings advancedImportSettings,
                        SumoImportParsers.TopologyResult parseSumoTopologyResult, List<SumoJunction> junctionsWithoutCrossings) {
        List<SumoStructure> structures = parseSumoTopologyResult.structures();
        List<SumoJunction> pedestrianJunctions = parseSumoTopologyResult.pedestrianJunctions();
        List<SumoJunction> roadJunctions = parseSumoTopologyResult.roadJunctions();
        List<SumoEdge> allEdges = parseSumoTopologyResult.allEdges();

        List<SumoObject> allObstacles = new ArrayList<>();
        List<ScenarioElement> additionalScenarioElements = new ArrayList<>();

        Map<SumoInvertGroup, List<SumoObject>> invertGroupObjects = new Hashtable<>();
        Map<SumoInvertGroup, SumoObjectImportSetting> importSettingDictionary = new Hashtable<>();

        for (SumoObjectImportSetting importSetting : importSettings) {
            SumoObjectType sumoObjectType = importSetting.getSumoObjectType();
            if(sumoObjectType != null){
                switch (sumoObjectType) {
                    case Structures:
                        handleObjectType(structures, importSetting, allObstacles);
                        handleInvertGroup(structures, importSetting, invertGroupObjects);
                        handleAdditionalScenarioElements(structures, importSetting, additionalScenarioElements);
                        break;
                    case PedestrianWalkingAreas:
                        handleObjectType(pedestrianJunctions, importSetting, allObstacles);
                        handleInvertGroup(pedestrianJunctions, importSetting, invertGroupObjects);
                        handleAdditionalScenarioElements(pedestrianJunctions, importSetting, additionalScenarioElements);
                        //fall through
                    case Roads:
                    case PedestrianRoadCrossings:
                    case PedestrianWalkways:
                        SumoEdgeToLaneFilter lanes = new SumoEdgeToLaneFilter(allEdges, sumoObjectType);
                        handleObjectType(lanes, importSetting, allObstacles);
                        handleInvertGroup(lanes, importSetting, invertGroupObjects);
                        handleAdditionalScenarioElements(lanes, importSetting, additionalScenarioElements);
                        break;
                    case RoadJunctions:
                        handleObjectType(roadJunctions, importSetting, allObstacles);
                        handleInvertGroup(roadJunctions, importSetting, invertGroupObjects);
                        handleAdditionalScenarioElements(roadJunctions, importSetting, additionalScenarioElements);
                        break;
                    case RoadJunctionsWithoutCrossings:
                        handleObjectType(junctionsWithoutCrossings, importSetting, allObstacles);
                        handleInvertGroup(junctionsWithoutCrossings, importSetting, invertGroupObjects);
                        handleAdditionalScenarioElements(junctionsWithoutCrossings, importSetting, additionalScenarioElements);
                        break;
                    default:
                        throw new IllegalStateException("Unknown SumoObjectType " + sumoObjectType);
                }
            }

            SumoInvertGroup sumoInvertGroup = importSetting.getInvertGroup();
            if (sumoInvertGroup != null) {
                importSettingDictionary.put(sumoInvertGroup, importSetting);
            }
        }

        for (Map.Entry<SumoInvertGroup, SumoObjectImportSetting> entry : importSettingDictionary.entrySet()) {
            SumoInvertGroup invertGroup = entry.getKey();
            if(!invertGroupObjects.containsKey(invertGroup)){
                continue;
            }

            List<Polygon> allOccupyingPolygons = invertGroupObjects
                    .get(invertGroup)
                    .stream()
                    .map(SumoObject::getPolygon)
                    .collect(Collectors.toList());

            SumoInvertSettings settings = advancedImportSettings.getInvertGroupSettings().get(invertGroup);
            List<SumoUnoccupied> unoccupiedGroup = invertedGroupProcessor.calculateInvertedSpace(allOccupyingPolygons, vadereIdProvider, settings);

            handleObjectType(unoccupiedGroup, entry.getValue(), allObstacles);
            handleAdditionalScenarioElements(unoccupiedGroup, entry.getValue(), additionalScenarioElements);
        }

        if(allObstacles.isEmpty() && additionalScenarioElements.isEmpty()){
            throw new IllegalStateException("Aborted due to empty result");
        }

        return new Result(allObstacles, additionalScenarioElements);
    }

    private static<T extends SumoObject> void handleObjectType(
            Iterable<T> objects,
            SumoObjectImportSetting importSetting,
            List<SumoObject> allObstacles) {
        for (SumoImportObjectFlag sumoObjectFlag : importSetting.getSumoObjectFlags()) {
            switch (sumoObjectFlag) {
                case Obstacle:
                    for (SumoObject sumoObject : objects) {
                        if(sumoObject.getPolygon() == null){
                            continue;
                        }
                        allObstacles.add(sumoObject);
                    }
                    break;
            }
        }
    }

    private static<T extends SumoObject> void handleInvertGroup(
            Iterable<T> objects,
            SumoObjectImportSetting importSetting,
            Map<SumoInvertGroup, List<SumoObject>> sumoInvertGroupListMap) {

        SumoInvertGroup invertGroup = importSetting.getTargetInvertGroup();
        if(invertGroup == null){
            return;
        }
        for (SumoObject sumoObject : objects) {
            if(sumoObject.getPolygon() == null){
                continue;
            }
            CollectionUtils.addToValueList(sumoInvertGroupListMap, invertGroup, sumoObject);
        }
    }

    private static<T extends SumoObject> void handleAdditionalScenarioElements(
            Iterable<T> objects,
            SumoObjectImportSetting importSetting,
            List<ScenarioElement> additionalScenarioElements) {
        for (ScenarioElementType scenarioElementType : importSetting.getScenarioElementTypes()) {
            for (SumoObject sumoObject : objects) {
                Polygon jtsPolygon = sumoObject.getPolygon();
                if (jtsPolygon == null) continue;

                VPolygon polygon = GeometryUtils.toVaderePolygon(jtsPolygon);

                ScenarioElement scenarioElement = TopographyElementFactory.getInstance().createScenarioShape(scenarioElementType, polygon);
                additionalScenarioElements.add(scenarioElement);
            }
        }
    }
}
