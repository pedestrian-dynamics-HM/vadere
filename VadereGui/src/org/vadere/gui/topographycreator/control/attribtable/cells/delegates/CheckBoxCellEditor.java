package org.vadere.gui.topographycreator.control.attribtable.cells.delegates;

import org.vadere.gui.topographycreator.control.attribtable.model.AbstractModel;

import javax.swing.*;

public class CheckBoxCellEditor extends AttributeEditor {
    private JCheckBox checkBox;

    public CheckBoxCellEditor(AbstractModel parent, String id, JPanel contentPanel) {
        super(parent, id, contentPanel);
    }


    @Override
    protected void initialize() {
        this.checkBox = new JCheckBox();
        this.add(checkBox);
        this.checkBox.addItemListener(e -> updateModel(checkBox.isSelected()));
    }

    @Override
    public void modelChanged(Object value) {
        this.checkBox.setSelected((Boolean) value);
    }
}
