package org.vadere.gui.components.utils;

import org.vadere.state.types.ScenarioElementType;

public class LocalizedStrings {
    public static String getScenarioElement(ScenarioElementType scenarioElementType) {
        return switch (scenarioElementType) {
            case SOURCE -> Localization.getString(ResourceStrings.ScenarioElementType.SOURCE);
            case TARGET -> Localization.getString(ResourceStrings.ScenarioElementType.TARGET);
            case STAIRS -> Localization.getString(ResourceStrings.ScenarioElementType.STAIRS);
            case OBSTACLE -> Localization.getString(ResourceStrings.ScenarioElementType.OBSTACLE);
            case PEDESTRIAN -> Localization.getString(ResourceStrings.ScenarioElementType.PEDESTRIAN);
            case TARGET_CHANGER -> Localization.getString(ResourceStrings.ScenarioElementType.TARGET_CHANGER);
            case ABSORBING_AREA -> Localization.getString(ResourceStrings.ScenarioElementType.ABSORBING_AREA);
            case MEASUREMENT_AREA -> Localization.getString(ResourceStrings.ScenarioElementType.MEASUREMENT_AREA);
            case TELEPORTER ->  Localization.getString(ResourceStrings.ScenarioElementType.TELEPORTER);
            case AEROSOL_CLOUD ->  Localization.getString(ResourceStrings.ScenarioElementType.AEROSOL_CLOUD);
            case DROPLETS ->  Localization.getString(ResourceStrings.ScenarioElementType.DROPLETS);
        };
    }

    public static String getScenarioElementWithEnglish(ScenarioElementType scenarioElementType) {
        return switch (scenarioElementType) {
            case SOURCE -> Localization.getString(ResourceStrings.ScenarioElementType.SOURCE_WITH_ENGLISH);
            case TARGET -> Localization.getString(ResourceStrings.ScenarioElementType.TARGET_WITH_ENGLISH);
            case STAIRS -> Localization.getString(ResourceStrings.ScenarioElementType.STAIRS_WITH_ENGLISH);
            case OBSTACLE -> Localization.getString(ResourceStrings.ScenarioElementType.OBSTACLE_WITH_ENGLISH);
            case PEDESTRIAN -> Localization.getString(ResourceStrings.ScenarioElementType.PEDESTRIAN_WITH_ENGLISH);
            case TARGET_CHANGER -> Localization.getString(ResourceStrings.ScenarioElementType.TARGET_CHANGER_WITH_ENGLISH);
            case ABSORBING_AREA -> Localization.getString(ResourceStrings.ScenarioElementType.ABSORBING_AREA_WITH_ENGLISH);
            case MEASUREMENT_AREA -> Localization.getString(ResourceStrings.ScenarioElementType.MEASUREMENT_AREA_WITH_ENGLISH);
            case TELEPORTER ->  Localization.getString(ResourceStrings.ScenarioElementType.TELEPORTER_WITH_ENGLISH);
            case AEROSOL_CLOUD ->  Localization.getString(ResourceStrings.ScenarioElementType.AEROSOL_CLOUD_WITH_ENGLISH);
            case DROPLETS ->  Localization.getString(ResourceStrings.ScenarioElementType.DROPLETS_WITH_ENGLISH);
        };
    }


}
