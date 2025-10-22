package org.vadere.gui.sumoImport.control;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang3.mutable.MutableInt;
import org.vadere.gui.sumoImport.model.data.SumoObjectImportSetting;
import org.vadere.gui.sumoImport.model.data.SumoObjectType;
import org.vadere.gui.sumoImport.model.logic.SumoImportParsers;
import org.vadere.gui.sumoImport.model.logic.SumoToVadereConverter;
import org.vadere.util.importSumo.fileParsers.roadNetwork.SumoJunction;
import org.vadere.util.importSumo.processors.RemoveCrossingFromJunctionsSumoProcessor;
import org.vadere.util.importSumo.processors.fillGaps.FillGapsSumoProcessor;
import org.vadere.util.importSumo.processors.increaseJunctionSize.IncreaseJunctionSizesProcessor;
import org.vadere.util.importSumo.settings.SumoAdvancedImportSettings;

import javax.annotation.Nullable;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;

public class SumoImportDialogControl {
    private final SumoImportParsers parsers;
    private final MutableInt vadereIdProvider = new MutableInt();

    private final FillGapsSumoProcessor fillGapsSumoProcessor = new FillGapsSumoProcessor();
    private final IncreaseJunctionSizesProcessor increaseJunctionSizeProcessor = new IncreaseJunctionSizesProcessor();
    private final RemoveCrossingFromJunctionsSumoProcessor removeCrossingFromJunctionsSumoProcessor = new RemoveCrossingFromJunctionsSumoProcessor(vadereIdProvider);
    private final SumoImportApplyImportSettings applyImportSettings = new SumoImportApplyImportSettings(vadereIdProvider);
    private final SumoToVadereConverter toVadereConverter = new SumoToVadereConverter();

    private final Consumer<String> onImportCompletedCallback;

    public SumoImportDialogControl(File sumoDirectoryPath, Consumer<String> onImportCompletedCallback) {
        parsers = new SumoImportParsers(sumoDirectoryPath, vadereIdProvider);
        if(!parsers.isValidSumoDirectory(sumoDirectoryPath)){
            throw new RuntimeException("Invalid Sumo directory path");
        }

        this.onImportCompletedCallback = onImportCompletedCallback;
    }

    public void submit(List<SumoObjectImportSetting> importSettings, SumoAdvancedImportSettings advancedImportSettings) throws JsonProcessingException {
        parsers.clear();
        SumoImportParsers.TopologyResult sumoTopologyResult = parsers.ParseSumoFiles();

        fillGapsSumoProcessor.postProcess(sumoTopologyResult.allEdges(), sumoTopologyResult.allJunctions(), advancedImportSettings.getFillGapsSumoProcessorSettings());

        increaseJunctionSizeProcessor.process(sumoTopologyResult.allJunctions(), advancedImportSettings.getIncreaseJunctionSizeSettings());

        List<SumoJunction> junctionsWithoutCrossings = extractJunctionsWithoutCrossings(sumoTopologyResult, importSettings, advancedImportSettings);
        SumoImportApplyImportSettings.Result appliedSettingsResult = applyImportSettings.apply(importSettings, advancedImportSettings, sumoTopologyResult, junctionsWithoutCrossings);

        String json = toVadereConverter.ConvertToVadereJson(appliedSettingsResult.allObstacles(), appliedSettingsResult.scenarioElements());
        onImportCompletedCallback.accept(json);
    }

    @Nullable
    private List<SumoJunction> extractJunctionsWithoutCrossings(
            SumoImportParsers.TopologyResult parseSumoTopologyResult, List<SumoObjectImportSetting> importSettings, SumoAdvancedImportSettings advancedImportSettings) {

        if (importSettings.stream().noneMatch(sumoObjectImportSetting -> sumoObjectImportSetting.getSumoObjectType() == SumoObjectType.RoadJunctionsWithoutCrossings)) {
            return null;
        }

        List<SumoJunction> junctionsWithoutCrossings = removeCrossingFromJunctionsSumoProcessor.process(
                parseSumoTopologyResult.roadJunctions(),
                advancedImportSettings.getCrossingsFromJunctionsSettingsSettings());
        return junctionsWithoutCrossings;
    }
}
