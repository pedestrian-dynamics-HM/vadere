package org.vadere.simulator.projects.migration.jsontranformation.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.vadere.annotation.factories.migrationassistant.MigrationTransformation;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.jsontranformation.SimpleJsonTransformation;
import org.vadere.state.attributes.distributions.AttributesLinearInterpolationDistribution;
import org.vadere.state.attributes.spawner.AttributesMixedSpawner;
import org.vadere.state.attributes.spawner.AttributesRegularSpawner;
import org.vadere.state.attributes.spawner.AttributesTimeSeriesSpawner;
import org.vadere.util.version.Version;

import java.util.HashMap;
import java.util.Iterator;


@MigrationTransformation(targetVersionLabel = "2.4")
public class TargetVersionV2_4 extends SimpleJsonTransformation {

    @FunctionalInterface
    public interface CheckedFunction<T, R> {
        R apply(T t) throws MigrationException;
    }

    private final HashMap<String,String> map = new HashMap<>(){{
        put("constant","org.vadere.state.attributes.distributions.AttributesConstantDistribution");
        put("binomial","org.vadere.state.attributes.distributions.AttributesBinomialDistribution");
        put("empirical","org.vadere.state.attributes.distributions.AttributesEmpiricalDistribution");
        put("linearInterpolation","org.vadere.state.attributes.distributions.AttributesLinearInterpolationDistribution");
        put("mixed","org.vadere.state.attributes.distributions.AttributesMixedDistribution");
        put("negativeExponential","org.vadere.state.attributes.distributions.AttributesNegativeExponentialDistribution");
        put("normal","org.vadere.state.attributes.distributions.AttributesNormalDistribution");
        put("poisson","org.vadere.state.attributes.distributions.AttributesPoissonDistribution");
        put("singleSpawn","org.vadere.state.attributes.distributions.AttributesSingleSpawnDistribution");
        put("timeSeries","org.vadere.state.attributes.distributions.AttributesTimeSeriesDistribution");
    }};

    private final String PATH_SRC = "scenario/topography/sources";
    private final String PATH_TRG = "scenario/topography/targets";
    private final String DIST_FIELD_SRC = "interSpawnTimeDistribution";
    private final String DIST_FIELD_TRG = "waitingTimeDistribution";
    private final String DIST_FIELD_NEW = "distribution";

    private final String SPAWNER_FIELD = "spawner";
    private final String PARAM_FIELD ="distributionParameters";

    private final String TYPE_STRING = "type";
    private final String PARAM_STRING = "parameters";

    private final String WAITER = "waiter";

    private final String ABSORBER = "absorber";

    public TargetVersionV2_4() {
        super(Version.V2_5);
    }

    @Override
    protected void initDefaultHooks() {
        addPostHookLast(this::mergeDistributionNameWithParameters);
        addPostHookLast(this::createSpawnerNodeInSources);
        addPostHookLast(this::moveAndRenameSourceFields);
        addPostHookLast(this::createNewNodesInTargets);
        addPostHookLast(this::moveAndRenameTargetFields);
        //addPostHookLast(this::sort);
    }

    private JsonNode createNewNodesInTargets(JsonNode node) throws MigrationException{
        if (!path(node, PATH_TRG).isMissingNode()) {
            Iterator<JsonNode> iter = iteratorTargets(node);
            while (iter.hasNext()) {
                var nd = (ObjectNode) iter.next();
                nd.set(WAITER,getMapper().createObjectNode());
                nd.set(ABSORBER,getMapper().createObjectNode());
            }
        }
        return node;
    }

    private JsonNode moveAndRenameTargetFields(JsonNode node) throws MigrationException {
        if (!path(node, PATH_TRG).isMissingNode()) {
            Iterator<JsonNode> iter = iteratorTargets(node);
            while (iter.hasNext()) {
                var nd = (ObjectNode) iter.next();
                moveRenameField(nd,"deletionDistance","deletionDistance",ABSORBER);
                moveRenameField(nd,"enabled","absorbing",ABSORBER);
                renameField(nd,"parallelWaiters","parallelEvents");
                renameField(nd,"nextSpeed","leavingSpeed");

                moveRenameField(nd,DIST_FIELD_NEW,DIST_FIELD_TRG,WAITER);
                if(nd.get("waitingBehaviour").asText().equals("NO_WAITING")){
                    ((ObjectNode)nd.path(WAITER)).put("enabled",false);
                }else{
                    ((ObjectNode)nd.path(WAITER)).put("enabled",true);
                }
                remove(nd,"waitingBehaviour");
                remove(nd,"startingWithRedLight");
                remove(nd,"waitingTimeYellowPhase");
            }
        }
        return node;
    }

