package org.vadere.gui.sumoImport.view;

import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.sumoImport.model.data.SumoImportObjectFlag;
import org.vadere.gui.sumoImport.model.data.SumoObjectType;
import org.vadere.util.importSumo.settings.SumoInvertGroup;

public class SumoImportDialogLoca {
    public final String add = Messages.getString("Add.text");
    public final String edit = Messages.getString("Edit.text");
    public final String invertGroup = Messages.getString("SumoImport.Enum.InvertGroup");
    public final String scenarioElements = Messages.getString("SumoImport.ScenarioElements.text");
    public final String popupSelectScenarioElementTitle = Messages.getString("SumoImport.Popup.SelectScenarioElement.title ");

    public String translate(SumoImportObjectFlag flag) {
        switch (flag) {
            case Obstacle:
                return Messages.getString("SumoImport.Enum.SumoImportObjectFlag.Obstacle");
            default:
                throw new IllegalStateException("Unknown importObjectFlag " + flag);
        }
    }

    public String translate(SumoObjectType sumoObjectType) {
        switch (sumoObjectType) {
            case Roads:
                return Messages.getString("SumoImport.Enum.SumoObjectType.Roads");
            case RoadJunctions:
                return Messages.getString("SumoImport.Enum.SumoObjectType.RoadJunctions");
            case RoadJunctionsWithoutCrossings:
                return Messages.getString("SumoImport.Enum.SumoObjectType.RoadJunctionsWithoutCrossings");
            case Structures:
                return  Messages.getString("SumoImport.Enum.SumoObjectType.Structures");
            case PedestrianWalkways:
                return  Messages.getString("SumoImport.Enum.SumoObjectType.PedestrianWalkways");
            case PedestrianWalkingAreas:
                return  Messages.getString("SumoImport.Enum.SumoObjectType.PedestrianWalkingAreas");
            case PedestrianRoadCrossings:
                return  Messages.getString("SumoImport.Enum.SumoObjectType.PedestrianRoadCrossings");
            default:
                throw new IllegalStateException("Unknown SumoObjectType " + sumoObjectType);
        }
    }

    public String translate(SumoInvertGroup sumoInvertGroup) {
        return invertGroup + " " + sumoInvertGroup.getValue();
    }
}
