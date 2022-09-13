package org.vadere.gui.topographycreator.control.attribtable.cells.delegates;

import org.vadere.gui.topographycreator.control.attribtable.tree.AttributeTree;
import org.vadere.gui.topographycreator.control.attribtable.tree.FieldNode;

import javax.swing.*;
import javax.swing.text.NumberFormatter;
/**
 * @author Ludwig Jaeck
 * This class is a table delegate for editing Double fields.
 */
public class DoubleSpinnerCellEditor extends AttributeEditor {
    private JSpinner spinner;

    public DoubleSpinnerCellEditor(AttributeTree.TreeNode model, JPanel contentPanel) {
        super(model, contentPanel);
    }

    @Override
    protected void initialize() {
        this.spinner = new JSpinner();
        initializeSpinnerModel();
        initializeSpinnerValue();
        initializeSpinnerListener();
        this.add(spinner);
    }

    private void initializeSpinnerModel() {
        this.spinner.setModel(
                new SpinnerNumberModel(
                        0.0,
                        -1000.0,
                        1000.0,
                        0.01
                )
        );
        this.spinner.setEditor(new JSpinner.NumberEditor(spinner, "0.00"));
        var editor = (JSpinner.NumberEditor) spinner.getEditor();
        var txt = editor.getTextField();
        var formatter = (NumberFormatter) txt.getFormatter();
        formatter.setAllowsInvalid(false);
    }

    private void initializeSpinnerValue() {
        var value = ((FieldNode) model).getValueNode().getValue();
        if (value != null)
            this.spinner.setValue(((FieldNode) model).getValueNode().getValue());
    }

    private void initializeSpinnerListener() {
        this.spinner.addChangeListener(e -> updateModel(spinner.getValue()));
    }

    public void onModelChanged(Object value) {
        this.spinner.setValue(value);
    }

}
