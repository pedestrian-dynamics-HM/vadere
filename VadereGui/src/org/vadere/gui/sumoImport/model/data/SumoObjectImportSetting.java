package org.vadere.gui.sumoImport.model.data;

import org.vadere.gui.sumoImport.view.SumoImportDialogLoca;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.util.importSumo.settings.SumoInvertGroup;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class SumoObjectImportSetting {
    @Nullable
    private final SumoObjectType sumoObjectType;
    @Nullable
    private final SumoInvertGroup invertGroup;
    @Nullable
    private SumoInvertGroup targetInvertGroup;
    private final HashSet<SumoImportObjectFlag> sumoObjectFlags = new HashSet<>();
    private List<ScenarioElementType> scenarioElementTypes = new ArrayList<>();

    public SumoObjectImportSetting(SumoObjectType sumoObjectType) {
        this.sumoObjectType = sumoObjectType;
        this.invertGroup = null;
    }

    public SumoObjectImportSetting(SumoInvertGroup invertGroup) {
        this.invertGroup = invertGroup;
        this.sumoObjectType = null;
    }

    @Nullable
    public SumoObjectType getSumoObjectType() {
        return sumoObjectType;
    }

    @Nullable
    public SumoInvertGroup getInvertGroup() {
        return invertGroup;
    }

    public HashSet<SumoImportObjectFlag> getSumoObjectFlags() {
        return sumoObjectFlags;
    }

    public List<ScenarioElementType> getScenarioElementTypes() {
        return scenarioElementTypes;
    }

    public SumoObjectImportSetting setScenarioElementTypes(List<ScenarioElementType> scenarioElementTypes) {
        this.scenarioElementTypes = scenarioElementTypes;
        return this;
    }

    public String getKeyLocalized(SumoImportDialogLoca loca){
        if(sumoObjectType != null){
            return loca.translate(sumoObjectType);
        }
        assert invertGroup != null;
        return loca.translate(invertGroup);
    }

    @Nullable
    public SumoInvertGroup getTargetInvertGroup() {
        return targetInvertGroup;
    }

    public SumoObjectImportSetting setTargetInvertGroup(@Nullable SumoInvertGroup targetGroup) {
        this.targetInvertGroup = targetGroup;
        return this;
    }

    public SumoObjectImportSetting addFlag(SumoImportObjectFlag flag) {
        this.sumoObjectFlags.add(flag);
        return this;
    }
}
