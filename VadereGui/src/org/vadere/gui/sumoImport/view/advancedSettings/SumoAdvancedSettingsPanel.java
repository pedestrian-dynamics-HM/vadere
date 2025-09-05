package org.vadere.gui.sumoImport.view.advancedSettings;

import org.vadere.util.importSumo.processors.increaseJunctionSize.IncreaseJunctionSizeSettings;
import org.vadere.util.importSumo.processors.inverseSpace.SumoInvertSettings;
import org.vadere.util.importSumo.settings.SumoAdvancedImportSettings;
import org.vadere.util.importSumo.processors.fillGaps.FillGapsSumoProcessorSettings;
import org.vadere.util.importSumo.settings.SumoInvertGroup;

import javax.swing.*;
import java.awt.*;
import java.util.Hashtable;
import java.util.Map;

import static org.vadere.gui.sumoImport.view.advancedSettings.SumoAdvanceSettingsSwingUiUtils.*;

public class SumoAdvancedSettingsPanel extends JPanel {
    private final JCheckBox enableLaneToJunctionSnapping = new JCheckBox();
    private JTextField laneToJunctionSnapPointsDistance;

    private final JCheckBox enableCrosswalkToEdgeSnapping = new JCheckBox();
    private JTextField crosswalkToEdgeSnappingDistance;
    private JTextField crosswalkToEdgeSnappingMaxAngle;

    private final JCheckBox enableIncreaseJunctionSize = new JCheckBox();
    private JTextField increaseJunctionSize;

    private SumoAdvancedSettingsLoca loca = new SumoAdvancedSettingsLoca();

    private final SumoAdvancedSettingsInvertGroupPanel removeCrossingsFromJunctionsSettings
            = new SumoAdvancedSettingsInvertGroupPanel(loca.categoryJunctionsWithoutCrossings,1, null, 0.1, null);

    private final Map<SumoInvertGroup, SumoAdvancedSettingsInvertGroupPanel> invertGroups = new Hashtable<>();

    public SumoAdvancedImportSettings getAdvanceImportSettings() {

        FillGapsSumoProcessorSettings fillGapsSettings = new FillGapsSumoProcessorSettings(
                enableLaneToJunctionSnapping.isSelected(),
                ParseDouble(laneToJunctionSnapPointsDistance),
                enableCrosswalkToEdgeSnapping.isSelected(),
                ParseDouble(crosswalkToEdgeSnappingDistance),
                ParseDouble(crosswalkToEdgeSnappingMaxAngle)
                );

        SumoInvertSettings crossingsFromJunctionsSettingsSettings = removeCrossingsFromJunctionsSettings.getSettings();

        Map<SumoInvertGroup, SumoInvertSettings> invertGroupSettings = new Hashtable<>();
        for (Map.Entry<SumoInvertGroup, SumoAdvancedSettingsInvertGroupPanel> invertGroupEntry : invertGroups.entrySet()) {
            invertGroupSettings.put(invertGroupEntry.getKey(), invertGroupEntry.getValue().getSettings());
        }

        IncreaseJunctionSizeSettings increaseJunctionSizeSettings = new IncreaseJunctionSizeSettings(0.005);

        return new SumoAdvancedImportSettings(fillGapsSettings, crossingsFromJunctionsSettingsSettings, increaseJunctionSizeSettings, invertGroupSettings);
    }

    public SumoAdvancedSettingsPanel() {
        FlowLayout flowLayout = new FlowLayout();
        flowLayout.setAlignment(FlowLayout.LEFT);
        setLayout(flowLayout);

        addSettingViews();
    }

    private void addSettingViews() {
        add(createLaneToJunctionSnappingSettingView());
        add(createCrosswalkToWalkwaySnappingSettingView());
        add(createIncreaseJunctionSizeSettingView());
        add(removeCrossingsFromJunctionsSettings);
        for (SumoInvertGroup invertGroup : SumoInvertGroup.values()) {
            SumoAdvancedSettingsInvertGroupPanel invertGroupPanel = new SumoAdvancedSettingsInvertGroupPanel(loca.translate(invertGroup));
            invertGroups.put(invertGroup, invertGroupPanel);
            add(invertGroupPanel);
        }
    }

    private JPanel createLaneToJunctionSnappingSettingView() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = createSettingsGridBagConstraints();

        enableLaneToJunctionSnapping.setSelected(true);
        panel.add(createLabeledComponent(loca.enabled+":", enableLaneToJunctionSnapping), gbc);
        gbc.gridy++;

        laneToJunctionSnapPointsDistance = createDoubleTextField();
        laneToJunctionSnapPointsDistance.setText("1.5");
        panel.add(createLabeledComponentWithTooltip(loca.maxSnapDistance, laneToJunctionSnapPointsDistance,"m",
                loca.lanetoJunctionMaxSnapDistanceTooltip), gbc);
        gbc.gridy++;

        return addBorder(panel, loca.categoryLaneToJunctionFix);
    }

    private JPanel createCrosswalkToWalkwaySnappingSettingView() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = createSettingsGridBagConstraints();

        enableCrosswalkToEdgeSnapping.setSelected(true);
        panel.add(createLabeledComponent(loca.enabled+":", enableCrosswalkToEdgeSnapping), gbc);
        gbc.gridy++;

        crosswalkToEdgeSnappingDistance = createDoubleTextField();
        crosswalkToEdgeSnappingDistance.setText("4");
        panel.add(createLabeledComponentWithTooltip(loca.maxSnapDistance, crosswalkToEdgeSnappingDistance, "m",
                loca.crosswalkWalkwayMaxSnapDistanceTooltip), gbc);
        gbc.gridy++;

        crosswalkToEdgeSnappingMaxAngle = createDoubleTextField();
        crosswalkToEdgeSnappingMaxAngle.setText("65");
        panel.add(createLabeledComponentWithTooltip(loca.maxAngle, crosswalkToEdgeSnappingMaxAngle, "°",
               loca.maxCrosswalkSnapAngle), gbc);
        gbc.gridy++;

        return addBorder(panel, loca.categoryCrosswalkToWalkwaySnappingFix);
    }

    private JPanel createIncreaseJunctionSizeSettingView() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = createSettingsGridBagConstraints();

        enableIncreaseJunctionSize.setSelected(true);
        panel.add(createLabeledComponent(loca.enabled+":", enableIncreaseJunctionSize), gbc);
        gbc.gridy++;

        increaseJunctionSize = createDoubleTextField();
        increaseJunctionSize.setText("0.005");
        panel.add(createLabeledComponentWithTooltip(loca.increaseJunctionSizes, increaseJunctionSize,"m",
                loca.increaseJunctionSizesTooltip), gbc);
        gbc.gridy++;

        return addBorder(panel, loca.categoryJunctionFixes);
    }
}
