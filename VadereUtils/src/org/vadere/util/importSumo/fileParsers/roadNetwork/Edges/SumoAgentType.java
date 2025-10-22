package org.vadere.util.importSumo.fileParsers.roadNetwork.Edges;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// also used in blacklists - thus every possible element needs to be defined
public enum SumoAgentType {
    Pedestrian("pedestrian"),
    Passenger("passenger"), // default vehicle class
    Bus("bus"),
    Tram("tram"),
    RailUrban("rail_urban"),
    Rail("rail"),
    RailElectric("rail_electric"),
    RailFast("rail_fast"),
    Ship("ship"),
    Container("container"),
    CableCar("cable_car"),
    Subway("subway"),
    Aircraft("aircraft"),
    Wheelchair("wheelchair"),
    Scooter("scooter"),
    Drone("drone"),
    Bicycle("bicycle"),;

    private final String sumoMapping;

    private static final Map<String, SumoAgentType> lookup = new HashMap<>();

    static {
        for (SumoAgentType s : SumoAgentType.values()) {
            lookup.put(s.sumoMapping, s);
        }
    }

    SumoAgentType(String sumoMapping) {
        this.sumoMapping = sumoMapping;
    }

    public static Set<SumoAgentType> subtractFromAll(Set<SumoAgentType> disallowedAgents) {
        HashSet<SumoAgentType> result = new HashSet<>();
        for (SumoAgentType value : SumoAgentType.values()) {
            if(disallowedAgents.contains(value)){
                continue;
            }
            result.add(value);
        }
        return result;
    }

    @Nullable
    public static SumoAgentType get(String label) {
        return lookup.get(label);
    }
}
