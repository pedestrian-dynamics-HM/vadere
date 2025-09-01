package org.vadere.gui.sumoImport.view.advancedSettings;

import org.jetbrains.annotations.Nullable;
import org.vadere.util.importSumo.processors.inverseSpace.SumoInvertSettings;

import javax.swing.*;

import java.awt.*;

import static org.vadere.gui.sumoImport.view.advancedSettings.SumoAdvanceSettingsSwingUiUtils.*;

public class SumoAdvancedSettingsInvertGroupPanel extends JPanel {
    private final JTextField cellSize;
    private final JCheckBox maxCellsToCombinePerAxisEnabled = new JCheckBox();;
    private final JTextField maxCellsToCombinePerAxis;

    private final JCheckBox minimumPolygonDiameterEnabled = new JCheckBox();
    private final JTextField minimumPolygonDiameter;
    private final JCheckBox minimumPolygonSizeEnabled = new JCheckBox();
    private final JTextField minPolygonSize;
    private final SumoAdvancedSettingsLoca loca = new SumoAdvancedSettingsLoca();

    public SumoAdvancedSettingsInvertGroupPanel(String title){
        this(title, 4, 5, null, 0.5);
    }

    public SumoAdvancedSettingsInvertGroupPanel(String title, int defaultCellSize, @Nullable Integer defaultCellsToCombinePerAxis, @Nullable Double defaultMinimumDiameter, @Nullable Double defaultMinimumPolygonSize) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = createSettingsGridBagConstraints();

        cellSize = createDoubleTextField();
        cellSize.setText(defaultCellSize+"");
        panel.add(createLabeledComponentWithTooltip(
                loca.invertGroupCellSize, cellSize,
                loca.invertGroupCellSizeTooltip), gbc);
        gbc.gridy++;

        maxCellsToCombinePerAxisEnabled.setSelected(defaultCellsToCombinePerAxis != null);
        maxCellsToCombinePerAxis = createIntegerTextField();
        maxCellsToCombinePerAxis.setText(defaultCellsToCombinePerAxis != null ? defaultCellsToCombinePerAxis.toString() : "0");
        panel.add(createLabeledDisableableComponentWithTooltip(
                loca.invertGroupCellsToCombine, maxCellsToCombinePerAxis, maxCellsToCombinePerAxisEnabled,
                loca.invertGroupCellsToCombineTooltip), gbc);
        gbc.gridy++;

        minimumPolygonDiameterEnabled.setSelected(defaultMinimumDiameter != null);
        minimumPolygonDiameter = createDoubleTextField();
        minimumPolygonDiameter.setText(defaultMinimumDiameter != null? defaultMinimumDiameter.toString() : "0");
        panel.add(createLabeledDisableableComponentWithTooltip(
                loca.invertGroupMinPolygonDiameter, minimumPolygonDiameter, minimumPolygonDiameterEnabled,
                loca.invertGroupMinPolygonDiameterTooltip), gbc);
        gbc.gridy++;

        minimumPolygonSizeEnabled.setSelected(defaultMinimumPolygonSize != null);
        minPolygonSize = createDoubleTextField();
        minPolygonSize.setText(defaultMinimumPolygonSize != null? defaultMinimumPolygonSize.toString() : "0");
        panel.add(createLabeledDisableableComponentWithTooltip(
                loca.invertGroupMinPolygonSize, minPolygonSize, minimumPolygonSizeEnabled,
                loca.invertGroupMinPolygonSizeTooltip), gbc);
        gbc.gridy++;

        add(addBorder(panel, title));
    }

    public SumoInvertSettings getSettings() {
        return new SumoInvertSettings(
                    ParseDouble(cellSize),
                    maxCellsToCombinePerAxisEnabled.isSelected()?ParseInt(maxCellsToCombinePerAxis) : null,
                    minimumPolygonDiameterEnabled.isSelected()?ParseDouble(minimumPolygonDiameter) : null,
                    minimumPolygonSizeEnabled.isSelected()?ParseNullableDouble(minPolygonSize) : null
                );
    }
}
