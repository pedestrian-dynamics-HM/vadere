package org.vadere.gui.topographycreator.control.celleditor.impl;

import org.vadere.gui.topographycreator.control.JAttributeTable;
import org.vadere.gui.topographycreator.model.TopographyCreatorModel;
import org.vadere.util.Attributes;

import javax.swing.*;
import java.lang.reflect.Field;

public class ComboBoxCellEditor extends AttributeEditor {
    private JComboBox comboBox;
    private Object oldValue;
    public ComboBoxCellEditor(
            JAttributeTable parent,
            Attributes fieldOwner,
            Field field,
            TopographyCreatorModel model,
            JPanel unused
    ) {
        super(parent,fieldOwner, field, model,null);
    }
    @Override
    protected void initialize() {
        this.comboBox = new JComboBox();
        this.add(comboBox);
        this.comboBox.addItemListener(e -> updateModel(comboBox.getSelectedItem()));
    }

    public void modelChanged(Object value) {
        if(value!=oldValue) {
            oldValue = value;
            this.comboBox.setSelectedItem(value);
        }
    }
}
