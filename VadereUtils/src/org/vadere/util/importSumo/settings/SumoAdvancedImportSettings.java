package org.vadere.util.importSumo.settings;

import org.vadere.util.importSumo.processors.increaseJunctionSize.IncreaseJunctionSizeSettings;
import org.vadere.util.importSumo.processors.inverseSpace.SumoInvertSettings;
import org.vadere.util.importSumo.processors.fillGaps.FillGapsSumoProcessorSettings;

import java.util.Map;

public class SumoAdvancedImportSettings {
    private final FillGapsSumoProcessorSettings fillGapsSumoProcessorSettings;
    private final SumoInvertSettings crossingsFromJunctionsSettingsSettings;
    private final IncreaseJunctionSizeSettings increaseJunctionSizeSettings;
    private final Map<SumoInvertGroup, SumoInvertSettings> invertGroupSettings;

    public SumoAdvancedImportSettings(
            FillGapsSumoProcessorSettings fillGapsSumoProcessorSettings,
            SumoInvertSettings crossingsFromJunctionsSettingsSettings,
            IncreaseJunctionSizeSettings increaseJunctionSizeSettings,
            Map<SumoInvertGroup, SumoInvertSettings> invertGroupSettings) {
        this.fillGapsSumoProcessorSettings = fillGapsSumoProcessorSettings;
        this.crossingsFromJunctionsSettingsSettings = crossingsFromJunctionsSettingsSettings;
        this.increaseJunctionSizeSettings = increaseJunctionSizeSettings;
        this.invertGroupSettings = invertGroupSettings;
    }

    public FillGapsSumoProcessorSettings getFillGapsSumoProcessorSettings() {
        return fillGapsSumoProcessorSettings;
    }

    public IncreaseJunctionSizeSettings getIncreaseJunctionSizeSettings() {
        return increaseJunctionSizeSettings;
    }

    public Map<SumoInvertGroup, SumoInvertSettings> getInvertGroupSettings() {
        return invertGroupSettings;
    }

    public SumoInvertSettings getCrossingsFromJunctionsSettingsSettings() {
        return crossingsFromJunctionsSettingsSettings;
    }
}
