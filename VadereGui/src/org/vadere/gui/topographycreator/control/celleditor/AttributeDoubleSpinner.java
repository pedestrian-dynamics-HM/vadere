package org.vadere.gui.topographycreator.control.celleditor;

import org.vadere.gui.topographycreator.model.TopographyCreatorModel;
import org.vadere.util.Attributes;
import org.vadere.util.AttributesAttached;

import javax.swing.*;
import java.lang.reflect.Field;

public class AttributeDoubleSpinner extends AttributeEditor {
    private JSpinner spinner;

    public AttributeDoubleSpinner(Attributes attached, Field field, TopographyCreatorModel model) {
        super(attached, field, model);
        this.spinner = new JSpinner();
        this.spinner.setModel(new SpinnerNumberModel(0.0, -1000.0, 1000.0, 0.1));
        this.add(spinner);
        this.spinner.addChangeListener(e ->SwingUtilities.invokeLater(()-> updateModelFromValue(spinner.getValue())));
        //this.setBorder(new FlatTextBorder());
    }

    public void updateValueFromModel(Object value) {
        this.spinner.setValue(value);
    }

}
