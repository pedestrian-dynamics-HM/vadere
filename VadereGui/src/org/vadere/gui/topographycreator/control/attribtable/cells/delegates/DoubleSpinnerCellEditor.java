package org.vadere.gui.topographycreator.control.attribtable.cells.delegates;

import org.vadere.gui.topographycreator.control.attribtable.model.AbstractModel;

import javax.swing.*;
import javax.swing.text.NumberFormatter;

public class DoubleSpinnerCellEditor extends AttributeEditor {
    private JSpinner spinner;

    public DoubleSpinnerCellEditor(AbstractModel parent, String id, JPanel contentPanel) {
        super(parent, id, contentPanel);
    }


    @Override
    protected void initialize() {
        this.spinner = new JSpinner();

        this.spinner.setModel(new SpinnerNumberModel(0.0, -1000.0, 1000.0, 0.01));
        this.spinner.setEditor(new JSpinner.NumberEditor(spinner, "0.00"));
        JFormattedTextField txt = ((JSpinner.NumberEditor) spinner.getEditor()).getTextField();
        ((NumberFormatter) txt.getFormatter()).setAllowsInvalid(false);
        this.add(spinner);
        this.spinner.addChangeListener(e -> updateModel(spinner.getValue()));
    }

    public void modelChanged(Object value) {
        this.spinner.setValue(value);
    }

}