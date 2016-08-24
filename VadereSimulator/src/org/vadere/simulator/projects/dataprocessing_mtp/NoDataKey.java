package org.vadere.simulator.projects.dataprocessing_mtp;

public final class NoDataKey extends DataKey<Integer> implements Comparable<NoDataKey> {

    private static NoDataKey key;

    private NoDataKey() {
        super(0);
    }

    @Override
    public int compareTo(final NoDataKey o) {
        return (o instanceof NoDataKey) ? 0 : 1;
    }

    public static NoDataKey key() {
        if (NoDataKey.key == null)
            NoDataKey.key = new NoDataKey();

        return NoDataKey.key;
    }
}
