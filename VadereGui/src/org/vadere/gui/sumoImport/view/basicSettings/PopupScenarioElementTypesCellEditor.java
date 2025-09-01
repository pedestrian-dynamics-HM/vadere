package org.vadere.gui.sumoImport.view.basicSettings;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.jetbrains.annotations.NotNull;
import org.vadere.gui.sumoImport.view.SumoImportDialogLoca;
import org.vadere.state.attributes.AttributesScenarioElement;
import org.vadere.state.attributes.scenario.AttributesMeasurementArea;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.util.logging.Logger;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PopupScenarioElementTypesCellEditor extends AbstractCellEditor implements TableCellEditor {
    private SumoImportDialogLoca loca = new SumoImportDialogLoca();
    private List<ScenarioElementType> values;
    private final DefaultListModel<ScenarioElementType> listModel = new DefaultListModel<>();

    private final JButton triggerButton = new JButton(loca.edit+"...");
    private final String editText;

    private static final Logger logger = Logger.getLogger(PopupScenarioElementTypesCellEditor.class);
    private SumoImportDialogLoca sumoImportDialogLoca = new SumoImportDialogLoca();

    public PopupScenarioElementTypesCellEditor(String editTextTitle) {
        triggerButton.addActionListener(e -> openEditor(triggerButton));

        SimpleModule module = new SimpleModule();
        module.addAbstractTypeMapping(AttributesScenarioElement.class, AttributesMeasurementArea.class);

        this.editText = editTextTitle;
    }

    private void openEditor(Component parent) {
        JList<ScenarioElementType> jList = new JList<>(listModel);
        addDeleteKeyListener(jList);

        JScrollPane scrollPane = new JScrollPane(jList);
        scrollPane.setPreferredSize(new Dimension(400, 200));

        JPanel controlPanel = createAddRemoveElementsPanel(jList);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(controlPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        int option = JOptionPane.showConfirmDialog(
                SwingUtilities.getWindowAncestor(parent),
                mainPanel,
                editText,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (option == JOptionPane.OK_OPTION) {
            stopCellEditing();
        } else {
            cancelCellEditing();
        }
    }

    private void addDeleteKeyListener(JList<ScenarioElementType> jList) {
        Action deleteAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                removeSelectedItems(jList);
            }
        };

        jList.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
        jList.getActionMap().put("delete", deleteAction);
    }

    @NotNull
    private JPanel createAddRemoveElementsPanel(JList<ScenarioElementType> jList) {
        JComboBox<ScenarioElementType> scenarioElementDropdown = createScenarioElementDropdown();

        JButton removeButton = createRemoveButton(jList);

        removeButton.setEnabled(false);

        JPanel controlPanel = new JPanel();
        controlPanel.add(new JLabel(sumoImportDialogLoca.add+":"));
        controlPanel.add(scenarioElementDropdown);
        controlPanel.add(removeButton);
        return controlPanel;
    }

    @NotNull
    private JComboBox<ScenarioElementType> createScenarioElementDropdown() {
        ScenarioElementType[] scenarioElements = Arrays.stream(ScenarioElementType.values())
                .filter(scenarioElementType -> scenarioElementType!=ScenarioElementType.OBSTACLE)
                .toArray(ScenarioElementType[]::new);
        JComboBox<ScenarioElementType> comboBox = new JComboBox<>(scenarioElements);
        comboBox.addPopupMenuListener(new  PopupMenuListener() {

            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
                ScenarioElementType selected = (ScenarioElementType) comboBox.getSelectedItem();
                if (selected != null) {
                    values.add(selected);
                    listModel.addElement(selected);
                }
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {
            }
        });
        return comboBox;
    }

    @NotNull
    private JButton createRemoveButton(JList<ScenarioElementType> jList) {
        JButton removeButton = new JButton("-");

        jList.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean rowSelected = jList.getSelectedIndex() != -1;
                removeButton.setEnabled(rowSelected);
            }
        });

        removeButton.addActionListener(e -> {
            removeSelectedItems(jList);
        });
        return removeButton;
    }

    private void removeSelectedItems(JList<ScenarioElementType> jList) {
        for (int selectedIndex : jList.getSelectedIndices()) {
            values.remove(selectedIndex);
            listModel.remove(selectedIndex);
        }
    }

    @Override
    public Object getCellEditorValue() {
        return values;
    }

    @Override
    public Component getTableCellEditorComponent(
            JTable table, Object value, boolean isSelected, int row, int column) {

        listModel.clear();
        if(value == null){
            this.values = new ArrayList<>();
            return triggerButton;
        }
        this.values = (List<ScenarioElementType>) value;

        listModel.addAll(values);

        return triggerButton;
    }
}