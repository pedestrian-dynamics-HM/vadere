package org.vadere.gui.topographycreator.control.celleditor;

import org.vadere.gui.topographycreator.model.TopographyCreatorModel;
import org.vadere.util.Attributes;
import org.vadere.util.AttributesAttached;

import javax.swing.*;
import java.lang.reflect.Field;

public class AttributeComboBox extends AttributeEditor {
    private JComboBox comboBox;

    public AttributeComboBox(Attributes attached, Field field, TopographyCreatorModel model) {
        super(attached, field, model);
        this.comboBox = new JComboBox();
        this.add(comboBox);
        this.comboBox.addItemListener(e ->updateModelFromValue(comboBox.getSelectedItem()));
        //this.setBorder(new FlatTextBorder());
    }

    public void updateValueFromModel(Object value) {
        this.comboBox.setSelectedItem(value);
    }
}
