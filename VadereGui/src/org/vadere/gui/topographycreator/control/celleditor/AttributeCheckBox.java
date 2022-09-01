package org.vadere.gui.topographycreator.control.celleditor;

import org.vadere.gui.topographycreator.model.TopographyCreatorModel;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.lang.reflect.Field;

public class AttributeCheckBox extends AttributeEditor {
    private JCheckBox checkBox;
    private boolean isInit = true;
    public AttributeCheckBox(Object attached, Field field, TopographyCreatorModel model) {
        super(attached, field, model);
        this.checkBox = new JCheckBox();
        this.add(checkBox);
        this.checkBox.addItemListener(e -> updateModelFromValue(checkBox.isSelected()));
    }

    @Override
    public void updateValueFromModel(Object value) {
        super.updateValueFromModel(value);
        this.checkBox.setSelected((Boolean) value);
    }
}
