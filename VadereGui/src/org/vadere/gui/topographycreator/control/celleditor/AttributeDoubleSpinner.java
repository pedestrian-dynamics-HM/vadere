package org.vadere.gui.topographycreator.control.celleditor;

import org.vadere.gui.topographycreator.model.TopographyCreatorModel;

import javax.swing.*;
import java.lang.reflect.Field;

public class AttributeDoubleSpinner extends AttributeEditor {
    private JSpinner spinner;

    public AttributeDoubleSpinner(Object attached, Field field, TopographyCreatorModel model) {
        super(attached, field, model);
        this.spinner = new JSpinner();
        this.spinner.setModel(new SpinnerNumberModel(0.0, -1000.0, 1000.0, 0.1));
        this.add(spinner);
        this.spinner.addChangeListener(e -> updateModelFromValue(spinner.getValue()));
        //this.setBorder(new FlatTextBorder());
    }

    public void updateValueFromModel(Object value) {
        super.updateValueFromModel(value);
        this.spinner.setValue(value);
    }

}
