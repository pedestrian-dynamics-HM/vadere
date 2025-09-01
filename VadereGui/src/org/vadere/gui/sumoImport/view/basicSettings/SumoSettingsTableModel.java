package org.vadere.gui.sumoImport.view.basicSettings;

import org.jetbrains.annotations.NotNull;
import org.vadere.gui.sumoImport.model.data.SumoImportObjectFlag;
import org.vadere.gui.sumoImport.view.SumoImportDialogLoca;
import org.vadere.util.importSumo.settings.SumoInvertGroup;
import org.vadere.gui.sumoImport.model.data.SumoObjectImportSetting;
import org.vadere.state.types.ScenarioElementType;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SumoSettingsTableModel extends AbstractTableModel {
    private final SumoImportDialogLoca loca = new SumoImportDialogLoca();
    private final List<SumoObjectImportSetting> settings = new ArrayList<>();
    private final SumoImportObjectFlag[] flags = SumoImportObjectFlag.values();
    private final int nameLabelColumnIndex = 0;
    private final int targetInvertGroupColumnIndex = flags.length + 1;
    private final int scenarioElementTypeColumnIndex = flags.length + 2;

    public void setCellEditorsOn(JTable jTable) {
        TableColumn nameLabelColumn = jTable.getColumnModel().getColumn(nameLabelColumnIndex);
        nameLabelColumn.setMinWidth(250);

        TableColumn targetInvertGroupColumn = jTable.getColumnModel().getColumn(targetInvertGroupColumnIndex);
        addInvertGroupSelectionCellRenderers(targetInvertGroupColumn);
        targetInvertGroupColumn.setMinWidth(150);

        TableColumn elementTypeSelectionColumn = jTable.getColumnModel().getColumn(scenarioElementTypeColumnIndex);
        elementTypeSelectionColumn.setCellEditor(new PopupScenarioElementTypesCellEditor(loca.popupSelectScenarioElementTitle));
        elementTypeSelectionColumn.setMinWidth(150);
    }

    private void addInvertGroupSelectionCellRenderers(TableColumn targetInvertGroupColumn) {
        JComboBox<SumoInvertGroup> invertGroupSelectionDropdown = createInvertGroupSelectionDropdown();
        targetInvertGroupColumn.setCellEditor(new DefaultCellEditor(invertGroupSelectionDropdown));
        targetInvertGroupColumn.setCellRenderer(new DefaultTableCellRenderer(){
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if(value instanceof SumoInvertGroup invertGroup){
                    setText(loca.translate(invertGroup));
                }

                return component;
            }
        });
    }

    @NotNull
    private JComboBox<SumoInvertGroup> createInvertGroupSelectionDropdown() {
        JComboBox<SumoInvertGroup> comboBox = new JComboBox<>();
        comboBox.addItem(null);
        for (SumoInvertGroup e : SumoInvertGroup.values()) {
            comboBox.addItem(e);
        }
        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {

                if (value == null) {
                    value = " ";
                }else{
                    value = loca.translate(SumoInvertGroup.valueOf(value.toString()));
                }

                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
        return comboBox;
    }

    @Override
    public int getRowCount() {
        return settings.size();
    }

    @Override
    public int getColumnCount() {
        return 3 + flags.length; // type field + scenario element field + flags
    }

    @Override
    public String getColumnName(int column) {
        if (column == 0) return "";
        if (column == targetInvertGroupColumnIndex) return loca.invertGroup;
        if (column == scenarioElementTypeColumnIndex) return loca.scenarioElements;
        return loca.translate(flags[column - 1]);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 0 || columnIndex == scenarioElementTypeColumnIndex) return List.class;
        if (columnIndex == targetInvertGroupColumnIndex) return SumoInvertGroup.class;
        return Boolean.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex > 0;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        SumoObjectImportSetting setting = settings.get(rowIndex);
        if (columnIndex == 0) return setting.getKeyLocalized(loca);
        if (columnIndex == scenarioElementTypeColumnIndex) return setting.getScenarioElementTypes();
        if (columnIndex == targetInvertGroupColumnIndex) return setting.getTargetInvertGroup();
        return setting.getSumoObjectFlags().contains(flags[columnIndex - 1]);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex == 0) {
            return;
        }

        SumoObjectImportSetting setting = settings.get(rowIndex);
        if (columnIndex == scenarioElementTypeColumnIndex) {
            setting.setScenarioElementTypes((List<ScenarioElementType>) aValue);
            return;
        }

        if(columnIndex == targetInvertGroupColumnIndex){
            setting.setTargetInvertGroup((SumoInvertGroup) aValue);
            return;
        }

        SumoImportObjectFlag flag = flags[columnIndex - 1];
        boolean selected = (Boolean) aValue;

        if (selected) {
            setting.getSumoObjectFlags().add(flag);
        } else {
            setting.getSumoObjectFlags().remove(flag);
        }
        fireTableCellUpdated(rowIndex, columnIndex);
    }

    public void addSetting(SumoObjectImportSetting setting) {
        settings.add(setting);
        int newRow = settings.size() - 1;
        fireTableRowsInserted(newRow, newRow);
    }

    public void removeSetting(int index) {
        settings.remove(index);
        fireTableRowsDeleted(index, index);
    }

    public List<SumoObjectImportSetting> getImportSettings() {
        return settings;
    }
}
