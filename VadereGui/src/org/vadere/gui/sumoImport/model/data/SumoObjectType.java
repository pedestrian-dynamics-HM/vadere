package org.vadere.gui.sumoImport.model.data;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum SumoObjectType {
    Roads,
    RoadJunctions,
    RoadJunctionsWithoutCrossings,
    Structures,
    PedestrianWalkways,
    PedestrianWalkingAreas,
    PedestrianRoadCrossings;

    private static final Map<String,SumoObjectType> ENUM_MAP;

    static {
        Map<String,SumoObjectType> map = new ConcurrentHashMap<String, SumoObjectType>();
        for (SumoObjectType instance : SumoObjectType.values()) {
            map.put(instance.toString().toLowerCase(),instance);
        }
        ENUM_MAP = Collections.unmodifiableMap(map);
    }

    @Nullable
    public static SumoObjectType getOrNull(String name) {
        return ENUM_MAP.get(name.toLowerCase());
    }
}
