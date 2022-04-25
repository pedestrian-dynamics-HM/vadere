package org.vadere.state.attributes.models.psychology;

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

        map.put(1,"ChangeTarget");
        map.put(2, "ChangeTargetScripted");
        map.put(3, "Thread");
        map.put(4, "Wait");
        map.put(5, "WaitInArea");
        map.put(6, "DistanceRecommendation");

        return map;
    }

    public TreeMap<Integer, String> getSortedPriorityQueue(){
        return new TreeMap<>(this.priority);
    }
}
