package org.vadere.simulator.projects.migration.jsontranformation.json;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.Test;
import org.vadere.util.version.Version;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.jsontranformation.JsonTransformationTest;

import java.nio.file.Path;
import java.util.ArrayList;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.*;

public class V07ToV08Test extends JsonTransformationTest {

//    JsonNode jsonOld;
//    JsonNode jsonNew;
//    JsonTransformation transformation;

    @Override
    public Path getTestDir() {
        return getPathFromResources("/migration/v07_to_v08");
    }




    @Test
    public void testTyp1() throws MigrationException{
        String[] processorTypes = {
                "org.vadere.simulator.projects.dataprocessing.processor.FundamentalDiagramBProcessor",  // type 1 (measurementArea -> measurementAreaId)
                "org.vadere.simulator.projects.dataprocessing.processor.FundamentalDiagramCProcessor",  // type 1 (measurementArea -> measurementAreaId)
        };

        String jsonStr = getTestFileAsString("typ1.scenario");
        JsonNode jsonOld = getJsonFromString(jsonStr);
        JsonNode jsonNew = getJsonFromString(jsonStr);
        TargetVersionV0_8 transformation = factory.getTargetVersionV0_8();
        jsonNew = transformation.applyMeasurementAreaType1(jsonNew);

        for (String type : processorTypes) {
            ArrayList<JsonNode> processor =
                    getProcessorsByType(jsonNew, type);
            for (JsonNode p : processor) {
                assertThat(p, fieldChanged("attributes/",
                        "measurementArea","measurementAreaId", JsonNode::isNumber));

                int measurementAreaId = pathMustExist(p, "attributes/measurementAreaId").asInt();
                assertThat(jsonNew, measurementAreaExists(measurementAreaId));
            }
        }
    }


    @Test
    public void testTyp2() throws MigrationException{
        String[] processorTypes = {
                "org.vadere.simulator.projects.dataprocessing.processor.FundamentalDiagramDProcessor", //type 2 (measurementArea -> measurementAreaId, voronoiArea -> voronoiMeasurementAreaId)
                "org.vadere.simulator.projects.dataprocessing.processor.FundamentalDiagramEProcessor", //type 2 (measurementArea -> measurementAreaId, voronoiArea -> voronoiMeasurementAreaId)
                "org.vadere.simulator.projects.dataprocessing.processor.AreaDensityVoronoiProcessor", // type 2 (measurementArea -> measurementAreaId, voronoiArea -> voronoiMeasurementAreaId)
        };

        String jsonStr = getTestFileAsString("typ2.scenario");
        JsonNode jsonOld = getJsonFromString(jsonStr);
        JsonNode jsonNew = getJsonFromString(jsonStr);
        TargetVersionV0_8 transformation = factory.getTargetVersionV0_8();
        jsonNew = transformation.applyMeasurementAreaType2(jsonNew);

        for (String type : processorTypes) {
            ArrayList<JsonNode> processor =
                    getProcessorsByType(jsonNew, type);
            for (JsonNode p : processor) {
                assertThat(p, fieldChanged("attributes/",
                        "measurementArea","measurementAreaId", JsonNode::isNumber));
                assertThat(p, fieldChanged("attributes/",
                        "voronoiArea","voronoiMeasurementAreaId", JsonNode::isNumber));


                int measurementAreaId = pathMustExist(p, "attributes/measurementAreaId").asInt();
                int voronoiMeasurementAreaId = pathMustExist(p, "attributes/voronoiMeasurementAreaId").asInt();
                assertThat(measurementAreaId == voronoiMeasurementAreaId, is(true));
                assertThat(jsonNew, measurementAreaExists(measurementAreaId));
                assertThat(jsonNew, measurementAreaExists(voronoiMeasurementAreaId));
            }
        }
    }


