package org.vadere.state.attributes.models.psychology;

import java.util.*;

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


    public int[] getTargetIds(String information){
        ArrayList<Integer> al = (ArrayList<Integer>) routeChoices.get(information).get("targetIds");
        int[] arr = al.stream().mapToInt(i -> i).toArray();
        return arr;
    }

    public double[] getTargetProbabilities(String information){
        ArrayList<Double> al = (ArrayList<Double>) routeChoices.get(information).get("targetProbabilities");
        double[] arr = al.stream().mapToDouble(i -> i).toArray();
        return arr;
    }



}
