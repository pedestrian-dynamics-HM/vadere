package org.vadere.util.importSumo.processors.increaseJunctionSize;

import javax.annotation.Nullable;

public class IncreaseJunctionSizeSettings {
    private final @Nullable Double increasedJunctionSize;

    public IncreaseJunctionSizeSettings(@Nullable Double increasedJunctionSize) {
        this.increasedJunctionSize = increasedJunctionSize;
    }

    @Nullable
    public Double getIncreasedJunctionSize() {
        return increasedJunctionSize;
    }
}
