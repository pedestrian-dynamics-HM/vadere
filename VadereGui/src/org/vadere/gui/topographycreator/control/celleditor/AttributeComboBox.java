package org.vadere.gui.topographycreator.control.celleditor;

import org.vadere.gui.topographycreator.model.TopographyCreatorModel;

import javax.swing.*;
import java.lang.reflect.Field;

public class AttributeComboBox extends AttributeEditor {
    private JComboBox comboBox;

    public AttributeComboBox(Object attached, Field field, TopographyCreatorModel model) {
        super(attached, field, model);
        this.comboBox = new JComboBox();
        this.add(comboBox);
        this.comboBox.addItemListener(e ->updateModelFromValue(comboBox.getSelectedItem()));
        //this.setBorder(new FlatTextBorder());
    }

    public void updateValueFromModel(Object value) {
        super.updateValueFromModel(value);
        this.comboBox.setSelectedItem(value);
    }
}
