package org.vadere.simulator.projects.dataprocessing_mtp;

import org.jetbrains.annotations.NotNull;

public class TimestepDataKey extends DataKey<Integer> implements Comparable<TimestepDataKey> {
    public TimestepDataKey(int timeStep) {
        super(timeStep);
    }

    @Override
    public int compareTo(final TimestepDataKey o) {
        return this.getKey().compareTo(o.getKey());
    }

    public static String getHeader() {
        return "ts";
    }
}
