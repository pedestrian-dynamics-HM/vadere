package org.vadere.gui.topographycreator.control.attribtable.cells.delegates;

import org.vadere.gui.topographycreator.control.attribtable.tree.AttributeTreeModel;

import javax.swing.*;

public class ComboBoxCellEditor extends AttributeEditor {
    private JComboBox comboBox;

    public ComboBoxCellEditor(AttributeTreeModel.TreeNode model, JPanel contentPanel, Object initialValue) {
        super(model, contentPanel,initialValue);
    }

    @Override
    protected void initialize(Object initialValue) {
        this.comboBox = new JComboBox();
        this.comboBox.setSelectedItem(initialValue);
        if(initialValue!=null)
            this.add(comboBox);
        this.comboBox.addItemListener(e -> updateModel(comboBox.getSelectedItem()));
    }

    public void onModelChanged(Object value) {
        this.comboBox.setSelectedItem(value);
    }
}
