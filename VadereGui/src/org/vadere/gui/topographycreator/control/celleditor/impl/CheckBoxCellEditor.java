package org.vadere.gui.topographycreator.control.celleditor.impl;

import org.vadere.gui.topographycreator.control.JAttributeTable;
import org.vadere.gui.topographycreator.model.TopographyCreatorModel;
import org.vadere.util.Attributes;

import javax.swing.*;
import java.lang.reflect.Field;

public class CheckBoxCellEditor extends AttributeEditor {
    private JCheckBox checkBox;
    private final boolean isInit = true;
    public CheckBoxCellEditor(
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
        this.checkBox = new JCheckBox();
        this.add(checkBox);
        this.checkBox.addItemListener(e -> updateModel(checkBox.isSelected()));
    }
    @Override
    public void modelChanged(Object value) {
        this.checkBox.setSelected((Boolean) value);
    }
}
