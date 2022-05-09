package org.vadere.state.attributes.models.psychology.perception;

import org.vadere.state.psychology.perception.types.*;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class AttributesSimplePerceptionModel extends AttributesPerceptionModel {
    Map<Integer, String> priority;

    public AttributesSimplePerceptionModel() {
        this.priority = getDefaultRanking();
    }

    public Map<Integer, String> getDefaultRanking() {
        Map<Integer, String> map = new HashMap();

        map.put(1, InformationStimulus.class.getSimpleName());
        map.put(2, ChangeTargetScripted.class.getSimpleName());
        map.put(3, ChangeTarget.class.getSimpleName());
        map.put(4, Threat.class.getSimpleName());
        map.put(5, Wait.class.getSimpleName());
        map.put(6, WaitInArea.class.getSimpleName());
        map.put(7, DistanceRecommendation.class.getSimpleName());

        return map;
    }

    public TreeMap<Integer, String> getSortedPriorityQueue(){
        return new TreeMap<>(this.priority);
    }
}
