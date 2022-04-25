package org.vadere.state.attributes.models.psychology;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class AttributesProbabilisticCognitionModel extends AttributesCognitionModel {
    Map<String, Map> routeChoices;

    public AttributesProbabilisticCognitionModel(){
        this.routeChoices = getReactionBehavior();
    }

    public Map<String, Map> getReactionBehavior() {
        Map<String, Map> map = new HashMap();

        LinkedList<Integer> targetIds = new LinkedList<>();
        targetIds.add(1);
        targetIds.add(2);

        LinkedList<Double> targetProbabilities = new LinkedList<>();
        targetProbabilities.add(0.4);
        targetProbabilities.add(0.6);

        String instruction = "use target [1]";
        Map<String, Object> reactionBehavior = getRouteChoice(instruction, targetIds, targetProbabilities);

        map.put(instruction, reactionBehavior);
        return map;
    }

    public Map<String, Object> getRouteChoice( String instruction, LinkedList<Integer> targetIds, LinkedList<Double> targetProbabilities) {
        Map<String, Object> reactionBehavior = new HashMap<>();
        //reactionBehavior.put("instruction", instruction );
        reactionBehavior.put("targetIds", targetIds);
        reactionBehavior.put("targetProbabilities", targetProbabilities);
        return reactionBehavior;
    }

    public LinkedList<Integer> getTargetIds(String information){
        LinkedList<Integer> targets = new LinkedList<>();
        targets.addAll((Collection<? extends Integer>) routeChoices.get(information).get("targetIds"));
        return targets;
    }

    public LinkedList<Double> getTargetProbabilities(String information){
        LinkedList<Double> probs = new LinkedList<>();
        probs.addAll((Collection<? extends Double>) routeChoices.get(information).get("targetProbabilities"));
        return probs;
    }



}
