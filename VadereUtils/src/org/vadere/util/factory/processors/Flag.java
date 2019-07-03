package org.vadere.util.factory.processors;

import java.util.ArrayList;
import java.util.Objects;

public class Flag {


    public static final String needMeasurementArea = "needMeasurementArea";

    private final String description;

    public static ArrayList<Flag> getFlags(String... flags){
        ArrayList<Flag> ret = new ArrayList<>();
        for (String flag : flags) {
            ret.add(new Flag(flag));
        }
        return ret;
    }

    public Flag(String description){
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Flag that = (Flag) o;
        return description.equals(that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(description);
    }

}
