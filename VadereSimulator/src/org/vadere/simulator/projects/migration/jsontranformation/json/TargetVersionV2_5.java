package org.vadere.simulator.projects.migration.jsontranformation.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.vadere.annotation.factories.migrationassistant.MigrationTransformation;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.jsontranformation.SimpleJsonTransformation;
import org.vadere.util.version.Version;

import java.util.Iterator;
import java.util.function.Function;


@MigrationTransformation(targetVersionLabel = "2.5")
public class TargetVersionV2_5 extends SimpleJsonTransformation {

    @FunctionalInterface
    public interface CheckedFunction<T, R> {
        R apply(T t) throws MigrationException;
    }

    private final String PATH_SRC = "scenario/topography/sources";
    private final String PATH_TRG = "scenario/topography/targets";
    private final String DIST_FIELD_SRC = "interSpawnTimeDistribution";
    private final String DIST_FIELD_TRG = "waitingTimeDistribution";

    private final String PARAM_FIELD ="distributionParameters";

    private final String TYPE_STRING = "type";
    private final String PARAM_STRING = "parameters";

    public TargetVersionV2_5() {
        super(Version.V2_5);
    }

    @Override
    protected void initDefaultHooks() {
        addPostHookLast(this::mergeDistributionNameWithParameters);
        /* rearange nodes of distributions */
        addPostHookLast(this::sort);
    }

    private JsonNode mergeDistributionNameWithParameters(JsonNode node) throws MigrationException {
        merge(node,this::iteratorSources,PATH_SRC,DIST_FIELD_SRC);
        merge(node,this::iteratorTargets,PATH_TRG,DIST_FIELD_TRG);
        return node;
    }

    private void merge(JsonNode node, CheckedFunction<JsonNode,Iterator<JsonNode>> iterator, String nodePath, String nodeName) throws MigrationException{
        if (!path(node, nodePath).isMissingNode()) {
            Iterator<JsonNode> iter;
            iter = iterator.apply(node);
            while (iter.hasNext()) {
                var nd = (ObjectNode) iter.next();
                var distrNode = nd.get(nodeName);
                var paramNode = nd.get(PARAM_FIELD).deepCopy();
                String distrName = distrNode.asText();
                remove(nd,PARAM_FIELD);
                remove(nd,nodeName);
                var newNode = getMapper().createObjectNode();
                newNode.put(TYPE_STRING,distrName);
                newNode.set(PARAM_STRING,paramNode);
                nd.set(nodeName,newNode);
            }
        }
    }
}