    @Test
    public void testTyp4() throws MigrationException{
        String[] processorTypes = {
                "org.vadere.simulator.projects.dataprocessing.processor.PedestrianWaitingEndTimeProcessor", // type 3 (waitingArea -> waitingAreaId)
                "org.vadere.simulator.projects.dataprocessing.processor.PedestrianWaitingTimeProcessor" //type 3 (waitingArea -> waitingAreaId)
        };

        String jsonStr = getTestFileAsString("typ4.scenario");
        JsonNode jsonOld = getJsonFromString(jsonStr);
        JsonNode jsonNew = getJsonFromString(jsonStr);
        TargetVersionV0_8 transformation = factory.getTargetVersionV0_8();
        jsonNew = transformation.applyMeasurementAreaType4(jsonNew);

        for (String type : processorTypes) {
            ArrayList<JsonNode> processor =
                    getProcessorsByType(jsonNew, type);
            for (JsonNode p : processor) {
                assertThat(p, fieldChanged("attributes/",
                        "waitingArea","waitingAreaId", JsonNode::isNumber));


                int waitingAreaId = pathMustExist(p, "attributes/waitingAreaId").asInt();
                assertThat(jsonNew, measurementAreaExists(waitingAreaId));
            }
        }
    }


    @Test
    public void testTyp5() throws MigrationException{
        String[] processorTypes = {
                "org.vadere.simulator.projects.dataprocessing.processor.PedestrianCrossingTimeProcessor", //type 3 (waitingArea -> waitingAreaId, measurementArea -> measurementAreaId)
        };

        String jsonStr = getTestFileAsString("typ4.scenario");
        JsonNode jsonOld = getJsonFromString(jsonStr);
        JsonNode jsonNew = getJsonFromString(jsonStr);
        TargetVersionV0_8 transformation = factory.getTargetVersionV0_8();
        jsonNew = transformation.applyMeasurementAreaType5(jsonNew);

        for (String type : processorTypes) {
            ArrayList<JsonNode> processor =
                    getProcessorsByType(jsonNew, type);
            for (JsonNode p : processor) {
                assertThat(p, fieldChanged("attributes/",
                        "waitingArea","waitingAreaId", JsonNode::isNumber));
                assertThat(p, fieldChanged("attributes/",
                        "measurementArea","measurementAreaId", JsonNode::isNumber));


                int waitingAreaId = pathMustExist(p, "attributes/waitingAreaId").asInt();
                int measurementAreaId = pathMustExist(p, "attributes/measurementAreaId").asInt();
                assertThat(waitingAreaId == measurementAreaId, is(true));
                assertThat(jsonNew, measurementAreaExists(waitingAreaId));
                assertThat(jsonNew, measurementAreaExists(measurementAreaId));
            }
        }
    }




    private void testVersionBump(JsonNode jsonOld, JsonNode jsonNew) throws MigrationException {

        assertThat(pathMustExist(jsonOld, "release"), nodeHasText(Version.V0_7.label()));
        assertThat(pathMustExist(jsonNew, "release"), nodeHasText(Version.V0_8.label()));

        assertThat(path(jsonOld, "commithash"), missingNode());
        assertThat(pathMustExist(jsonNew, "commithash"), nodeHasText("warning: no commit hash"));
    }

//    @Test
//    public void FundamentalDiagramBProcessorTest() throws MigrationException {
//        jsonNew = transformation.applyAll(jsonNew);
//
//        Iterator<JsonNode> pIter = iteratorProcessorsByType(jsonNew,
//                "org.vadere.simulator.projects.dataprocessing.processor.FundamentalDiagramBProcessor");
//        ArrayList<Integer> areaId = new ArrayList<>();
//        while (pIter.hasNext()){
//            JsonNode processor = pIter.next();
//            assertThat(path(processor, "attributes/measurementAreaId"), not(missingNode()));
//            assertThat(path(processor, "attributes/measurementAreaId").isNumber(), is(true));
//            assertThat(jsonNew, measurementAreaExists(path(processor, "attributes/measurementAreaId").asInt()));
//
//            assertThat(path(processor, "attributes/measurementArea"), missingNode());
//        }
//
//
//    }
}