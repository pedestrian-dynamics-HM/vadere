package org.vadere.util.importSumo.settings;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum SumoInvertGroup {
    InvertGroup1(1),
    InvertGroup2(2),
    InvertGroup3(3);

    private final int value;
    private static final Map<String,SumoInvertGroup> ENUM_MAP;

    SumoInvertGroup(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    static {
        Map<String,SumoInvertGroup> map = new ConcurrentHashMap<String, SumoInvertGroup>();
        for (SumoInvertGroup instance : SumoInvertGroup.values()) {
            map.put(instance.toString().toLowerCase(),instance);
        }
        ENUM_MAP = Collections.unmodifiableMap(map);
    }

    @Nullable
    public static SumoInvertGroup getOrNull(String name) {
        return ENUM_MAP.get(name.toLowerCase());
    }
}
