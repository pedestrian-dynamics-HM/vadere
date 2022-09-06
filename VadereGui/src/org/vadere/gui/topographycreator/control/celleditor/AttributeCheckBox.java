package org.vadere.gui.topographycreator.control.celleditor;

import org.vadere.gui.topographycreator.model.TopographyCreatorModel;
import org.vadere.util.Attributes;
import org.vadere.util.AttributesAttached;

import javax.swing.*;
import java.lang.reflect.Field;

public class AttributeCheckBox extends AttributeEditor {
    private JCheckBox checkBox;
    private boolean isInit = true;
    public AttributeCheckBox(Field attached,
                             Field field,
                             TopographyCreatorModel model,
                             JPanel unused) {
        super(attached, field, model,null);
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
