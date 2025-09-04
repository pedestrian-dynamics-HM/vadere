package org.vadere.gui.sumoImport.view;

import org.vadere.gui.components.utils.Localization;
import org.vadere.gui.sumoImport.model.data.SumoImportObjectFlag;
import org.vadere.gui.sumoImport.model.data.SumoObjectType;
import org.vadere.util.importSumo.settings.SumoInvertGroup;

public class SumoImportDialogLoca {
    public final String add = Localization.getString("Add.text");
    public final String edit = Localization.getString("Edit.text");
    public final String invertGroup = Localization.getString("SumoImport.Enum.InvertGroup");
    public final String scenarioElements = Localization.getString("SumoImport.ScenarioElements.text");
    public final String popupSelectScenarioElementTitle = Localization.getString("SumoImport.Popup.SelectScenarioElement.title ");

    public String translate(SumoImportObjectFlag flag) {
        switch (flag) {
            case Obstacle:
                return Localization.getString("SumoImport.Enum.SumoImportObjectFlag.Obstacle");
            default:
                throw new IllegalStateException("Unknown importObjectFlag " + flag);
        }
    }

    public String translate(SumoObjectType sumoObjectType) {
        switch (sumoObjectType) {
            case Roads:
                return Localization.getString("SumoImport.Enum.SumoObjectType.Roads");
            case RoadJunctions:
                return Localization.getString("SumoImport.Enum.SumoObjectType.RoadJunctions");
            case RoadJunctionsWithoutCrossings:
                return Localization.getString("SumoImport.Enum.SumoObjectType.RoadJunctionsWithoutCrossings");
            case Structures:
                return  Localization.getString("SumoImport.Enum.SumoObjectType.Structures");
            case PedestrianWalkways:
                return  Localization.getString("SumoImport.Enum.SumoObjectType.PedestrianWalkways");
            case PedestrianWalkingAreas:
                return  Localization.getString("SumoImport.Enum.SumoObjectType.PedestrianWalkingAreas");
            case PedestrianRoadCrossings:
                return  Localization.getString("SumoImport.Enum.SumoObjectType.PedestrianRoadCrossings");
            default:
                throw new IllegalStateException("Unknown SumoObjectType " + sumoObjectType);
        }
    }

    public String translate(SumoInvertGroup sumoInvertGroup) {
        return invertGroup + " " + sumoInvertGroup.getValue();
    }
}
