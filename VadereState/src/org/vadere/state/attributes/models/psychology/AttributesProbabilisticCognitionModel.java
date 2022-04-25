package org.vadere.state.attributes.models.psychology;

import org.jetbrains.annotations.NotNull;
import org.vadere.state.psychology.perception.types.ChangeTarget;
import org.vadere.state.psychology.perception.types.Stimulus;
import org.vadere.state.psychology.perception.types.Wait;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class AttributesProbabilisticCognitionModel extends AttributesCognitionModel {
    Map<Integer, Map> routeChoices;

    public AttributesProbabilisticCognitionModel(){
        this.routeChoices = getReactionBehavior();
    }

    public Map<Integer, Map> getReactionBehavior() {
        Map<Integer, Map> map = new HashMap();

        LinkedList<Integer> targetIds = new LinkedList<>();
        targetIds.add(1);
        targetIds.add(2);

        LinkedList<Double> targetProbabilities = new LinkedList<>();
        targetProbabilities.add(0.4);
        targetProbabilities.add(0.6);

        String instruction = "use target [1]";
        Map<String, Object> reactionBehavior = getRouteChoice(instruction, targetIds, targetProbabilities);

        map.put(1, reactionBehavior);
        return map;
    }

    private Map<String, Object> getRouteChoice( String instruction, LinkedList<Integer> targetIds, LinkedList<Double> targetProbabilities) {
        Map<String, Object> reactionBehavior = new HashMap();
        reactionBehavior.put("instruction", instruction );
        reactionBehavior.put("targetIds", targetIds);
        reactionBehavior.put("targetProbabilities", targetProbabilities);
        return reactionBehavior;
    }


}
