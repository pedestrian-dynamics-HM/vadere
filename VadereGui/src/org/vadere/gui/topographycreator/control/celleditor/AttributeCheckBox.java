package org.vadere.gui.topographycreator.control.celleditor;

import org.vadere.gui.topographycreator.model.TopographyCreatorModel;
import org.vadere.util.Attributes;
import org.vadere.util.AttributesAttached;

import javax.swing.*;
import java.lang.reflect.Field;

public class AttributeCheckBox extends AttributeEditor {
    private JCheckBox checkBox;
    private boolean isInit = true;
    public AttributeCheckBox(Attributes attached, Field field, TopographyCreatorModel model) {
        super(attached, field, model);
        this.checkBox = new JCheckBox();
        this.add(checkBox);
        this.checkBox.addItemListener(e ->updateModelFromValue(checkBox.isSelected()));
    }

    @Override
    public void updateValueFromModel(Object value) {
        this.checkBox.setSelected((Boolean) value);
    }
}
