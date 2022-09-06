package org.vadere.gui.topographycreator.control.celleditor;

import org.vadere.gui.topographycreator.model.TopographyCreatorModel;
import org.vadere.util.Attributes;
import org.vadere.util.AttributesAttached;

import javax.swing.*;
import java.lang.reflect.Field;

public class AttributeComboBox extends AttributeEditor {
    private JComboBox comboBox;
    private Object oldValue;
    public AttributeComboBox(Field attached,
                             Field field,
                             TopographyCreatorModel model,
                             JPanel unused) {
        super(attached, field, model,null);
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
