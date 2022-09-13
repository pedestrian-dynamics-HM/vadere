package org.vadere.gui.topographycreator.control.attribtable.cells.delegates;

import org.vadere.gui.topographycreator.control.attribtable.tree.AttributeTree;

import javax.swing.*;
/**
 * @author Ludwig Jaeck
 * This class is a table delegate for editing boolean fields.
 */
public class CheckBoxCellEditor extends AttributeEditor {
    private JCheckBox checkBox;

    public CheckBoxCellEditor(AttributeTree.TreeNode model, JPanel contentPanel) {
        super(model, contentPanel);
    }


    @Override
    protected void initialize() {
        this.checkBox = new JCheckBox();
        initializeCheckBoxListener();
        this.add(checkBox);
    }

    private void initializeCheckBoxListener() {
        this.checkBox.addItemListener(e -> updateModel(checkBox.isSelected()));
    }

    @Override
    public void onModelChanged(Object value) {
        this.checkBox.setSelected((Boolean) value);
    }
}
