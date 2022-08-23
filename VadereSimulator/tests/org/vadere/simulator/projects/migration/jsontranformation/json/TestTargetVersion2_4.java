package org.vadere.simulator.projects.migration.jsontranformation.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.Assert;
import org.vadere.simulator.projects.migration.MigrationException;

import org.junit.Test;


import java.io.IOException;

public class TestTargetVersion2_4 {
    private static JsonNode rootNode = null;

    @Test
    public void testRenameUpdateFrequency(){
        initTree();
        initNodeWithValueString("interSpawnTimeDistribution","constant");
        initNodeWithValueString("waitingTimeDistribution","constant");
        var distsrc =(ObjectNode) rootNode.findPath("sources").findPath("distributionParameters");
        var disttrg =(ObjectNode) rootNode.findPath("targets").findPath("distributionParameters");
        distsrc.put("updateFrequency",0.0);
        disttrg.put("updateFrequency",0.0);

        var transformer = new TargetVersionV2_4();
        try {
            transformer.applyPostHooks(rootNode);
        } catch (MigrationException e) {
            throw new RuntimeException(e);
        }


        var nodesrc = distsrc.findPath("timeInterval");
        var nodetrg = disttrg.findPath("timeInterval");
        Assert.assertFalse(nodesrc.isMissingNode());
        Assert.assertFalse(nodetrg.isMissingNode());

        nodesrc = distsrc.findPath("updateFrequency");
        nodetrg = disttrg.findPath("updateFrequency");
        Assert.assertTrue(nodesrc.isMissingNode());
        Assert.assertTrue(nodetrg.isMissingNode());

    }

    @Test
    public void testRenameSpawnFrequency(){
        initTree();
        initNodeWithValueString("interSpawnTimeDistribution","linearInterpolation");
        initNodeWithValueString("waitingTimeDistribution","linearInterpolation");
        var distsrc =(ObjectNode) rootNode.findPath("sources").findPath("distributionParameters");
        var disttrg =(ObjectNode) rootNode.findPath("targets").findPath("distributionParameters");
        distsrc.put("spawnFrequency",0.0);
        disttrg.put("spawnFrequency",0.0);

        var transformer = new TargetVersionV2_4();
        try {
            transformer.applyPostHooks(rootNode);
        } catch (MigrationException e) {
            throw new RuntimeException(e);
        }

        var nodesrc = distsrc.findPath("timeInterval");
        var nodetrg = disttrg.findPath("timeInterval");
        Assert.assertFalse(nodesrc.isMissingNode());
        Assert.assertFalse(nodetrg.isMissingNode());

        nodesrc = distsrc.findPath("spawnFrequency");
        nodetrg = disttrg.findPath("spawnFrequency");
        Assert.assertTrue(nodesrc.isMissingNode());
        Assert.assertTrue(nodetrg.isMissingNode());
    }

    @Test
    public void testRenameNumberOfPeds(){
        initTree();
        initNodeWithValueString("interSpawnTimeDistribution","poisson");
        initNodeWithValueString("waitingTimeDistribution","poisson");
        var distsrc =(ObjectNode) rootNode.findPath("sources").findPath("distributionParameters");
        var disttrg =(ObjectNode) rootNode.findPath("targets").findPath("distributionParameters");
        distsrc.put("numberPedsPerSecond",0.0);
        disttrg.put("numberPedsPerSecond",0.0);

        var transformer = new TargetVersionV2_4();
        try {
            transformer.applyPostHooks(rootNode);
        } catch (MigrationException e) {
            throw new RuntimeException(e);
        }

        var nodesrc = distsrc.findPath("occurrencesPerSecond");
        var nodetrg = disttrg.findPath("occurrencesPerSecond");
        Assert.assertFalse(nodesrc.isMissingNode());
        Assert.assertFalse(nodetrg.isMissingNode());

        nodesrc = distsrc.findPath("numberPedsPerSecond");
        nodetrg = disttrg.findPath("numberPedsPerSecond");
        Assert.assertTrue(nodesrc.isMissingNode());
        Assert.assertTrue(nodetrg.isMissingNode());
    }

    @Test
    public void testRenameSpawnTime(){
        initTree();
        initNodeWithValueString("interSpawnTimeDistribution","singleSpawn");
        initNodeWithValueString("waitingTimeDistribution","singleSpawn");
        var distsrc =(ObjectNode) rootNode.findPath("sources").findPath("distributionParameters");
        var disttrg =(ObjectNode) rootNode.findPath("targets").findPath("distributionParameters");
        distsrc.put("spawnTime",0.0);
        disttrg.put("spawnTime",0.0);

        var transformer = new TargetVersionV2_4();
        try {
            transformer.applyPostHooks(rootNode);
        } catch (MigrationException e) {
            throw new RuntimeException(e);
        }

        var nodesrc = distsrc.findPath("eventTime");
        var nodetrg = disttrg.findPath("eventTime");
        Assert.assertFalse(nodesrc.isMissingNode());
        Assert.assertFalse(nodetrg.isMissingNode());

        nodesrc = distsrc.findPath("spawnTime");
        nodetrg = disttrg.findPath("spawnTime");
        Assert.assertTrue(nodesrc.isMissingNode());
        Assert.assertTrue(nodetrg.isMissingNode());
    }

    @Test
    public void testSingleTimeDistributionRenamed(){
        initTree();
        initNodeWithValueString("interSpawnTimeDistribution","singleSpawn");
        initNodeWithValueString("waitingTimeDistribution","singleSpawn");


        var transformer = new TargetVersionV2_4();
        try {
            transformer.applyPostHooks(rootNode);
        } catch (MigrationException e) {
            throw new RuntimeException(e);
        }

        var distsrc =(TextNode) rootNode.findPath("sources").findPath("interSpawnTimeDistribution");
        var disttrg =(TextNode) rootNode.findPath("targets").findPath("waitingTimeDistribution");
        Assert.assertTrue((distsrc.asText()).equals("singleEvent"));
        Assert.assertTrue((disttrg.asText()).equals("singleEvent"));
    }
    private static void initNodeWithValueString(String nodeName, String value) {
        var parentNode = (ObjectNode) rootNode.findParent(nodeName);
        parentNode.put(nodeName,value);
    }


    private void initTree() {
        String json = "{\n" +
                "  \"scenario\" : {\n" +
                "    \"topography\" : {\n" +
                "      \"sources\" : [ {\n" +
                "        \"interSpawnTimeDistribution\" : \"someDistribution\",\n" +
                "        \"distributionParameters\" : {\n" +
                "        }\n" +
                "      }],\n" +
                "      \"targets\" : [ {\n" +
                "        \"waitingTimeDistribution\" : \"someDistribution\",\n" +
                "        \"distributionParameters\" : {\n" +
                "        }\n" +
                "      }]\n" +
                "    }\n" +
                "  }\n" +
                "}";
        var mapper = new ObjectMapper();
        try {
            rootNode = mapper.readTree(json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
