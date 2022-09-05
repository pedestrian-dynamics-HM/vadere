package org.vadere.gui.topographycreator.control.celleditor;

import org.vadere.gui.topographycreator.model.TopographyCreatorModel;
import org.vadere.util.Attributes;
import org.vadere.util.AttributesAttached;

import javax.swing.*;
import java.lang.reflect.Field;

public class AttributeDoubleSpinner extends AttributeEditor {
    private JSpinner spinner;
    private Double oldValue;
    public AttributeDoubleSpinner(Attributes attached, Field field, TopographyCreatorModel model,JPanel unused) {
        super(attached, field, model,null);
    }
    @Override
    protected void initialize() {
        this.spinner = new JSpinner();
        this.spinner.setModel(new SpinnerNumberModel(0.0, -1000.0, 1000.0, 0.1));
        this.add(spinner);
        this.spinner.addChangeListener(e -> updateModel(spinner.getValue()));
    }

    public void modelChanged(Object value) {
        var doubleVal = (Double) value;
        if(doubleVal!=oldValue) {
            oldValue = doubleVal;
            this.spinner.setValue(doubleVal);
        }
    }

}
