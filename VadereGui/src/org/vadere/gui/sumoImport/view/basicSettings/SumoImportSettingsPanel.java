package org.vadere.gui.sumoImport.view.basicSettings;

import org.jetbrains.annotations.NotNull;
import org.vadere.gui.sumoImport.model.data.SumoImportObjectFlag;
import org.vadere.gui.sumoImport.model.data.SumoObjectImportSetting;
import org.vadere.gui.sumoImport.model.data.SumoObjectType;
import org.vadere.gui.sumoImport.view.SumoImportDialogLoca;
import org.vadere.util.importSumo.settings.SumoInvertGroup;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

public class SumoImportSettingsPanel extends JPanel {
    private SumoImportDialogLoca loca =  new SumoImportDialogLoca();
    private final SumoSettingsTableModel tableModel = new SumoSettingsTableModel();
    private final JTable table = new JTable(tableModel);
    private JButton removeSettingButton;

    public SumoImportSettingsPanel() {
        prepopulateDefaultSettings();
        createImportSettingsPanel();
        tableModel.setCellEditorsOn(table);
        addDeleteKeyListener(table);
    }

    private void prepopulateDefaultSettings() {
        tableModel.addSetting(new SumoObjectImportSetting(SumoObjectType.PedestrianWalkways).setTargetInvertGroup(SumoInvertGroup.InvertGroup1));
        tableModel.addSetting(new SumoObjectImportSetting(SumoObjectType.PedestrianRoadCrossings).setTargetInvertGroup(SumoInvertGroup.InvertGroup1));
        tableModel.addSetting(new SumoObjectImportSetting(SumoObjectType.PedestrianWalkingAreas).setTargetInvertGroup(SumoInvertGroup.InvertGroup1));
        tableModel.addSetting(new SumoObjectImportSetting(SumoInvertGroup.InvertGroup1).addFlag(SumoImportObjectFlag.Obstacle));
    }

    private void createImportSettingsPanel() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.gridx = 0;
        gbc.gridy = 0;

        JPanel buttonPanel = createAddRemoveButtonPanel();
        add(buttonPanel, gbc);
        gbc.gridy++;

        // add and stretch table
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, gbc);
    }

    @NotNull
    private JPanel createAddRemoveButtonPanel() {
        JPanel buttonPanel = new JPanel();

        JComboBox<String> addObjectTypeDropdown = createAddSettingDropdown();
        removeSettingButton = new JButton("-");

        buttonPanel.add(new JLabel(loca.add+":"));
        buttonPanel.add(addObjectTypeDropdown);
        buttonPanel.add(removeSettingButton);

        // disable remove button when selection is empty
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean rowSelected = table.getSelectedRow() != -1;
                removeSettingButton.setEnabled(rowSelected);
            }
        });

        removeSettingButton.addActionListener(e -> removeSelectedSetting());
        return buttonPanel;
    }

    @NotNull
    private JComboBox<String> createAddSettingDropdown() {
        Vector<String> elements = new Vector<>();
        elements.add("");
        elements.addAll(Arrays.stream(SumoObjectType.values()).map(Enum::name).toList());
        elements.add("");
        elements.addAll(Arrays.stream(SumoInvertGroup.values()).map(Enum::name).toList());

        JComboBox<String> addObjectTypeDropdown = new JComboBox<>(elements);

        addObjectTypeDropdown.setRenderer(new DefaultListCellRenderer(){
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);

                if (!(value instanceof String valueString)) {
                    return label;
                }

                SumoObjectType objectType = SumoObjectType.getOrNull(valueString);
                if (objectType != null) {
                    label.setText(loca.translate(objectType));
                }

                SumoInvertGroup invertGroup = SumoInvertGroup.getOrNull(valueString);
                if (invertGroup != null) {
                    label.setText(loca.translate(invertGroup));
                }

                return label;
            }
        });

        addObjectTypeDropdown.addPopupMenuListener(new PopupMenuListener() {

            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
                String selected = (String) addObjectTypeDropdown.getSelectedItem();
                if(selected == null || selected.isEmpty()){
                    return;
                }

                SumoObjectType objectType = SumoObjectType.getOrNull(selected);
                if (objectType != null) {
                    SumoObjectImportSetting setting = new SumoObjectImportSetting(objectType);
                    tableModel.addSetting(setting);
                }

                SumoInvertGroup invertGroup = SumoInvertGroup.getOrNull(selected);
                if (invertGroup != null) {
                    SumoObjectImportSetting setting = new SumoObjectImportSetting(invertGroup);
                    tableModel.addSetting(setting);
                }
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {
            }
        });
        return addObjectTypeDropdown;
    }

    private void addDeleteKeyListener(JTable jTable) {
        Action deleteAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                removeSelectedSetting();
            }
        };

        jTable.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
        jTable.getActionMap().put("delete", deleteAction);
    }

    private void removeSelectedSetting() {
        int[] selectedIndices = table.getSelectionModel().getSelectedIndices();

        for (int i = selectedIndices.length - 1; i >= 0; i--) {
            int selectedIndex = selectedIndices[i];
            tableModel.removeSetting(selectedIndex);
        }
    }

    public void onPacked(){
        removeSettingButton.setEnabled(false);
    }

    public List<SumoObjectImportSetting> getImportSettings(){
        return tableModel.getImportSettings();
    }
}
