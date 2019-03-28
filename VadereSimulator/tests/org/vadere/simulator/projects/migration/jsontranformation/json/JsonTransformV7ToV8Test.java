package org.vadere.simulator.projects.migration.jsontranformation.json;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.Before;
import org.junit.Test;
import org.vadere.simulator.entrypoints.Version;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.jsontranformation.JsonTransformation;
import org.vadere.simulator.projects.migration.jsontranformation.JsonTransformationTest;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.*;

public class JsonTransformV7ToV8Test extends JsonTransformationTest {

    JsonNode jsonOld;
    JsonNode jsonNew;
    JsonTransformation transformation;

    @Override
    public Path getTestDir() {
        return getPathFromResources("/migration/v07_to_v08");
    }

    @Before
    public void before(){
        String jsonStr = getTestFileAsString("rimea_04_flow_osm1_125_h.scenario");
        jsonOld = getJsonFromString(jsonStr);
        jsonNew = getJsonFromString(jsonStr);
        transformation = factory.getJsonTransformV7ToV8();
    }

    @Test
    public void TestVersionBump() throws MigrationException {
        jsonNew = transformation.applyAll(jsonNew);
        assertThat(pathMustExist(jsonOld, "release"), nodeHasText(Version.V0_7.label()));
        assertThat(pathMustExist(jsonNew, "release"), nodeHasText(Version.V0_8.label()));

        assertThat(path(jsonOld, "commithash"), missingNode());
        assertThat(pathMustExist(jsonNew, "commithash"), nodeHasText("warning: no commit hash"));
    }

    @Test
    public void FundamentalDiagramBProcessorTest() throws MigrationException {
        jsonNew = transformation.applyAll(jsonNew);

        Iterator<JsonNode> pIter = iteratorProcessorsByType(jsonNew,
                "org.vadere.simulator.projects.dataprocessing.processor.FundamentalDiagramBProcessor");
        ArrayList<Integer> areaId = new ArrayList<>();
        while (pIter.hasNext()){
            JsonNode processor = pIter.next();
            assertThat(path(processor, "attributes/measurementAreaId"), not(missingNode()));
            assertThat(path(processor, "attributes/measurementAreaId").isNumber(), is(true));
            assertThat(jsonNew, measurementAreaExists(path(processor, "attributes/measurementAreaId").asInt()));

            assertThat(path(processor, "attributes/measurementArea"), missingNode());
        }


    }
}