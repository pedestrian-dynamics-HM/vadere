package org.vadere.simulator.projects.migration.jsontranformation.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.vadere.annotation.factories.migrationassistant.MigrationTransformation;
import org.vadere.simulator.entrypoints.Version;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.jsontranformation.SimpleJsonTransformation;
import org.vadere.state.util.StateJsonConverter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.stream.Collectors;

@MigrationTransformation(targetVersionLabel = "0.8")
public class JsonTransformV7ToV8 extends SimpleJsonTransformation {

    ObjectMapper mapper;

    public JsonTransformV7ToV8() {
        super(Version.V0_8);
        this.mapper = StateJsonConverter.getMapper();
    }

    @Override
    protected void initDefaultHooks() {
        addPostHookLast(this::measurementAreaType1);
        addPostHookLast(this::measurementAreaType2);
        addPostHookLast(this::measurementAreaType3);
        addPostHookLast(this::measurementAreaType4);
        addPostHookLast(this::addCommitHashWarningIfMissing);
        addPostHookLast(this::sort);
    }

    @Override
    public JsonNode applyTransformation(JsonNode node) throws MigrationException {

        return super.applyTransformation(node);
    }

    private void migrate_measurementArea(JsonNode scenarioFile, JsonNode p) throws MigrationException {
        JsonNode attr = pathMustExist(p, "attributes");
        // find old field name
        JsonNode measurementArea = path(attr, "measurementArea");
        if (!measurementArea.isMissingNode()){
            // search existing or create new MeasurementArea and link processor to id.
            int measurementAreaId = transformShapeToMeasurementArea(scenarioFile, measurementArea, mapper);
            remove(attr, "measurementArea");
            addIntegerField(attr, "measurementAreaId", measurementAreaId);
        }
    }

    private void migrate_voronoiArea(JsonNode scenarioFile, JsonNode p) throws MigrationException{
        JsonNode attr = pathMustExist(p, "attributes");
        JsonNode measurementArea = path(attr, "voronoiArea");
        if (!measurementArea.isMissingNode()){
            // search existing or create new MeasurementArea and link processor to id.
            int measurementAreaId = transformShapeToMeasurementArea(scenarioFile, measurementArea, mapper);
            remove(attr, "voronoiArea");
            addIntegerField(attr, "voronoiMeasurementAreaIdArea", measurementAreaId);
        }
    }


    private void migrate_waitingArea(JsonNode scenarioFile, JsonNode p) throws MigrationException{
        JsonNode attr = pathMustExist(p, "attributes");
        JsonNode measurementArea = path(attr, "waitingArea");
        if (!measurementArea.isMissingNode()){
            // search existing or create new MeasurementArea and link processor to id.
            int measurementAreaId = transformShapeToMeasurementArea(scenarioFile, measurementArea, mapper);
            remove(attr, "waitingArea");
            addIntegerField(attr, "waitingAreaId", measurementAreaId);
        }
    }



    public JsonNode measurementAreaType1(JsonNode scenarioFile) throws MigrationException{
        String[] processorTypes = {
            "org.vadere.simulator.projects.dataprocessing.processor.FundamentalDiagramBProcessor",  // todo type 1 (measurementArea -> measurementAreaId)
            "org.vadere.simulator.projects.dataprocessing.processor.FundamentalDiagramCProcessor",  //todo type 1 (measurementArea -> measurementAreaId)
        };

        for (String type : processorTypes) {
            ArrayList<JsonNode> processor =
                    getProcessorsByType(scenarioFile, type);
            for (JsonNode p : processor) {
                //
                migrate_measurementArea(scenarioFile, p);
                //
            }
        }
        return scenarioFile;
    }

    public JsonNode measurementAreaType2(JsonNode scenarioFile) throws MigrationException{
        String[] processorTypes = {
            "org.vadere.simulator.projects.dataprocessing.processor.FundamentalDiagramDProcessor", //todo type 2 (measurementArea -> measurementAreaId, voronoiArea -> voronoiMeasurementAreaIdArea)
            "org.vadere.simulator.projects.dataprocessing.processor.FundamentalDiagramEProcessor", //todo type 2 (measurementArea -> measurementAreaId, voronoiArea -> voronoiMeasurementAreaIdArea)
        };

        for (String type : processorTypes) {
            ArrayList<JsonNode> processor =
                    getProcessorsByType(scenarioFile, type);
            for (JsonNode p : processor) {
                //
                migrate_measurementArea(scenarioFile, p);
                //
                migrate_voronoiArea(scenarioFile, p);
                //
            }
        }
        return scenarioFile;
    }

    public JsonNode measurementAreaType3(JsonNode scenarioFile) throws MigrationException{
        String[] processorTypes = {
            "org.vadere.simulator.projects.dataprocessing.processor.AreaDensityVoronoiProcessor", // todo type 4 (voronoiArea -> voronoiMeasurementAreaIdArea)
        };

        for (String type : processorTypes) {
            ArrayList<JsonNode> processor =
                    getProcessorsByType(scenarioFile, type);
            for (JsonNode p : processor) {
                //
                migrate_voronoiArea(scenarioFile, p);
                //
            }
        }
        return scenarioFile;
    }

    public JsonNode measurementAreaType4(JsonNode scenarioFile) throws MigrationException{
        String[] processorTypes = {
            "org.vadere.simulator.projects.dataprocessing.processor.PedestrianCrossingTimeProcessor", //todo type 3 (waitingArea -> waitingAreaId)
            "org.vadere.simulator.projects.dataprocessing.processor.PedestrianWaitingEndTimeProcessor", // todo type 3 (waitingArea -> waitingAreaId)
            "org.vadere.simulator.projects.dataprocessing.processor.PedestrianWaitingTimeProcessor" //todo type 3 (waitingArea -> waitingAreaId)
        };

        for (String type : processorTypes) {
            ArrayList<JsonNode> processor =
                    getProcessorsByType(scenarioFile, type);
            for (JsonNode p : processor) {
                //
                migrate_waitingArea(scenarioFile, p);
                //
            }
        }
        return scenarioFile;
    }

    private JsonNode removeShapeFromDataProcessors(JsonNode scenarioFile) throws MigrationException {


        return scenarioFile;
    }

    public static void main(String[] arg) throws Exception {
        BufferedReader r = new BufferedReader(
                new FileReader("/home/lphex/hm.d/vadere/VadereModelTests/TestOSM/scenarios/rimea_04_flow_osm1_125_h.scenario"));
        String jsonStr = r.lines().collect(Collectors.joining("\n"));
        ObjectMapper mapper = StateJsonConverter.getMapper();
        JsonNode jsonNode = StateJsonConverter.deserializeToNode(jsonStr);
        JsonTransformV7ToV8 transformation = new JsonTransformV7ToV8();

        JsonNode newScenario = transformation.applyAll(jsonNode);

        System.out.print(StateJsonConverter.getPrettyWriter().writeValueAsString(newScenario));

    }
}
