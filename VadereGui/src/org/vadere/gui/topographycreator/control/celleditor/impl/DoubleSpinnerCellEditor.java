package org.vadere.gui.topographycreator.control.celleditor.impl;

import org.vadere.gui.topographycreator.control.JAttributeTable;
import org.vadere.gui.topographycreator.model.TopographyCreatorModel;

import javax.swing.*;
import java.lang.reflect.Field;

public class DoubleSpinnerCellEditor extends AttributeEditor {
    private JSpinner spinner;
    private Double oldValue;
    public DoubleSpinnerCellEditor(
            JAttributeTable parent,
            Object fieldOwner,
            Field field,
            TopographyCreatorModel model,
            JPanel unused) {
        super(parent,fieldOwner, field, model,null);
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
