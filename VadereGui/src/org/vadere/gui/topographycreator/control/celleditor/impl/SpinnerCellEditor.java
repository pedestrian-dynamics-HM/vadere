package org.vadere.gui.topographycreator.control.celleditor.impl;

import org.vadere.gui.topographycreator.control.JAttributeTable;
import org.vadere.gui.topographycreator.model.TopographyCreatorModel;

import javax.swing.*;
import java.lang.reflect.Field;

public class SpinnerCellEditor extends AttributeEditor {
    private JSpinner spinner;
    private Integer oldValue;
    public SpinnerCellEditor(
            JAttributeTable parent,
            Object fieldOwner,
            Field field,
            TopographyCreatorModel model,
            JPanel contentPanel
    ) {
        super(parent,fieldOwner, field, model,null);
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