    private JsonNode moveAndRenameSourceFields(JsonNode node) throws MigrationException{
        if (!path(node, PATH_SRC).isMissingNode()) {
            Iterator<JsonNode> iter = iteratorSources(node);
            while (iter.hasNext()) {
                var nd = (ObjectNode) iter.next();
                moveRenameField(nd,"eventElementCount","spawnNumber",SPAWNER_FIELD);
                moveRenameField(nd,"constraintsTimeStart","startTime",SPAWNER_FIELD);
                moveRenameField(nd,"constraintsTimeEnd","endTime",SPAWNER_FIELD);
                moveRenameField(nd,"constraintsElementsMax","maxSpawnNumberTotal",SPAWNER_FIELD);
                moveRenameField(nd,"eventPositionRandom","spawnAtRandomPositions",SPAWNER_FIELD);
                moveRenameField(nd,"eventPositionGridCA","spawnAtGridPositionsCA",SPAWNER_FIELD);
                moveRenameField(nd,"eventPositionFreeSpace","useFreeSpaceOnly",SPAWNER_FIELD);
                moveRenameField(nd,"eventElement","attributesPedestrian",SPAWNER_FIELD);


                var ndTrg = nd.path(SPAWNER_FIELD).path("eventElement");
                /*if(ndTrg.asText() != "null") {
                    ((ObjectNode) ndTrg).put("type", nd.path("dynamicElementType"));
                }*/
                remove(nd,"dynamicElementType");
                moveRenameField(nd,DIST_FIELD_NEW,DIST_FIELD_SRC,SPAWNER_FIELD);


                var distTypeNode = nd.findPath(SPAWNER_FIELD).findPath("type");
                var spawnNode =(ObjectNode) nd.findPath(SPAWNER_FIELD);
                String typeName = distTypeNode.asText();
                if(typeName.equals(map.get("timeSeries")) ){
                    spawnNode.put(TYPE_STRING, AttributesTimeSeriesSpawner.class.getName());
                }else if( typeName.equals(map.get("mixed"))){
                    spawnNode.put(TYPE_STRING, AttributesMixedSpawner.class.getName());
                }else if (typeName.equals(map.get("linearInterpolation"))){
                    spawnNode.put(TYPE_STRING, AttributesLinearInterpolationDistribution.class.getName());
                }else{
                    spawnNode.put(TYPE_STRING, AttributesRegularSpawner.class.getName());
                }
            }
        }
        return node;
    }

    private JsonNode createSpawnerNodeInSources(JsonNode node) throws MigrationException {
        if (!path(node, PATH_SRC).isMissingNode()) {
            Iterator<JsonNode> iter = iteratorSources(node);
            while (iter.hasNext()) {
                var nd = (ObjectNode) iter.next();
                nd.set(SPAWNER_FIELD,getMapper().createObjectNode());
            }
        }
        return node;
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
                if(!distrName.equals("null")) {
                    ((ObjectNode)paramNode).put(TYPE_STRING, map.get(distrName));
                }
                nd.set(nodeName,paramNode);
            }
        }
    }

    private void moveRenameField(JsonNode node,String newName, String fieldName, String newParent)throws MigrationException{
        var ndSrc = node.path(fieldName);
        var ndTrg = node.path(newParent);
        if(ndTrg.isMissingNode())
            throw new MigrationException("target "+ndTrg.asText()+ "node does not exist");
        if(ndSrc.isMissingNode()){
            ((ObjectNode)ndTrg).set(newName,(JsonNode) null);
        }else{
            ((ObjectNode)ndTrg).set(newName,ndSrc.deepCopy());
            remove(node,fieldName);
        }
    }
}
