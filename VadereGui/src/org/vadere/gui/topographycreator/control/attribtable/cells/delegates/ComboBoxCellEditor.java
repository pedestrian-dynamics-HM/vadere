package org.vadere.gui.topographycreator.control.attribtable.cells.delegates;

import org.vadere.gui.topographycreator.control.attribtable.tree.AttributeTree;

import javax.swing.*;

public class ComboBoxCellEditor extends AttributeEditor {
    private JComboBox comboBox;

    public ComboBoxCellEditor(AttributeTree.TreeNode model, JPanel contentPanel) {
        super(model, contentPanel);
    }

    @Override
    protected void initialize() {
        this.comboBox = new JComboBox();
        this.add(comboBox);
        this.comboBox.addItemListener(e -> updateModel(comboBox.getSelectedItem()));
    }

    public void onModelChanged(Object value) {
        this.comboBox.setSelectedItem(value);
    }
}
