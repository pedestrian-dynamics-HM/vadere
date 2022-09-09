package org.vadere.gui.topographycreator.control.attribtable.cells.delegates;

import org.vadere.gui.topographycreator.control.attribtable.model.AbstractModel;

import javax.swing.*;

public class ComboBoxCellEditor extends AttributeEditor {
    private JComboBox comboBox;

    public ComboBoxCellEditor(AbstractModel parent, String id, JPanel contentPanel) {
        super(parent, id, contentPanel);
    }


    @Override
    protected void initialize() {
        this.comboBox = new JComboBox();
        this.add(comboBox);
        this.comboBox.addItemListener(e -> updateModel(comboBox.getSelectedItem()));
    }

    public void modelChanged(Object value) {
        this.comboBox.setSelectedItem(value);
    }
}
