package org.vadere.gui.topographycreator.control.attribtable.cells.delegates;

import org.vadere.gui.topographycreator.control.attribtable.tree.AttributeTree;
import org.vadere.gui.topographycreator.control.attribtable.tree.FieldNode;

import javax.swing.*;
import javax.swing.text.NumberFormatter;

public class DoubleSpinnerCellEditor extends AttributeEditor {
    private JSpinner spinner;

    public DoubleSpinnerCellEditor(AttributeTree.TreeNode model, JPanel contentPanel) {
        super(model, contentPanel);
    }


    @Override
    protected void initialize() {
        this.spinner = new JSpinner();

        this.spinner.setModel(new SpinnerNumberModel(0.0, -1000.0, 1000.0, 0.01));
        this.spinner.setEditor(new JSpinner.NumberEditor(spinner, "0.00"));
        JFormattedTextField txt = ((JSpinner.NumberEditor) spinner.getEditor()).getTextField();
        ((NumberFormatter) txt.getFormatter()).setAllowsInvalid(false);
        var value = ((FieldNode) model).getValueNode().getValue();
        if (value != null)
            this.spinner.setValue(((FieldNode) model).getValueNode().getValue());
        this.add(spinner);
        this.spinner.addChangeListener(e -> updateModel(spinner.getValue()));
    }

    public void onModelChanged(Object value) {
        this.spinner.setValue(value);
    }

}
