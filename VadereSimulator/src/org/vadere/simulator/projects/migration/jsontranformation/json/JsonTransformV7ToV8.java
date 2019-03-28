package org.vadere.simulator.projects.migration.jsontranformation.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.vadere.simulator.entrypoints.Version;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.jsontranformation.AbstractJsonTransformation;
import org.vadere.simulator.projects.migration.jsontranformation.SimpleJsonTransformation;
import org.vadere.state.util.StateJsonConverter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class JsonTransformV7ToV8 extends SimpleJsonTransformation {

    ObjectMapper mapper;

    public JsonTransformV7ToV8() {
        super(Version.V0_7);
        this.mapper = StateJsonConverter.getMapper();
    }

    @Override
    protected void initDefaultHooks() {
        addPostHookLast(AbstractJsonTransformation::sort);
    }

    @Override
    public JsonNode applyTransformation(JsonNode node) throws MigrationException {

        return super.applyTransformation(node);
    }

    private JsonNode removeShapeFromDataProcessors(JsonNode scenarioFile) throws MigrationException, IOException {
        ArrayList<JsonNode> processor =
                getProcessorsByType(scenarioFile, "org.vadere.simulator.projects.dataprocessing.processor.FundamentalDiagramBProcessor");
        for (JsonNode p : processor) {
            JsonNode attr = pathMustExist(p, "attributes");
            JsonNode measurementArea = path(attr, "measurementArea");
            if (!measurementArea.isMissingNode()){
                int measurementAreaId = transformShapeToMeasurementArea(scenarioFile, measurementArea, mapper);
                remove(attr, "measurementArea");
                addIntegerField(attr, "measurementAreaId", measurementAreaId);
            }
        }

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
