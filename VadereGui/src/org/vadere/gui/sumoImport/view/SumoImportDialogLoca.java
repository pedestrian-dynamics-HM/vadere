package org.vadere.gui.sumoImport.view;

import org.vadere.gui.components.utils.Localization;
import org.vadere.gui.components.utils.ResourceStrings;
import org.vadere.gui.sumoImport.model.data.SumoImportObjectFlag;
import org.vadere.gui.sumoImport.model.data.SumoObjectType;
import org.vadere.util.importSumo.settings.SumoInvertGroup;

import java.util.Objects;

public class SumoImportDialogLoca {
    public final String popupTitle = Localization.getString("SumoImport.Popup.title");
    public final String add = Localization.getString("Add.text");
    public final String edit = Localization.getString("Edit.text");
    public final String invertGroup = Localization.getString("SumoImport.Enum.InvertGroup");
    public final String scenarioElements = Localization.getString("SumoImport.ScenarioElements.text");
    public final String popupSelectScenarioElementTitle = Localization.getString("SumoImport.Popup.SelectScenarioElement.title");
    public final String tabImportSettings = Localization.getString("SumoImport.Tabs.ImportSettings.title");
    public final String tabAdvancedSettings = Localization.getString("SumoImport.Tabs.AdvancedSettings.title");
    public final String startImport = Localization.getString("SumoImport.Popup.StartImportButton.title");
    public final String importErrorMessageDialogTitle = Localization.getString("SumoImport.Popup.ImportErrorMessageDialog.title");
    public final String importErrorMessageDialog = Localization.getString("SumoImport.Popup.ImportErrorMessageDialog.text");

    public String translate(SumoImportObjectFlag flag) {
        if (Objects.requireNonNull(flag) == SumoImportObjectFlag.Obstacle) {
            return Localization.getString(ResourceStrings.ScenarioElementType.OBSTACLE_WITH_ENGLISH);
        }
        throw new IllegalStateException("Unknown importObjectFlag " + flag);
    }

    public String translate(SumoObjectType sumoObjectType) {
        return switch (sumoObjectType) {
            case Roads -> Localization.getString("SumoImport.Enum.SumoObjectType.Roads");
            case RoadJunctions -> Localization.getString("SumoImport.Enum.SumoObjectType.RoadJunctions");
            case RoadJunctionsWithoutCrossings ->
                    Localization.getString("SumoImport.Enum.SumoObjectType.RoadJunctionsWithoutCrossings");
            case Structures -> Localization.getString("SumoImport.Enum.SumoObjectType.Structures");
            case PedestrianWalkways -> Localization.getString("SumoImport.Enum.SumoObjectType.PedestrianWalkways");
            case PedestrianWalkingAreas ->
                    Localization.getString("SumoImport.Enum.SumoObjectType.PedestrianWalkingAreas");
            case PedestrianRoadCrossings ->
                    Localization.getString("SumoImport.Enum.SumoObjectType.PedestrianRoadCrossings");
        };
    }

    public String translate(SumoInvertGroup sumoInvertGroup) {
        return invertGroup + " " + sumoInvertGroup.getValue();
    }
}
