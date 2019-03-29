package org.vadere.util.factory.processors;

import java.util.ArrayList;
import java.util.Objects;

public class ProcessorFlag{


    public static final String needMeasurementArea = "needMeasurementArea";

    private final String description;

    public static ArrayList<ProcessorFlag> getFlags(String... flags){
        ArrayList<ProcessorFlag> ret = new ArrayList<>();
        for (String flag : flags) {
            ret.add(new ProcessorFlag(flag));
        }
        return ret;
    }

    public ProcessorFlag(String description){
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcessorFlag that = (ProcessorFlag) o;
        return description.equals(that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(description);
    }

}
