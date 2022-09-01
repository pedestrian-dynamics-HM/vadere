package org.vadere.gui.topographycreator.control.celleditor;

import org.vadere.gui.topographycreator.model.TopographyCreatorModel;
import org.vadere.state.attributes.AttributesAttached;

import javax.swing.*;
import java.lang.reflect.Field;

public class AttributeSpinner extends AttributeEditor {
    private JSpinner spinner;

    public AttributeSpinner(AttributesAttached attached, Field field, TopographyCreatorModel model) {
        super(attached, field, model);
        this.spinner = new JSpinner();
        this.add(spinner);
        this.spinner.addChangeListener(e -> SwingUtilities.invokeLater(()->updateModelFromValue(spinner.getValue())));
        //this.setBorder(new FlatTextBorder());
    }

    public void updateValueFromModel(Object value) {
        super.updateValueFromModel(value);
        this.spinner.setValue(value);
    }

}
