package org.vadere.gui.topographycreator.control.celleditor;

import org.vadere.gui.topographycreator.model.TopographyCreatorModel;
import org.vadere.util.Attributes;
import org.vadere.util.AttributesAttached;

import javax.swing.*;
import java.lang.reflect.Field;

public class AttributeSpinner extends AttributeEditor {
    private JSpinner spinner;
    private Integer oldValue;
    public AttributeSpinner(Attributes attached, Field field, TopographyCreatorModel model,JPanel contentPanel) {
        super(attached, field, model,null);
    }

    @Override
    protected void initialize() {
        this.spinner = new JSpinner();
        this.add(spinner);
        this.spinner.addChangeListener(e ->updateModel(spinner.getValue()));
    }

    public void modelChanged(Object value) {
        var integer = (Integer)value;
        if(integer!=oldValue) {
            oldValue = integer;
            this.spinner.setValue(integer);
        }

    }

}
