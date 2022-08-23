package org.vadere.simulator.projects.migration.jsontranformation.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.vadere.annotation.factories.migrationassistant.MigrationTransformation;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.jsontranformation.SimpleJsonTransformation;
import org.vadere.util.version.Version;

import java.util.Iterator;


@MigrationTransformation(targetVersionLabel = "2.4")
public class TargetVersionV2_4 extends SimpleJsonTransformation {

    private final String pathSrc = "scenario/topography/sources";
    private final String pathTrg = "scenario/topography/targets";
    private final String distributionFieldSrc = "interSpawnTimeDistribution";
    private final String distributionFieldTrg = "waitingTimeDistribution";

    public TargetVersionV2_4() {
        super(Version.V2_4);
    }

    @Override
    protected void initDefaultHooks() {
        addPostHookLast(this::renameUpdateFrequencyToTimeInterval);
        addPostHookLast(this::renameSpawnFrequencyToTimeInterval);
        addPostHookLast(this::renameNumberPedsToOccurences);
        addPostHookLast(this::renameSpawnTimeToEventTime);
        addPostHookLast(this::renameSpawnsPerIntervalToEventsPerInterval);
        addPostHookLast(this::sort);
    }


    private void renameDistributionFieldInSource(JsonNode node, String distribution, String paramOld, String paramNew) throws MigrationException {
        if (!path(node, pathSrc).isMissingNode()) {
            Iterator<JsonNode> iter = iteratorSources(node);
            rename(distribution, paramOld, paramNew, iter, distributionFieldSrc);
        }
    }

    private void renameDistributionFieldInTarget(JsonNode node,String distribution,String paramOld, String paramNew) throws MigrationException {
        if (!path(node, pathTrg).isMissingNode()) {
            Iterator<JsonNode> iter = iteratorTargets(node);
            rename(distribution, paramOld, paramNew, iter, distributionFieldTrg);
        }
    }
    private void rename(String distribution, String paramOld, String paramNew, Iterator<JsonNode> iter, String distributionFieldSrc) {
        while (iter.hasNext()) {
            JsonNode target = iter.next();
            var n =target.get(distributionFieldSrc);
            if (n != null){
                if(n.asText().equals(distribution)){
                    renameField((ObjectNode) target.get("distributionParameters"), paramOld, paramNew);
                }
            }

        }
    }



    private JsonNode renameSpawnsPerIntervalToEventsPerInterval(JsonNode node) throws MigrationException {

        String paramOld = "spawnsPerInterval";
        String paramNew = "eventsPerInterval";
        String distribution = "timeSeries";

        renameDistributionFieldInSource(node,distribution,paramOld, paramNew);
        renameDistributionFieldInTarget(node,distribution,paramOld, paramNew);
        return node;
    }


    /**
     * ConstantParameter: updateFrequency -> timeInterval
     */
    private JsonNode renameUpdateFrequencyToTimeInterval(JsonNode node) throws  MigrationException{
        String paramOld = "updateFrequency";
        String paramNew = "timeInterval";
        String distribution = "constant";
        renameDistributionFieldInSource(node,distribution,paramOld, paramNew);
        renameDistributionFieldInTarget(node,distribution,paramOld, paramNew);
        return node;
    }
    /**
     * LinearInterpolationParameter: spawnFrequency -> timeInterval
     */
    private JsonNode renameSpawnFrequencyToTimeInterval(JsonNode node) throws  MigrationException{
        String paramOld = "spawnFrequency";
        String paramNew = "timeInterval";
        String distribution = "linearInterpolation";
        renameDistributionFieldInSource(node,distribution,paramOld, paramNew);
        renameDistributionFieldInTarget(node,distribution,paramOld, paramNew);
        return node;
    }

    /**
     * PoissonParameter: numberPedsPerSecond -> occurrencesPerSecond
     */
    private JsonNode renameNumberPedsToOccurences(JsonNode node) throws  MigrationException{
        String paramOld = "numberPedsPerSecond";
        String paramNew = "occurrencesPerSecond";
        String distribution = "poisson";
        renameDistributionFieldInSource(node,distribution,paramOld, paramNew);
        renameDistributionFieldInTarget(node,distribution,paramOld, paramNew);
        return node;
    }
    /**
     * singleSpawn : spawnTime -> eventTime
     */
    private JsonNode renameSpawnTimeToEventTime(JsonNode node) throws  MigrationException{
        String paramOld = "spawnTime";
        String paramNew = "eventTime";
        String distribution = "singleSpawn";
        renameDistributionFieldInSource(node,distribution,paramOld, paramNew);
        renameDistributionFieldInTarget(node,distribution,paramOld, paramNew);

        renameSingleDistrSource(node);

        renameSingleDistrTarget(node);

        return node;
    }

    private void renameSingleDistrSource(JsonNode node) throws MigrationException {
        if (!path(node, pathSrc).isMissingNode()) {
            Iterator<JsonNode> iter = iteratorSources(node);
            while (iter.hasNext()) {
                JsonNode target = iter.next();
                JsonNode distr = target.get(distributionFieldSrc);
                if (distr!= null) {
                    if(distr.asText().equals("singleSpawn")){
                        ((ObjectNode)target).put(distributionFieldSrc,"singleEvent");
                    }
                }
            }
        }
    }

    private void renameSingleDistrTarget(JsonNode node) throws MigrationException {
        if (!path(node, pathTrg).isMissingNode()) {
            Iterator<JsonNode> iter = iteratorTargets(node);
            while (iter.hasNext()) {
                JsonNode target = iter.next();
                JsonNode distr = target.get(distributionFieldTrg);
                if (distr!= null) {
                    if(distr.asText().equals("singleSpawn")){
                        ((ObjectNode)target).put(distributionFieldTrg,"singleEvent");
                    }
                }
            }
        }
    }

}
